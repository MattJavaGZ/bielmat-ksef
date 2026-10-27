# Build stage
FROM maven:3.9-eclipse-temurin-25 AS maven_build
WORKDIR /build
COPY ./pom.xml ./pom.xml
RUN mvn dependency:go-offline -B
COPY ./src ./src
RUN mvn package -DskipTests

# Runtime stage
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=maven_build --chown=1000:1000 /build/target/bielmat-ksef-*.jar app.jar
USER 1000
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
