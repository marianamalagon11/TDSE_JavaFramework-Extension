# primera etapa: compilo con maven
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q clean package -DskipTests

# segunda etapa: solo java y el jar, la imagen final queda mas liviana
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/httpServer2-1.0-SNAPSHOT.jar app.jar

# por defecto produccion, asi /shutdown no existe si se olvida la variable
ENV APP_ENV=production
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
