import { listarAlertas, listarProductos, listarVentas } from './api.js';
import { avisar, dinero, extraerLista, fecha, filaVacia, idCorto, limpiarError, montarCabecera, montarPaginacion, mostrarError } from './comunes.js';

const TAMANO_CATALOGO = 200; // Tope de size que aceptan las dos APIs.
const INTERVALO_REFRESCO_MS = 10_000;

const sesion = montarCabecera('dashboard', () => refrescar());
const paginacion = montarPaginacion('paginacion-alertas', () => cargarAlertas());

/** Ids de alertas ya vistas, para avisar solo de las nuevas y no repetir en cada refresco. */
const alertasConocidas = new Set();
let primerCargue = true;
let temporizador = null;

async function refrescar() {
    if (!sesion.haySesion()) {
        document.getElementById('indicadores').innerHTML =
            '<div class="aviso info" style="grid-column:1/-1; margin:0">Obten un token con el selector de rol de la barra superior para cargar los indicadores.</div>';
        document.getElementById('lista-alertas').innerHTML = '';
        document.getElementById('cuerpo-riesgo').innerHTML = filaVacia(5, 'Sin sesion activa.');
        document.getElementById('cuerpo-ultimas-ventas').innerHTML = filaVacia(5, 'Sin sesion activa.');
        paginacion.actualizar({ total: 0, totalPaginas: 1, tieneSiguiente: false, tieneAnterior: false });
        return;
    }

    await Promise.all([cargarIndicadores(), cargarAlertas()]);
}

async function cargarIndicadores() {
    try {
        const [productos, ventas, alertasPendientes] = await Promise.all([
            listarProductos({ page: 0, size: TAMANO_CATALOGO }),
            listarVentas({ page: 0, size: TAMANO_CATALOGO }),
            listarAlertas({ atendida: false, page: 0, size: 1 }),
        ]);
        limpiarError('error-general');

        const catalogo = extraerLista(productos?.datos ?? productos);
        const historico = extraerLista(ventas?.datos ?? ventas);
        const enRiesgo = catalogo.filter((producto) => producto.stock < producto.umbralMinimo);
        const registradas = historico.filter((venta) => venta.estado === 'REGISTRADA');
        const anuladas = historico.filter((venta) => venta.estado === 'ANULADA');
        const facturado = registradas.reduce((suma, venta) => suma + Number(venta.total), 0);
        const valorInventario = catalogo.reduce((suma, p) => suma + Number(p.precio) * p.stock, 0);

        pintarIndicadores([
            { etiqueta: 'Productos en catalogo', valor: productos.total },
            { etiqueta: 'Por reponer', valor: enRiesgo.length, clase: enRiesgo.length > 0 ? 'atencion' : '' },
            { etiqueta: 'Alertas pendientes', valor: alertasPendientes.total, clase: alertasPendientes.total > 0 ? 'atencion' : '' },
            { etiqueta: 'Ventas registradas', valor: registradas.length },
            { etiqueta: 'Ventas anuladas', valor: anuladas.length, clase: anuladas.length > 0 ? 'negativo' : '' },
            { etiqueta: 'Total facturado', valor: dinero(facturado) },
            { etiqueta: 'Valor del inventario', valor: dinero(valorInventario) },
        ]);

        pintarRiesgo(enRiesgo);
        pintarUltimasVentas(historico.slice(0, 8));
    } catch (error) {
        mostrarError('error-general', `No se pudieron cargar los indicadores: [${error.codigo}] ${error.message}`);
    }
}

function pintarIndicadores(indicadores) {
    document.getElementById('indicadores').innerHTML = indicadores.map((indicador) => `
        <div class="indicador ${indicador.clase ?? ''}">
            <div class="etiqueta">${indicador.etiqueta}</div>
            <div class="valor">${indicador.valor}</div>
        </div>`).join('');
}

function pintarRiesgo(productos) {
    const cuerpo = document.getElementById('cuerpo-riesgo');
    if (productos.length === 0) {
        cuerpo.innerHTML = filaVacia(5, 'Ningun producto esta por debajo de su umbral minimo.');
        return;
    }

    cuerpo.innerHTML = productos
        .sort((a, b) => (a.stock - a.umbralMinimo) - (b.stock - b.umbralMinimo))
        .map((producto) => `
            <tr class="fila-riesgo">
                <td>${escapar(producto.nombre)}</td>
                <td class="numero"><strong>${producto.stock}</strong></td>
                <td class="numero">${producto.umbralMinimo}</td>
                <td class="numero">${producto.umbralMinimo - producto.stock}</td>
                <td class="numero">${dinero(producto.precio)}</td>
            </tr>`).join('');
}

function pintarUltimasVentas(ventas) {
    const cuerpo = document.getElementById('cuerpo-ultimas-ventas');
    if (ventas.length === 0) {
        cuerpo.innerHTML = filaVacia(5, 'Todavia no se han registrado ventas.');
        return;
    }

    cuerpo.innerHTML = ventas.map((venta) => `
        <tr>
            <td class="mono" title="${venta.id}">${idCorto(venta.id)}</td>
            <td>${fecha(venta.fecha)}</td>
            <td class="numero">${venta.items.length}</td>
            <td class="numero">${dinero(venta.total)}</td>
            <td><span class="insignia ${venta.estado === 'ANULADA' ? 'anulada' : 'ok'}">${venta.estado}</span></td>
        </tr>`).join('');
}

async function cargarAlertas() {
    if (!sesion.haySesion()) {
        return;
    }

    const lista = document.getElementById('lista-alertas');
    try {
        const filtro = document.getElementById('filtro-atendida').value;
        const respuesta = await listarAlertas({ atendida: filtro, ...paginacion.estado });
        const alertas = extraerLista(respuesta?.datos ?? respuesta);

        if (alertas.length === 0) {
            lista.innerHTML = '<li class="vacio">No hay notificaciones de stock bajo para el filtro seleccionado.</li>';
        } else {
            lista.innerHTML = alertas.map((alerta) => `
                <li class="notificacion ${alerta.atendida ? 'atendida' : ''}">
                    <span class="icono">${alerta.atendida ? '\u2713' : '\u26A0'}</span>
                    <div class="cuerpo">
                        <div class="titulo">${escapar(alerta.productoNombre)}</div>
                        <div class="detalle">
                            Quedaron <strong>${alerta.stockActual}</strong> unidades frente a un umbral minimo de
                            <strong>${alerta.umbralMinimo}</strong> · ${fecha(alerta.fecha)}
                        </div>
                    </div>
                    <span class="insignia ${alerta.atendida ? 'ok' : 'baja'}">${alerta.atendida ? 'ATENDIDA' : 'PENDIENTE'}</span>
                </li>`).join('');
        }

        paginacion.actualizar(respuesta);
        anunciarNuevas(alertas);
    } catch (error) {
        lista.innerHTML = '<li class="vacio">No se pudieron cargar las notificaciones.</li>';
        mostrarError('error-general', `Alertas: [${error.codigo}] ${error.message}`);
    }
}

/** Avisa en pantalla cuando aparece una alerta que no estaba en el refresco anterior. */
function anunciarNuevas(alertas) {
    alertas.forEach((alerta) => {
        if (!alertasConocidas.has(alerta.id)) {
            alertasConocidas.add(alerta.id);
            if (!primerCargue && !alerta.atendida) {
                avisar(`Stock bajo: ${alerta.productoNombre} quedo en ${alerta.stockActual} (umbral ${alerta.umbralMinimo}).`, 'error');
            }
        }
    });
    primerCargue = false;
}

function ajustarAutoRefresco() {
    clearInterval(temporizador);
    if (document.getElementById('auto-refresco').checked) {
        temporizador = setInterval(() => refrescar(), INTERVALO_REFRESCO_MS);
    }
}

document.getElementById('filtro-atendida').addEventListener('change', () => {
    paginacion.reiniciar();
    cargarAlertas();
});

document.getElementById('auto-refresco').addEventListener('change', ajustarAutoRefresco);

function escapar(texto) {
    return String(texto ?? '').replace(/[&<>"]/g, (caracter) =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[caracter]);
}

refrescar();
ajustarAutoRefresco();
