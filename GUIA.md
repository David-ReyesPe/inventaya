# Guía de instalación, ejecución y pruebas

## 1. Requisitos
- Docker Desktop / Docker Engine 24+ con Docker Compose v2 (`docker compose version`).
- Puertos libres: 5432, 8080, 8081, 8082, 8083, 8084, 9092, 29092.
- ~4 GB de RAM libres. Conexión a internet en el primer arranque (descarga imágenes y dependencias Maven **dentro** de Docker).
- Opcional, solo para pruebas fuera de Docker: JDK 21 y Maven 3.9+.
- Opcional para los scripts `.sh`: `bash`, `curl`, `python3` (en Windows use Git Bash/WSL o los `.ps1`).

## 2. Instalación y arranque (orden de ejecución)
```bash
cp .env.example .env          # Windows PowerShell: Copy-Item .env.example .env
docker compose up -d --build  # primer arranque: varios minutos (compila ambos servicios)
docker compose ps             # esperar a que todos estén "healthy"/running
```
Si ya existía un volumen de PostgreSQL de una ejecución anterior con otras credenciales:
`docker compose down -v` y volver a levantar.

Orden interno garantizado por healthchecks: PostgreSQL + Kafka → servicio-inventario (migra el esquema) → servicio-ventas → interfaz-web.

## 3. Direcciones
| Qué | URL |
|---|---|
| Interfaz gráfica | http://localhost:8083 |
| Swagger Ventas / Inventario | http://localhost:8081/swagger-ui.html · http://localhost:8082/swagger-ui.html |
| Kafka UI | http://localhost:8080 |
| pgAdmin | http://localhost:8084 (usuario/clave: variables `PGADMIN_*` del `.env`) |

## 4. Datos de prueba (ficticios)
```bash
./scripts/cargar_seed.sh               # 10 productos por SQL (db/seed/datos_prueba.sql)
./scripts/insertar_datos_prueba.sh     # 20 productos + 20 ventas por la API (Windows: insertar_datos_prueba.ps1)
```

## 5. Pruebas
```bash
# Unitarias e integración (no necesitan Docker; usan H2)
mvn test
# End-to-end sobre el stack levantado
./scripts/prueba_e2e.sh | tee docs/evidencias/prueba_e2e.log
```
Guardar los reportes de Maven: copiar `servicio-*/target/surefire-reports/*.txt` a `docs/evidencias/surefire-final/` y completar `docs/pruebas/matriz-pruebas.md`.

## 6. Uso rápido
1. Abrir http://localhost:8083, elegir rol `ADMIN` en la barra superior.
2. Inventario → crear un producto (p. ej. stock 12, umbral 10).
3. Ventas → registrar una venta de 8 unidades → el stock baja a 4 y aparece una alerta en el Dashboard.
4. Anular la venta → el stock vuelve a 12.
Ejemplos `curl` en `README.md`.

## 7. Detener / limpiar
```bash
docker compose down        # detiene
docker compose down -v     # detiene y borra la base de datos
```

## 8. Problemas frecuentes
| Síntoma | Causa / solución |
|---|---|
| `port is already allocated` | Otro programa usa el puerto; ciérrelo o cambie el mapeo en `docker-compose.yml`. |
| `servicio-ventas` no arranca | Espera a que inventario esté sano (hasta ~2 min en el primer arranque). Ver `docker compose logs servicio-inventario`. |
| `password authentication failed` | Volumen antiguo con otra clave: `docker compose down -v`. |
| 401 en la API | Falta el token: `GET /api/dev/token?role=ADMIN` (solo con perfil `dev`). |
