import { anularVenta, esAdministrador, listarProductos, listarVentas, registrarVenta } from './api.js';
import { aInstante, avisar, dinero, extraerLista, fecha, filaVacia, idCorto, limpiarError, montarCabecera, montarPaginacion, mostrarError } from './comunes.js';

const selectorProducto = document.getElementById('producto');
const cuerpoCarrito = document.getElementById('cuerpo-carrito');
const cuerpoVentas = document.getElementById('cuerpo-ventas');
const modalAnular = document.getElementById('modal-anular');

const sesion = montarCabecera('ventas', () => iniciar());
const paginacion = montarPaginacion('paginacion-ventas', () => cargarVentas());

/** Catalogo en memoria para resolver nombres y precios sin volver a consultar Inventario. */
let catalogo = [];
let carrito = [];
let ventaAAnular = null;
let filtros = {};

async function iniciar() {
    if (!sesion.haySesion()) {
        selectorProducto.innerHTML = '<option value="">Obten un token para cargar el catalogo</option>';
        cuerpoCarrito.innerHTML = filaVacia(5, 'Sin sesion activa.');
        cuerpoVentas.innerHTML = filaVacia(6, 'Obten un token con el selector de rol para consultar las ventas.');
        paginacion.actualizar({ total: 0, totalPaginas: 1, tieneSiguiente: false, tieneAnterior: false });
        return;
    }
    await cargarCatalogo();
    await cargarVentas();
}

async function cargarCatalogo() {
    try {
        const respuesta = await listarProductos({ page: 0, size: 200 });
        catalogo = extraerLista(respuesta?.datos ?? respuesta);
        selectorProducto.innerHTML = catalogo.length === 0
            ? '<option value="">No hay productos: crea uno en la pantalla de Inventario</option>'
            : catalogo.map((producto) =>
                `<option value="${producto.id}">${escapar(producto.nombre)} — stock ${producto.stock} — ${dinero(producto.precio)}</option>`).join('');
        sincronizarPrecio();
        limpiarError('error-general');
    } catch (error) {
        mostrarError('error-general', `No se pudo cargar el catalogo de Inventario: [${error.codigo}] ${error.message}`);
    }
}

function productoSeleccionado() {
    return catalogo.find((producto) => producto.id === selectorProducto.value);
}

function sincronizarPrecio() {
    const producto = productoSeleccionado();
    document.getElementById('precio').value = producto ? producto.precio : '';
}

selectorProducto.addEventListener('change', sincronizarPrecio);

document.getElementById('boton-agregar').addEventListener('click', () => {
    const producto = productoSeleccionado();
    if (!producto) {
        avisar('Selecciona un producto del catalogo.', 'error');
        return;
    }

    const cantidad = Number(document.getElementById('cantidad').value);
    const precioUnitario = Number(document.getElementById('precio').value);
    if (!Number.isInteger(cantidad) || cantidad < 1) {
        avisar('La cantidad debe ser un entero mayor que cero.', 'error');
        return;
    }

    // Se avisa antes de llamar a la API, pero la validacion que manda es la del backend:
    // el stock puede cambiar entre la carga del catalogo y el registro de la venta.
    const yaEnCarrito = carrito
        .filter((linea) => linea.productoId === producto.id)
        .reduce((suma, linea) => suma + linea.cantidad, 0);
    if (yaEnCarrito + cantidad > producto.stock) {
        avisar(`"${producto.nombre}" solo tiene ${producto.stock} unidades disponibles.`, 'error');
        return;
    }

    carrito.push({ productoId: producto.id, nombre: producto.nombre, cantidad, precioUnitario });
    pintarCarrito();
});

function pintarCarrito() {
    if (carrito.length === 0) {
        cuerpoCarrito.innerHTML = filaVacia(5, 'Agrega al menos un item para registrar la venta.');
        document.getElementById('total-carrito').textContent = dinero(0);
        document.getElementById('boton-registrar').disabled = true;
        return;
    }

    cuerpoCarrito.innerHTML = carrito.map((linea, indice) => `
        <tr>
            <td>${escapar(linea.nombre)}</td>
            <td class="numero">${linea.cantidad}</td>
            <td class="numero">${dinero(linea.precioUnitario)}</td>
            <td class="numero">${dinero(linea.cantidad * linea.precioUnitario)}</td>
            <td><button class="secundario pequeno" data-quitar="${indice}">Quitar</button></td>
        </tr>`).join('');

    cuerpoCarrito.querySelectorAll('[data-quitar]').forEach((boton) => {
        boton.addEventListener('click', () => {
            carrito.splice(Number(boton.dataset.quitar), 1);
            pintarCarrito();
        });
    });

    const total = carrito.reduce((suma, linea) => suma + linea.cantidad * linea.precioUnitario, 0);
    document.getElementById('total-carrito').textContent = dinero(total);
    document.getElementById('boton-registrar').disabled = false;
}

document.getElementById('boton-vaciar').addEventListener('click', () => {
    carrito = [];
    pintarCarrito();
});

document.getElementById('boton-registrar').addEventListener('click', async () => {
    const boton = document.getElementById('boton-registrar');
    boton.disabled = true;

    try {
        const items = carrito.map(({ productoId, cantidad, precioUnitario }) => ({ productoId, cantidad, precioUnitario }));
        const respuesta = await registrarVenta(items);
        avisar(`Venta ${idCorto(respuesta.datos.id)} registrada por ${dinero(respuesta.datos.total)}. Inventario descontara el stock al consumir el evento.`, 'exito');
        carrito = [];
        pintarCarrito();
        paginacion.reiniciar();
        // El descuento de stock es asincrono, asi que se relee el catalogo para reflejarlo.
        await cargarCatalogo();
        await cargarVentas();
    } catch (error) {
        avisar(mensajeDeError(error), 'error');
        boton.disabled = false;
    }
});

async function cargarVentas() {
    if (!sesion.haySesion()) {
        return;
    }

    cuerpoVentas.innerHTML = filaVacia(6, 'Cargando ventas...');
    try {
        const respuesta = await listarVentas({ ...filtros, ...paginacion.estado });
        pintarVentas(extraerLista(respuesta?.datos ?? respuesta));
        paginacion.actualizar(respuesta);
    } catch (error) {
        cuerpoVentas.innerHTML = filaVacia(6, 'No se pudieron cargar las ventas.');
        avisar(mensajeDeError(error), 'error');
    }
}

function pintarVentas(ventas) {
    if (ventas.length === 0) {
        cuerpoVentas.innerHTML = filaVacia(6, 'No hay ventas en el rango consultado.');
        return;
    }

    const puedeAnular = esAdministrador();

    cuerpoVentas.innerHTML = ventas.map((venta) => {
        const anulada = venta.estado === 'ANULADA';
        const detalle = venta.items.map((item) => `
            <tr>
                <td>${escapar(nombreDeProducto(item.productoId))}</td>
                <td class="numero">${item.cantidad}</td>
                <td class="numero">${dinero(item.precioUnitario)}</td>
                <td class="numero">${dinero(item.subtotal)}</td>
            </tr>`).join('');

        return `
            <tr>
                <td class="mono" title="${venta.id}">${idCorto(venta.id)}</td>
                <td>${fecha(venta.fecha)}</td>
                <td class="numero">${venta.items.length}</td>
                <td class="numero"><strong>${dinero(venta.total)}</strong></td>
                <td><span class="insignia ${anulada ? 'anulada' : 'ok'}">${venta.estado}</span></td>
                <td>
                    <button class="secundario pequeno" data-detalle="${venta.id}">Detalle</button>
                    <button class="peligro pequeno" data-anular="${venta.id}"
                            ${anulada || !puedeAnular ? 'disabled' : ''}
                            title="${anulada ? 'La venta ya esta anulada' : puedeAnular ? '' : 'Requiere rol ADMIN'}">Anular</button>
                </td>
            </tr>
            <tr class="oculto" data-fila-detalle="${venta.id}">
                <td colspan="6" style="background:var(--fondo)">
                    <table>
                        <thead>
                        <tr><th>Producto</th><th class="numero">Cantidad</th><th class="numero">Precio</th><th class="numero">Subtotal</th></tr>
                        </thead>
                        <tbody>${detalle}</tbody>
                    </table>
                </td>
            </tr>`;
    }).join('');

    cuerpoVentas.querySelectorAll('[data-detalle]').forEach((boton) => {
        boton.addEventListener('click', () => {
            cuerpoVentas.querySelector(`[data-fila-detalle="${boton.dataset.detalle}"]`).classList.toggle('oculto');
        });
    });

    cuerpoVentas.querySelectorAll('[data-anular]').forEach((boton) => {
        boton.addEventListener('click', () => {
            ventaAAnular = ventas.find((venta) => venta.id === boton.dataset.anular);
            document.getElementById('anular-venta').textContent =
                `Venta ${idCorto(ventaAAnular.id)} del ${fecha(ventaAAnular.fecha)} por ${dinero(ventaAAnular.total)}. La venta no se borra: queda marcada como ANULADA y el stock se revierte de forma asincrona.`;
            document.getElementById('motivo').value = '';
            modalAnular.showModal();
        });
    });
}

function nombreDeProducto(productoId) {
    return catalogo.find((producto) => producto.id === productoId)?.nombre ?? productoId;
}

document.getElementById('formulario-anular').addEventListener('submit', async (evento) => {
    evento.preventDefault();

    try {
        await anularVenta(ventaAAnular.id, document.getElementById('motivo').value.trim());
        modalAnular.close();
        avisar(`Venta ${idCorto(ventaAAnular.id)} anulada. Inventario revertira el stock al consumir el evento.`, 'exito');
        await cargarCatalogo();
        await cargarVentas();
    } catch (error) {
        avisar(mensajeDeError(error), 'error');
    }
});

document.querySelectorAll('dialog [data-cerrar]').forEach((boton) => {
    boton.addEventListener('click', () => boton.closest('dialog').close());
});

document.getElementById('boton-filtrar').addEventListener('click', () => {
    filtros = {
        desde: aInstante(document.getElementById('desde').value),
        hasta: aInstante(document.getElementById('hasta').value),
    };
    paginacion.reiniciar();
    cargarVentas();
});

document.getElementById('boton-limpiar-filtro').addEventListener('click', () => {
    document.getElementById('desde').value = '';
    document.getElementById('hasta').value = '';
    filtros = {};
    paginacion.reiniciar();
    cargarVentas();
});

function mensajeDeError(error) {
    if (error.codigo === 403) {
        return 'Sin permisos: anular ventas exige rol ADMIN. Cambia de rol en la barra superior.';
    }
    if (error.codigo === 409) {
        return `Rechazada por el servicio: ${error.message}`;
    }
    return `[${error.codigo}] ${error.message}`;
}

function escapar(texto) {
    return String(texto ?? '').replace(/[&<>"]/g, (caracter) =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[caracter]);
}

pintarCarrito();
iniciar();
