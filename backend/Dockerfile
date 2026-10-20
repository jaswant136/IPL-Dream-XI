# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# ---- run ----
FROM eclipse-temurin:21-jre
RUN useradd --system --create-home appuser
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
USER appuser
EXPOSE 8080
# No secrets are baked into the image: everything comes from environment variables at runtime.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
