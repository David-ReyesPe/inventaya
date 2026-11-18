# Manual técnico

## 1. Stack y versiones
| Capa | Tecnología | Versión (fuente) |
|---|---|---|
| Lenguaje / JDK | Java | 21 (`pom.xml`, imagen `eclipse-temurin:21-jre-alpine`) |
| Framework | Spring Boot | 3.3.5 (`spring-boot-starter-parent`) |
| Mensajería | Apache Kafka (KRaft) + Spring Kafka | 3.8.0 (`apache/kafka:3.8.0`) |
| Base de datos | PostgreSQL | 16 (`postgres:16`) |
| Migraciones | Flyway | gestionado por Spring Boot 3.3.5 |
| Contrato API | springdoc-openapi | 2.6.0 |
| Seguridad | JJWT (HMAC-SHA) | 0.12.6 |
| Pruebas | JUnit 5, Mockito, MockMvc, H2 | gestionado por Spring Boot |
| Build | Maven (imagen `maven:3.9.9-eclipse-temurin-21`) | 3.9.9 |
| Frontend | HTML5 + CSS3 + JavaScript (sin framework) sobre nginx | `nginx:1.27-alpine` |
| Orquestación | Docker Compose | v2 |
| Herramientas | Kafka UI, pgAdmin | `provectuslabs/kafka-ui:latest`, `dpage/pgadmin4:latest` (sin versión fija) |

## 2. Estructura de cada servicio
Paquetes `config` (JWT, Kafka, OpenAPI), `controller`, `dto`, `entity`, `repository`, `service`, `kafka`, `exception`, `mapper`.

## 3. Seguridad
`JwtAuthFilter` valida el token (401) y `RolInterceptor` + `@RolRequerido` comprueban el rol (403). Roles: `ADMIN`, `USER`. Clave compartida `INVENTAYA_JWT_SECRET` (≥ 32 caracteres). El emisor `/api/dev/token` existe solo con el perfil `dev`.

## 4. Mensajería
Ver `kafka/kafka-eventos.md`. Garantías: idempotencia por `eventoId` (tabla `eventos_procesados`), orden por `ventaId`, publicación síncrona dentro de la transacción.

## 5. Variables de entorno
Ver `.env.example`. Cada servicio lee además `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SPRING_KAFKA_BOOTSTRAP_SERVERS` (definidas en `docker-compose.yml`).

## 6. Base de datos
Esquema en `servicio-inventario/src/main/resources/db/migration/V1__esquema_inicial.sql` (único dueño del esquema). Datos ficticios: `db/seed/datos_prueba.sql`.

## 7. Compilar y probar sin Docker
```bash
mvn test                                  # ambos módulos
mvn -f servicio-ventas/pom.xml package    # JAR de un servicio
```

## 8. Dependencias externas y limitaciones
- Docker Hub (imágenes) y Maven Central (dependencias) en el primer build.
- Sin servicios de terceros (no hay correo, SMS ni pasarelas).
- Limitaciones y pendientes: ver `docs/arquitectura.md` sección 5 y `README.md` “Limitaciones conocidas”.
