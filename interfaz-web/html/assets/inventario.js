import { actualizarProducto, ajustarStock, crearProducto, esAdministrador, listarProductos } from './api.js';
import { avisar, dinero, extraerLista, filaVacia, idCorto, limpiarError, montarCabecera, montarPaginacion, mostrarError } from './comunes.js';

const cuerpo = document.getElementById('cuerpo-productos');
const modalStock = document.getElementById('modal-stock');
const modalEditar = document.getElementById('modal-editar');

const sesion = montarCabecera('inventario', () => cargar());
const paginacion = montarPaginacion('paginacion-productos', () => cargar());

let productoSeleccionado = null;

async function cargar() {
    if (!sesion.haySesion()) {
        cuerpo.innerHTML = filaVacia(7, 'Obten un token con el selector de rol para consultar el catalogo.');
        paginacion.actualizar({ total: 0, totalPaginas: 1, tieneSiguiente: false, tieneAnterior: false });
        return;
    }

    cuerpo.innerHTML = filaVacia(7, 'Cargando catalogo...');
    try {
        const respuesta = await listarProductos(paginacion.estado);
        limpiarError('error-general');
        pintar(extraerLista(respuesta?.datos ?? respuesta));
        paginacion.actualizar(respuesta);
    } catch (error) {
        cuerpo.innerHTML = filaVacia(7, 'No se pudo cargar el catalogo.');
        mostrarError('error-general', `[${error.codigo}] ${error.message}`);
    }
}

function pintar(productos) {
    if (productos.length === 0) {
        cuerpo.innerHTML = filaVacia(7, 'Todavia no hay productos registrados.');
        return;
    }

    cuerpo.innerHTML = productos.map((producto) => {
        const enRiesgo = producto.stock < producto.umbralMinimo;
        return `
            <tr class="${enRiesgo ? 'fila-riesgo' : ''}">
                <td class="mono" title="${producto.id}">${idCorto(producto.id)}</td>
                <td>${escapar(producto.nombre)}</td>
                <td class="numero">${dinero(producto.precio)}</td>
                <td class="numero"><strong>${producto.stock}</strong></td>
                <td class="numero">${producto.umbralMinimo}</td>
                <td><span class="insignia ${enRiesgo ? 'baja' : 'ok'}">${enRiesgo ? 'STOCK BAJO' : 'OK'}</span></td>
                <td>
                    <button class="secundario pequeno" data-accion="stock" data-id="${producto.id}">Stock</button>
                    <button class="secundario pequeno" data-accion="editar" data-id="${producto.id}">Editar</button>
                </td>
            </tr>`;
    }).join('');

    cuerpo.querySelectorAll('button[data-accion]').forEach((boton) => {
        boton.addEventListener('click', () => {
            productoSeleccionado = productos.find((p) => p.id === boton.dataset.id);
            if (boton.dataset.accion === 'stock') {
                abrirAjusteStock();
            } else {
                abrirEdicion();
            }
        });
    });
}

function abrirAjusteStock() {
    document.getElementById('stock-producto').textContent =
        `${productoSeleccionado.nombre} · stock actual ${productoSeleccionado.stock} · umbral ${productoSeleccionado.umbralMinimo}`;
    document.getElementById('cantidad').value = 1;
    modalStock.showModal();
}

function abrirEdicion() {
    document.getElementById('editar-nombre').value = productoSeleccionado.nombre;
    document.getElementById('editar-precio').value = productoSeleccionado.precio;
    document.getElementById('editar-umbral').value = productoSeleccionado.umbralMinimo;
    modalEditar.showModal();
}

document.getElementById('formulario-producto').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const formulario = evento.target;
    const boton = document.getElementById('boton-crear');
    boton.disabled = true;

    try {
        const respuesta = await crearProducto({
            nombre: formulario.nombre.value.trim(),
            precio: Number(formulario.precio.value),
            stockInicial: Number(formulario.stockInicial.value),
            umbralMinimo: Number(formulario.umbralMinimo.value),
        });
        avisar(`Producto "${respuesta.datos.nombre}" creado.`, 'exito');
        formulario.reset();
        paginacion.reiniciar();
        await cargar();
    } catch (error) {
        avisar(mensajeDeError(error), 'error');
    } finally {
        boton.disabled = false;
    }
});

document.getElementById('formulario-stock').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const formulario = evento.target;

    try {
        const respuesta = await ajustarStock(
            productoSeleccionado.id,
            formulario.operacion.value,
            Number(formulario.cantidad.value));
        modalStock.close();
        avisar(`Stock de "${respuesta.datos.nombre}" actualizado a ${respuesta.datos.stock}.`, 'exito');
        await cargar();
    } catch (error) {
        avisar(mensajeDeError(error), 'error');
    }
});

document.getElementById('formulario-editar').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const formulario = evento.target;

    try {
        const respuesta = await actualizarProducto(productoSeleccionado.id, {
            nombre: formulario.nombre.value.trim(),
            precio: Number(formulario.precio.value),
            umbralMinimo: Number(formulario.umbralMinimo.value),
        });
        modalEditar.close();
        avisar(`Producto "${respuesta.datos.nombre}" actualizado.`, 'exito');
        await cargar();
    } catch (error) {
        avisar(mensajeDeError(error), 'error');
    }
});

document.querySelectorAll('dialog [data-cerrar]').forEach((boton) => {
    boton.addEventListener('click', () => boton.closest('dialog').close());
});

document.getElementById('boton-recargar').addEventListener('click', () => cargar());

/** Traduce los codigos que devuelve la API a una explicacion util dentro de la interfaz. */
function mensajeDeError(error) {
    if (error.codigo === 403) {
        return `Sin permisos: esta operacion exige rol ADMIN y la sesion actual es ${esAdministrador() ? 'ADMIN' : 'USER'}.`;
    }
    if (error.codigo === 409) {
        return `Operacion rechazada: ${error.message}`;
    }
    return `[${error.codigo}] ${error.message}`;
}

function escapar(texto) {
    return String(texto ?? '').replace(/[&<>"]/g, (caracter) =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[caracter]);
}

cargar();
