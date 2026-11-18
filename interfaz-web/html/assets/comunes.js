/** Utilidades compartidas por las tres pantallas: sesion, formato y avisos. */

import { cerrarSesion, iniciarSesion, sesionActual } from './api.js';

const formatoDinero = new Intl.NumberFormat('es-CO', {
    style: 'currency',
    currency: 'COP',
    maximumFractionDigits: 0,
});

const formatoFecha = new Intl.DateTimeFormat('es-CO', {
    dateStyle: 'medium',
    timeStyle: 'short',
});

export function dinero(valor) {
    return formatoDinero.format(Number(valor ?? 0));
}

export function fecha(instante) {
    return instante ? formatoFecha.format(new Date(instante)) : '-';
}

export function idCorto(uuid) {
    return uuid ? uuid.slice(0, 8) : '-';
}

/** Extrae de forma segura el arreglo de elementos sea un arreglo plano o un objeto PaginaResponse. */
export function extraerLista(fuente) {
    if (Array.isArray(fuente)) return fuente;
    if (Array.isArray(fuente?.contenido)) return fuente.contenido;
    if (Array.isArray(fuente?.items)) return fuente.items;
    if (Array.isArray(fuente?.elementos)) return fuente.elementos;
    if (fuente?.datos) return extraerLista(fuente.datos);
    return [];
}

/** Convierte el valor de un input datetime-local al formato Instant que esperan los filtros. */
export function aInstante(valorLocal) {
    return valorLocal ? new Date(valorLocal).toISOString() : undefined;
}

export function avisar(mensaje, tipo = 'info') {
    let contenedor = document.getElementById('avisos');
    if (!contenedor) {
        contenedor = document.createElement('div');
        contenedor.id = 'avisos';
        document.body.appendChild(contenedor);
    }

    const aviso = document.createElement('div');
    aviso.className = `notificacion-flotante ${tipo}`;
    aviso.textContent = mensaje;
    contenedor.appendChild(aviso);
    setTimeout(() => aviso.remove(), tipo === 'error' ? 7000 : 4000);
}

export function mostrarError(idContenedor, mensaje) {
    const caja = document.getElementById(idContenedor);
    if (!caja) {
        avisar(mensaje, 'error');
        return;
    }
    caja.textContent = mensaje;
    caja.classList.remove('oculto');
}

export function limpiarError(idContenedor) {
    document.getElementById(idContenedor)?.classList.add('oculto');
}

/**
 * Dibuja la barra de sesion y marca el enlace activo. La interfaz completa exige token,
 * asi que si no hay sesion se pide un rol antes de cargar datos.
 */
export function montarCabecera(paginaActual, alCambiarSesion) {
    document.querySelectorAll('.navegacion a').forEach((enlace) => {
        if (enlace.dataset.pagina === paginaActual) {
            enlace.classList.add('activo');
        }
    });

    const estado = document.getElementById('sesion-estado');
    const selector = document.getElementById('selector-rol');
    const botonEntrar = document.getElementById('boton-entrar');
    const botonSalir = document.getElementById('boton-salir');

    function repintar() {
        const { rol } = sesionActual();
        if (rol) {
            estado.innerHTML = `Sesion activa como <span class="insignia-rol ${rol === 'ADMIN' ? '' : 'usuario'}">${rol}</span>`;
            selector.classList.add('oculto');
            botonEntrar.classList.add('oculto');
            botonSalir.classList.remove('oculto');
        } else {
            estado.textContent = 'Sin sesion: elige un rol para obtener un token de prueba.';
            selector.classList.remove('oculto');
            botonEntrar.classList.remove('oculto');
            botonSalir.classList.add('oculto');
        }
    }

    botonEntrar.addEventListener('click', async () => {
        botonEntrar.disabled = true;
        try {
            const datos = await iniciarSesion(selector.value);
            avisar(`Token emitido para el rol ${datos.rol}.`, 'exito');
            repintar();
            alCambiarSesion?.();
        } catch (error) {
            avisar(error.message, 'error');
        } finally {
            botonEntrar.disabled = false;
        }
    });

    botonSalir.addEventListener('click', () => {
        cerrarSesion();
        repintar();
        alCambiarSesion?.();
    });

    repintar();
    return { haySesion: () => Boolean(sesionActual().rol) };
}

/** Controles de paginacion reutilizables: devuelve el estado de la pagina visible. */
export function montarPaginacion(idContenedor, alCambiar) {
    const contenedor = document.getElementById(idContenedor);
    const estado = {
        page: 0,
        size: 10,
        total: 0,
        totalPaginas: 1,
        tieneSiguiente: false,
        tieneAnterior: false,
    };

    contenedor.innerHTML = `
        <button class="secundario pequeno" data-accion="anterior" disabled>Anterior</button>
        <button class="secundario pequeno" data-accion="siguiente" disabled>Siguiente</button>
        <span data-rol="texto"></span>`;

    const anterior = contenedor.querySelector('[data-accion="anterior"]');
    const siguiente = contenedor.querySelector('[data-accion="siguiente"]');
    const texto = contenedor.querySelector('[data-rol="texto"]');

    anterior.addEventListener('click', () => {
        if (!anterior.disabled && estado.tieneAnterior) {
            estado.page -= 1;
            alCambiar();
        }
    });
    siguiente.addEventListener('click', () => {
        if (!siguiente.disabled && estado.tieneSiguiente) {
            estado.page += 1;
            alCambiar();
        }
    });

    return {
        estado,
        reiniciar() {
            estado.page = 0;
            estado.tieneSiguiente = false;
            estado.tieneAnterior = false;
            anterior.disabled = true;
            siguiente.disabled = true;
        },
        actualizar(infoPaginacion = {}) {
            const {
                total = 0,
                totalPaginas = 1,
                tienePaginaSiguiente,
                tienePaginaAnterior,
                tieneSiguiente = tienePaginaSiguiente,
                tieneAnterior = tienePaginaAnterior,
                hayPaginaSiguiente,
                hayPaginaAnterior,
                hasNext,
                hasPrevious,
            } = infoPaginacion;

            estado.total = total;
            estado.totalPaginas = Math.max(totalPaginas, 1);

            // Evaluar los parametros enviados por el API REST sobre si hay pagina posterior y anterior
            const hayPosterior = tieneSiguiente ?? hayPaginaSiguiente ?? hasNext ?? (estado.page + 1 < estado.totalPaginas);
            const hayPrevio = tieneAnterior ?? hayPaginaAnterior ?? hasPrevious ?? (estado.page > 0);

            estado.tieneSiguiente = Boolean(hayPosterior);
            estado.tieneAnterior = Boolean(hayPrevio);

            // Activar o desactivar los botones de siguiente y anterior evaluando esos parametros
            anterior.disabled = !estado.tieneAnterior;
            siguiente.disabled = !estado.tieneSiguiente;

            texto.textContent = total === 0
                ? 'Sin resultados'
                : `Pagina ${estado.page + 1} de ${estado.totalPaginas} · ${total} registro(s)`;
        },
    };
}

export function filaVacia(columnas, mensaje) {
    return `<tr><td class="vacio" colspan="${columnas}">${mensaje}</td></tr>`;
}
