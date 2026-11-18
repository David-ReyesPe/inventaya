# Build en dos etapas: el contexto es solo esta carpeta, por lo que el modulo se compila
# de forma independiente del resto del monorepo (su parent es spring-boot-starter-parent).
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S inventaya && adduser -S inventaya -G inventaya
COPY --from=build /workspace/target/servicio-ventas-*.jar app.jar
USER inventaya
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
