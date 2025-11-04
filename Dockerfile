# Imagen base con Java 21
FROM eclipse-temurin:21-jdk

# Directorio de trabajo dentro del contenedor
WORKDIR /app

# Copiar los archivos del proyecto
COPY . .

# Construir el proyecto (usar el wrapper de Maven)
RUN ./mvnw clean package -DskipTests

# Exponer el puerto 8080
EXPOSE 8080

# Comando para arrancar la app
CMD ["java", "-jar", "target/gestion-proyectos-0.0.1-SNAPSHOT.jar"]
