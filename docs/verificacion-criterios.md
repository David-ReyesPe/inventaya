# Verificación de la entrega contra los criterios

| Criterio | Dónde se cumple | Estado |
|---|---|---|
| Repositorio público con README (proyecto, integrantes, tecnologías y versiones, arquitectura, instalación) | `README.md` | Cumplido; falta publicar la versión final y la etiqueta (ver README, “Versión entregada”) |
| Versión identificada (tag/release/commit) | `README.md` | **Pendiente: crear tag y completar el commit** |
| Frontend, backend, mensajería, base de datos | `interfaz-web/`, `servicio-*`, Kafka, PostgreSQL | Implementado |
| Gateway GraphQL y notificaciones | `gateway-graphql/`, `servicio-notificaciones/` | **Pendiente** (solo contrato/README); declarado en todos los documentos |
| Diagramas y responsabilidades | `docs/arquitectura.md` | Cumplido |
| Dockerfiles y Compose (servicios, red, volúmenes, dependencias) | `docker-compose.yml`, `*/Dockerfile` | Cumplido |
| Migraciones y datos ficticios | `V1__esquema_inicial.sql`, `db/seed/`, `scripts/insertar_datos_prueba.*` | Cumplido |
| `.env.example` sin secretos | `.env.example` | Cumplido |
| Documentación de API | `docs/api.md`, `contratos/` | Cumplido |
| Manual técnico, instalación y uso | `docs/manual-tecnico.md`, `GUIA.md` | Cumplido |
| Matriz de pruebas | `docs/pruebas/matriz-pruebas.md` | Cumplido: 63 casos automatizados, 17 casos E2E y 7 flujos de UI verificados |
| Evidencias legibles | `docs/evidencias/` | Cumplido: reportes Surefire finales, log E2E y 7 capturas PNG |
| Medido vs. simulado vs. futuro | `docs/pruebas/matriz-pruebas.md` sección 0 y 4 | Cumplido |
| ZIP ejecutable siguiendo solo las instrucciones | `GUIA.md` | **Por verificar por el equipo** en carpeta limpia |
| Datos de seguimiento | Archivo compartido del curso | **Por actualizar por el equipo** |
