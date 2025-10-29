# --------- STAGE 1: BUILD ---------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copiamos solo lo necesario para cachear dependencias
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline

# Ahora el código
COPY src src

# Compila (saldrá el jar en target/)
RUN ./mvnw clean package -DskipTests


# --------- STAGE 2: RUNTIME ---------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Usuario no-root
RUN useradd -r -s /usr/sbin/nologin appuser

# Copiamos el jar construido
COPY --from=build /workspace/target/*-SNAPSHOT.jar /app/app.jar

# Directorios para datos persistentes
RUN mkdir -p /data /app/uploads && chown -R appuser:appuser /data /app/uploads

# Variables de entorno útiles
ENV TZ=Europe/Madrid \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC"

# IMPORTANTE: Render expone la variable PORT
# Forzamos server.port a $PORT y configuramos H2 a /data
ENV SPRING_DATASOURCE_URL="jdbc:h2:file:/data/gestionproyectos-db;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE" \
    SPRING_DATASOURCE_DRIVER_CLASS_NAME="org.h2.Driver"

EXPOSE 8080

USER appuser

# Usamos ENTRYPOINT para respetar $PORT en Render
ENTRYPOINT sh -c 'java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar /app/app.jar'