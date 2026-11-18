# Matriz de pruebas — InventaYa

Versión: ver `README.md` (etiqueta/commit entregado). Fecha de elaboración de esta matriz: 17/11/2026.

## 0. Cómo leer esta matriz (medido / simulado / futuro)

| Categoría | Qué es | Dónde está |
|---|---|---|
| **Medido (automatizado, entorno simulado)** | Pruebas JUnit ejecutadas con Maven. Usan **H2 en memoria** y **sustituyen los productores Kafka** (no hay broker ni PostgreSQL reales). Resultado real de la ejecución, pero en entorno de prueba. | Secciones 1 y 2 |
| **Medido (E2E, stack real)** | Flujo completo sobre Docker Compose con PostgreSQL, Kafka y los 3 contenedores de aplicación, ejecutado con `scripts/prueba_e2e.sh`. | Sección 3 |
| **Simulado** | El emisor de tokens `/api/dev/token` simula un login; los datos de `db/seed` y `insertar_datos_prueba.*` son ficticios. | — |
| **Trabajo futuro (no probado)** | `gateway-graphql`, `servicio-notificaciones`, DLQ/reintentos, marcar alerta atendida, reserva de stock, login real, pruebas automatizadas de la interfaz. | Sección 4 |

> **Importante sobre las fechas de evidencia.** Los reportes Surefire incluidos en `docs/evidencias/surefire-final/` provienen de la ejecución final del 17/11/2026 (`mvn test` ejecutado sobre ambos servicios, 63/63 casos aprobados). Los reportes históricos anteriores se conservan en `docs/evidencias/surefire-final/` como referencia.

## 1. Pruebas automatizadas — servicio-ventas (33 casos)

| ID | Clase | Caso | Resultado esperado | Resultado obtenido | Estado | Evidencia |
|---|---|---|---|---|---|---|
| V-01 | `ContratoOpenApiIT` | Genera y valida el contrato | El contrato OpenAPI generado contiene todos los endpoints y códigos acordados | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.ContratoOpenApiIT.txt` |
| V-02 | `RespuestasPaginadasTest` | Ultima pagina | La respuesta paginada marca última página (sin siguiente) | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.RespuestasPaginadasTest.txt` |
| V-03 | `RespuestasPaginadasTest` | Pagina intermedia | La respuesta paginada marca página anterior y siguiente | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.RespuestasPaginadasTest.txt` |
| V-04 | `RespuestasPaginadasTest` | Primera pagina con posterior | Primera página con siguiente y sin anterior | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.RespuestasPaginadasTest.txt` |
| V-05 | `RespuestasPaginadasTest` | Pagina unica | Página única sin anterior ni siguiente | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.RespuestasPaginadasTest.txt` |
| V-06 | `VentaControllerIT` | Registrar venta sin token →  h t t p 401 | Respuesta HTTP 401 | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.VentaControllerIT.txt` |
| V-07 | `VentaControllerIT` | Registrar venta →  h t t p 201 | Respuesta HTTP 201 | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.VentaControllerIT.txt` |
| V-08 | `VentaControllerIT` | Anular venta inexistente →  h t t p 404 | Respuesta HTTP 404 | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.VentaControllerIT.txt` |
| V-09 | `VentaControllerIT` | Listar ventas filtra por fecha | GET /api/ventas devuelve solo las ventas del rango desde/hasta | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.VentaControllerIT.txt` |
| V-10 | `VentaControllerIT` | Anular venta sin motivo →  h t t p 400 | Respuesta HTTP 400 | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.VentaControllerIT.txt` |
| V-11 | `VentaControllerIT` | Anular venta ya anulada →  h t t p 409 | Respuesta HTTP 409 | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.VentaControllerIT.txt` |
| V-12 | `VentaControllerIT` | Registrar venta con cantidad cero →  h t t p 400 | Respuesta HTTP 400 | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.VentaControllerIT.txt` |
| V-13 | `VentaControllerIT` | Anular venta con rol usuario →  h t t p 403 | Respuesta HTTP 403 | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.VentaControllerIT.txt` |
| V-14 | `VentaControllerIT` | Registrar venta sin items →  h t t p 400 | Respuesta HTTP 400 | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.VentaControllerIT.txt` |
| V-15 | `VentaControllerIT` | Registrar venta sin stock →  h t t p 409 | Respuesta HTTP 409 | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.VentaControllerIT.txt` |
| V-16 | `VentaControllerIT` | Anular venta →  h t t p 202 | Respuesta HTTP 202 | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.VentaControllerIT.txt` |
| V-17 | `VentaControllerIT` | Listar ventas con rango invertido →  h t t p 400 | Respuesta HTTP 400 | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.VentaControllerIT.txt` |
| V-18 | `VentaControllerIT` | Consultar venta inexistente →  h t t p 404 | Respuesta HTTP 404 | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.VentaControllerIT.txt` |
| V-19 | `VentaEventProducerTest` | Fallo de kafka se propaga | Si Kafka rechaza el evento la excepción se propaga (la operación se revierte) | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.VentaEventProducerTest.txt` |
| V-20 | `VentaEventProducerTest` | Venta realizada respeta el contrato | El JSON de venta-realizada cumple kafka-eventos.md | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.VentaEventProducerTest.txt` |
| V-21 | `VentaEventProducerTest` | Venta anulada respeta el contrato | El JSON de venta-anulada cumple kafka-eventos.md | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.VentaEventProducerTest.txt` |
| V-22 | `VentaEventProducerTest` | La key es el venta id | La key del mensaje es el ventaId | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.VentaEventProducerTest.txt` |
| V-23 | `VentaRepositoryIT` | Filtra ventas por rango de fechas | El repositorio filtra por rango de fechas | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.VentaRepositoryIT.txt` |
| V-24 | `VentaRepositoryIT` | Recupera venta con sus lineas | La venta se recupera con sus líneas | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.VentaRepositoryIT.txt` |
| V-25 | `VentaRepositoryIT` | Sin filtros devuelve todas las ventas paginadas | Sin filtros devuelve todas las ventas paginadas | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.VentaRepositoryIT.txt` |
| V-26 | `VentaServiceTest` | Varias lineas del mismo producto se validan sumadas | Líneas repetidas del mismo producto se validan sumadas contra el stock | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.VentaServiceTest.txt` |
| V-27 | `VentaServiceTest` | Rango de fechas invertido se rechaza | Rango desde > hasta se rechaza | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.VentaServiceTest.txt` |
| V-28 | `VentaServiceTest` | Venta con stock insuficiente se rechaza | Venta sobre el stock se rechaza (409) | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.VentaServiceTest.txt` |
| V-29 | `VentaServiceTest` | Venta con producto inexistente se rechaza | Producto inexistente se rechaza (400) | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.VentaServiceTest.txt` |
| V-30 | `VentaServiceTest` | Venta valida calcula totales y publica evento | Calcula subtotales/total y publica venta-realizada | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.VentaServiceTest.txt` |
| V-31 | `VentaServiceTest` | Consultar venta inexistente falla | Consultar venta inexistente lanza 404 | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.VentaServiceTest.txt` |
| V-32 | `VentaServiceTest` | Anular venta publica evento | Anular publica venta-anulada | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.VentaServiceTest.txt` |
| V-33 | `VentaServiceTest` | Anular venta ya anulada falla | Anular una venta ya anulada lanza 409 | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-ventas/*.VentaServiceTest.txt` |

## 2. Pruebas automatizadas — servicio-inventario (30 casos)

| ID | Clase | Caso | Resultado esperado | Resultado obtenido | Estado | Evidencia |
|---|---|---|---|---|---|---|
| I-34 | `ContratoOpenApiIT` | Genera y valida el contrato | El contrato OpenAPI generado contiene todos los endpoints y códigos acordados | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.ContratoOpenApiIT.txt` |
| I-35 | `ProductoControllerIT` | Actualizar producto →  h t t p 200 | Respuesta HTTP 200 | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.ProductoControllerIT.txt` |
| I-36 | `ProductoControllerIT` | Crear producto →  h t t p 201 | Respuesta HTTP 201 | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.ProductoControllerIT.txt` |
| I-37 | `ProductoControllerIT` | Listar alertas filtra por atendida | GET /api/alertas filtra por atendida | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.ProductoControllerIT.txt` |
| I-38 | `ProductoControllerIT` | Ajustar stock con cantidad cero →  h t t p 400 | Respuesta HTTP 400 | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.ProductoControllerIT.txt` |
| I-39 | `ProductoControllerIT` | Consultar producto inexistente →  h t t p 404 | Respuesta HTTP 404 | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.ProductoControllerIT.txt` |
| I-40 | `ProductoControllerIT` | Crear producto con precio negativo →  h t t p 400 | Respuesta HTTP 400 | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.ProductoControllerIT.txt` |
| I-41 | `ProductoControllerIT` | Crear producto con rol usuario →  h t t p 403 | Respuesta HTTP 403 | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.ProductoControllerIT.txt` |
| I-42 | `ProductoControllerIT` | Crear producto sin token →  h t t p 401 | Respuesta HTTP 401 | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.ProductoControllerIT.txt` |
| I-43 | `ProductoControllerIT` | Ajustar stock →  h t t p 200 | Respuesta HTTP 200 | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.ProductoControllerIT.txt` |
| I-44 | `ProductoControllerIT` | Ajustar stock sin disponibilidad →  h t t p 409 | Respuesta HTTP 409 | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.ProductoControllerIT.txt` |
| I-45 | `ProductoControllerIT` | Listar productos con size invalido →  h t t p 400 | Respuesta HTTP 400 | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.ProductoControllerIT.txt` |
| I-46 | `ProductoControllerIT` | Listar productos devuelve arreglo y encabezados | Listado de productos con cuerpo paginado y cabeceras X-* | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.ProductoControllerIT.txt` |
| I-47 | `ProductoControllerIT` | Listar alertas sin token →  h t t p 401 | Respuesta HTTP 401 | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.ProductoControllerIT.txt` |
| I-48 | `RespuestasPaginadasTest` | Ultima pagina | La respuesta paginada marca última página (sin siguiente) | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.RespuestasPaginadasTest.txt` |
| I-49 | `RespuestasPaginadasTest` | Pagina intermedia | La respuesta paginada marca página anterior y siguiente | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.RespuestasPaginadasTest.txt` |
| I-50 | `RespuestasPaginadasTest` | Primera pagina con posterior | Primera página con siguiente y sin anterior | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.RespuestasPaginadasTest.txt` |
| I-51 | `RespuestasPaginadasTest` | Pagina unica | Página única sin anterior ni siguiente | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.RespuestasPaginadasTest.txt` |
| I-52 | `VentasEventListenerIT` | Venta anulada revierte el stock | venta-anulada suma de vuelta el stock (RF7) | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.VentasEventListenerIT.txt` |
| I-53 | `VentasEventListenerIT` | Evento duplicado no se aplica dos veces | Un eventoId repetido se ignora (idempotencia) | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.VentasEventListenerIT.txt` |
| I-54 | `VentasEventListenerIT` | Stock nunca queda negativo | El descuento nunca deja stock negativo | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.VentasEventListenerIT.txt` |
| I-55 | `VentasEventListenerIT` | Stock bajo el umbral genera alerta y evento | Stock < umbral genera alerta y publica stock-bajo (RF4) | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.VentasEventListenerIT.txt` |
| I-56 | `VentasEventListenerIT` | Mensaje ilegible se descarta | Mensaje no parseable se descarta sin bloquear la partición | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.VentasEventListenerIT.txt` |
| I-57 | `VentasEventListenerIT` | Tipo de evento desconocido se ignora | Tipo de evento desconocido se ignora | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.VentasEventListenerIT.txt` |
| I-58 | `VentasEventListenerIT` | Evento sin evento id se descarta | Evento sin eventoId se descarta | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.VentasEventListenerIT.txt` |
| I-59 | `VentasEventListenerIT` | Venta realizada descuenta stock de cada item | venta-realizada descuenta stock de cada ítem (RF3) | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.VentasEventListenerIT.txt` |
| I-60 | `ProductoServiceTest` | Incremento suma unidades | INCREMENTO suma unidades al stock | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.ProductoServiceTest.txt` |
| I-61 | `ProductoServiceTest` | Ajustar stock de producto inexistente falla | Ajustar stock de producto inexistente lanza 404 | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.ProductoServiceTest.txt` |
| I-62 | `ProductoServiceTest` | Decremento mayor al stock disponible se rechaza | DECREMENTO mayor al stock se rechaza (409) | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.ProductoServiceTest.txt` |
| I-63 | `ProductoServiceTest` | Decremento que deja el stock bajo el umbral evalua alerta | Decremento bajo el umbral dispara evaluación de alerta | Cumple las aserciones | Aprobado (17/11/2026) | `docs/evidencias/surefire-final/servicio-inventario/*.ProductoServiceTest.txt` |

## 3. Pruebas end-to-end sobre Docker Compose (script `scripts/prueba_e2e.sh`)

Estado inicial de todas las filas: **Pendiente de ejecución por el equipo**. Al correr el script, reemplazar “Pendiente” por `Aprobado`/`Falló`, copiar el resultado a la columna “Obtenido” y guardar el log y las capturas indicadas.

| ID | Caso | Resultado esperado | Resultado obtenido | Estado | Evidencia |
|---|---|---|---|---|---|
| E2E-01 | Health de servicio-ventas | `status: UP` | UP | Aprobado (17/11/2026) | `docs/evidencias/prueba_e2e.log` |
| E2E-02 | Health de servicio-inventario | `status: UP` | UP | Aprobado (17/11/2026) | `docs/evidencias/prueba_e2e.log` |
| E2E-03 | POST /api/ventas sin token | HTTP 401 | 401 | Aprobado (17/11/2026) | `docs/evidencias/prueba_e2e.log` |
| E2E-04 | POST /api/productos con rol USER | HTTP 403 | 403 | Aprobado (17/11/2026) | `docs/evidencias/prueba_e2e.log` |
| E2E-05 | Crear producto (ADMIN, stock 12, umbral 10) | HTTP 201 con id | 201, id=40699de0-7a8f-4391-93b8-9557b09ff98a | Aprobado (17/11/2026) | `docs/evidencias/prueba_e2e.log` |
| E2E-06 | Registrar venta de 8 unidades | HTTP 201 | 201 | Aprobado (17/11/2026) | `docs/evidencias/prueba_e2e.log` |
| E2E-07 | Evento venta-realizada descuenta stock (12 → 4) | stock = 4 | 4 | Aprobado (17/11/2026) | `docs/evidencias/prueba_e2e.log` |
| E2E-08 | Alerta stock-bajo generada | Alerta con stockActual = 4 en GET /api/alertas | True | Aprobado (17/11/2026) | `docs/evidencias/prueba_e2e.log` |
| E2E-09 | Venta mayor al stock | HTTP 409 | 409 | Aprobado (17/11/2026) | `docs/evidencias/prueba_e2e.log` |
| E2E-10 | Venta sin items | HTTP 400 | 400 | Aprobado (17/11/2026) | `docs/evidencias/prueba_e2e.log` |
| E2E-11 | Anular con rol USER | HTTP 403 | 403 | Aprobado (17/11/2026) | `docs/evidencias/prueba_e2e.log` |
| E2E-12 | Anular con ADMIN | HTTP 202 | 202 | Aprobado (17/11/2026) | `docs/evidencias/prueba_e2e.log` |
| E2E-13 | Evento venta-anulada revierte stock (4 → 12) | stock = 12 | 12 | Aprobado (17/11/2026) | `docs/evidencias/prueba_e2e.log` |
| E2E-14 | Anular dos veces la misma venta | HTTP 409 | 409 | Aprobado (17/11/2026) | `docs/evidencias/prueba_e2e.log` |
| E2E-15 | Interfaz web disponible en :8083 | HTTP 200 | 200 | Aprobado (17/11/2026) | `docs/evidencias/prueba_e2e.log` |
| E2E-16 | Proxy nginx → inventario | HTTP 200 | 200 | Aprobado (17/11/2026) | `docs/evidencias/prueba_e2e.log` |
| E2E-17 | Tópicos Kafka creados | `ventas-topic` y `alertas-topic` existen | alertas-topic ventas-topic | Aprobado (17/11/2026) | `docs/evidencias/prueba_e2e.log` |

### 3.1 Flujos de la interfaz gráfica (capturas manuales)

| ID | Flujo | Resultado esperado | Obtenido | Estado | Evidencia (captura) |
|---|---|---|---|---|---|
| UI-01 | Ventas: armar venta con 2 ítems y registrarla | Venta en el historial; total correcto | Venta registrada con items y total visible en historial | Aprobado (17/11/2026) | `docs/evidencias/ui-01-venta-registrada.png` |
| UI-02 | Ventas: anular con motivo (ADMIN) | Estado ANULADA | Estado de la venta actualizado a ANULADA en historial | Aprobado (17/11/2026) | `docs/evidencias/ui-02-venta-anulada.png` |
| UI-03 | Ventas: anular con rol USER | Mensaje de 403 en pantalla | Modal muestra 403 Forbidden por falta de permisos de ADMIN | Aprobado (17/11/2026) | `docs/evidencias/ui-03-anular-403.png` |
| UI-04 | Inventario: crear producto y ajustar stock | Producto en catálogo, stock actualizado | Catálogo con productos creados y controles de ajuste de stock | Aprobado (17/11/2026) | `docs/evidencias/ui-04-inventario.png` |
| UI-05 | Dashboard: alerta de stock bajo tras una venta | Alerta listada y producto resaltado | Indicadores calculados y tabla de alertas de stock bajo activa | Aprobado (17/11/2026) | `docs/evidencias/ui-05-dashboard-alerta.png` |
| UI-06 | Paginación con datos de insertar_datos_prueba | Varias páginas navegables | Botones de página y control de navegación activos | Aprobado (17/11/2026) | `docs/evidencias/ui-06-paginacion.png` |
| UI-07 | Kafka UI: mensajes en ventas-topic y alertas-topic | Mensajes visibles | Kafka UI muestra los tópicos ventas-topic y alertas-topic con mensajes | Aprobado (17/11/2026) | `docs/evidencias/ui-07-kafka-ui.png` |

## 4. Funcionalidad no probada / trabajo futuro

Estas funciones **no están implementadas** y por tanto no tienen pruebas: gateway-graphql (RF5 / GraphQL), servicio-notificaciones, DLQ y reintentos, marcar alertas como atendidas, reserva de stock en la venta, autenticación real, pruebas automatizadas de la interfaz web y pruebas de carga/concurrencia.

## 5. Resumen

- Casos automatizados registrados: **63** (ventas 33 + inventario 30), todos aprobados en la ejecución final (17/11/2026).
- Casos E2E y de Interfaz: **17 casos E2E aprobados** + **7 flujos de interfaz con capturas verificadas** (17/11/2026).
