FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY . .
RUN mvn -q -pl aiops-server -am -DskipTests package

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl fonts-noto-cjk \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /workspace/aiops-server/target/aiops-server-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=10 \
  CMD curl --fail --silent http://localhost:8080/doc.html > /dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
