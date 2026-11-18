/**
 * Cliente de las dos APIs REST. La interfaz se sirve desde el mismo origen que /api/**
 * (nginx hace de reverse proxy hacia servicio-ventas y servicio-inventario), asi que aqui
 * solo hay rutas relativas y no interviene CORS.
 */

const CLAVE_TOKEN = 'inventaya.token';
const CLAVE_ROL = 'inventaya.rol';

/** Error de negocio devuelto por la API con el esquema ErrorResponse (codigo, mensaje, timestamp). */
export class ErrorApi extends Error {
    constructor(codigo, mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }
}

export function sesionActual() {
    return {
        token: sessionStorage.getItem(CLAVE_TOKEN),
        rol: sessionStorage.getItem(CLAVE_ROL),
    };
}

export function esAdministrador() {
    return sesionActual().rol === 'ADMIN';
}

/**
 * Pide un token al endpoint de desarrollo. En una version productiva este paso lo
 * reemplazaria un login real; el resto de la interfaz no cambiaria, porque solo consume
 * el token que quede guardado en la sesion del navegador.
 */
export async function iniciarSesion(rol) {
    const respuesta = await fetch(`/api/dev/token?role=${encodeURIComponent(rol)}&subject=interfaz-web`);
    if (!respuesta.ok) {
        throw new ErrorApi(respuesta.status,
            'No se pudo obtener el token de prueba. Verifica que los servicios esten arriba con el perfil dev.');
    }
    const datos = await respuesta.json();
    sessionStorage.setItem(CLAVE_TOKEN, datos.token);
    sessionStorage.setItem(CLAVE_ROL, datos.rol);
    return datos;
}

export function cerrarSesion() {
    sessionStorage.removeItem(CLAVE_TOKEN);
    sessionStorage.removeItem(CLAVE_ROL);
}

async function pedir(metodo, ruta, cuerpo) {
    const { token } = sesionActual();
    const cabeceras = {};
    if (token) {
        cabeceras.Authorization = `Bearer ${token}`;
    }
    if (cuerpo !== undefined) {
        cabeceras['Content-Type'] = 'application/json';
    }

    let respuesta;
    try {
        respuesta = await fetch(ruta, {
            method: metodo,
            headers: cabeceras,
            body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
        });
    } catch (causa) {
        throw new ErrorApi(0, 'No hay conexion con el servicio. Revisa que los contenedores esten en ejecucion.');
    }

    const texto = await respuesta.text();
    const datos = texto ? JSON.parse(texto) : null;

    if (!respuesta.ok) {
        // Todos los errores de las dos APIs comparten el esquema ErrorResponse, asi que la
        // interfaz puede mostrar el mensaje del backend tal cual en lugar de inventar uno.
        throw new ErrorApi(datos?.codigo ?? respuesta.status,
            datos?.mensaje ?? `El servicio respondio ${respuesta.status}.`);
    }

    // Extraer el listado de elementos si la respuesta es paginada o un arreglo
    const lista = Array.isArray(datos)
        ? [...datos]
        : Array.isArray(datos?.contenido)
            ? [...datos.contenido]
            : Array.isArray(datos?.items)
                ? [...datos.items]
                : Array.isArray(datos?.elementos)
                    ? [...datos.elementos]
                    : Array.isArray(datos?.datos)
                        ? [...datos.datos]
                        : null;

    // Si no es un arreglo ni respuesta con lista (ej. objeto unico o null), devolver tal cual
    if (!lista) {
        return datos;
    }

    const total = Number(
        respuesta.headers.get('X-Total-Count') ??
        (Array.isArray(datos) ? datos.length : (datos?.totalElementos ?? datos?.total ?? lista.length))
    );
    const totalPaginas = Number(
        respuesta.headers.get('X-Total-Pages') ??
        (datos?.totalPaginas ?? 1)
    );
    const paginaActual = Number(
        respuesta.headers.get('X-Page') ??
        (datos?.paginaActual ?? datos?.page ?? 0)
    );
    const tamanoPagina = Number(
        respuesta.headers.get('X-Page-Size') ??
        (datos?.tamanoPagina ?? datos?.size ?? 20)
    );

    const cabeceraSiguiente = respuesta.headers.get('X-Has-Next') ?? respuesta.headers.get('X-Tiene-Siguiente');
    const cabeceraAnterior = respuesta.headers.get('X-Has-Previous') ?? respuesta.headers.get('X-Tiene-Anterior');

    // Evaluar los parametros de pagina posterior y anterior prioritariamente desde el cuerpo JSON,
    // luego desde los encabezados HTTP, y finalmente calculados:
    const tieneSiguiente = (datos?.tienePaginaSiguiente ?? datos?.hayPaginaSiguiente ?? datos?.hasNext) !== undefined
        ? Boolean(datos.tienePaginaSiguiente ?? datos.hayPaginaSiguiente ?? datos.hasNext)
        : (cabeceraSiguiente !== null
            ? (cabeceraSiguiente === 'true')
            : (paginaActual + 1 < totalPaginas));

    const tieneAnterior = (datos?.tienePaginaAnterior ?? datos?.hayPaginaAnterior ?? datos?.hasPrevious) !== undefined
        ? Boolean(datos.tienePaginaAnterior ?? datos.hayPaginaAnterior ?? datos.hasPrevious)
        : (cabeceraAnterior !== null
            ? (cabeceraAnterior === 'true')
            : (paginaActual > 0));

    // Enriquecer el arreglo 'lista' con los metadatos de navegacion y compatibilidad.
    // Al ser un Array real, soporta directamente .map(), .filter(), .forEach(), .length,
    // y al mismo tiempo provee .tienePaginaSiguiente, .tienePaginaAnterior, .contenido, .datos, etc.
    lista.datos = lista;
    lista.contenido = lista;
    lista.items = lista;
    lista.estado = respuesta.status;
    lista.total = total;
    lista.totalElementos = total;
    lista.totalPaginas = totalPaginas;
    lista.paginaActual = paginaActual;
    lista.tamanoPagina = tamanoPagina;
    lista.tienePaginaSiguiente = tieneSiguiente;
    lista.tienePaginaAnterior = tieneAnterior;
    lista.hayPaginaSiguiente = tieneSiguiente;
    lista.hayPaginaAnterior = tieneAnterior;
    lista.tieneSiguiente = tieneSiguiente;
    lista.tieneAnterior = tieneAnterior;
    lista.hasNext = tieneSiguiente;
    lista.hasPrevious = tieneAnterior;
    lista.cuerpoCompleto = datos;

    return lista;
}

/* ---------------- Servicio de Inventario ---------------- */

export function listarProductos({ page = 0, size = 20 } = {}) {
    return pedir('GET', `/api/productos?page=${page}&size=${size}`);
}

export function obtenerProducto(id) {
    return pedir('GET', `/api/productos/${id}`);
}

export function crearProducto(producto) {
    return pedir('POST', '/api/productos', producto);
}

export function actualizarProducto(id, producto) {
    return pedir('PUT', `/api/productos/${id}`, producto);
}

export function ajustarStock(id, operacion, cantidad) {
    return pedir('PUT', `/api/productos/${id}/stock`, { operacion, cantidad });
}

export function listarAlertas({ atendida, page = 0, size = 20 } = {}) {
    const parametros = new URLSearchParams({ page, size });
    if (atendida !== undefined && atendida !== null && atendida !== '') {
        parametros.set('atendida', atendida);
    }
    return pedir('GET', `/api/alertas?${parametros}`);
}

/* ---------------- Servicio de Ventas ---------------- */

export function listarVentas({ desde, hasta, page = 0, size = 20 } = {}) {
    const parametros = new URLSearchParams({ page, size });
    if (desde) {
        parametros.set('desde', desde);
    }
    if (hasta) {
        parametros.set('hasta', hasta);
    }
    return pedir('GET', `/api/ventas?${parametros}`);
}

export function registrarVenta(items) {
    return pedir('POST', '/api/ventas', { items });
}

export function anularVenta(id, motivo) {
    return pedir('PATCH', `/api/ventas/${id}/anular`, { motivo });
}
