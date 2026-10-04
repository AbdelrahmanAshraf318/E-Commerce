# ---------- Stage 1: build the jar ----------
# Full JDK + Maven are only needed to compile; they are thrown away after this stage.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Download dependencies first, in their own layer, so code changes don't re-download the internet.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src ./src
# Tests need a running database, so they are skipped here; run them with ./mvnw test.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests \
 && cp target/*.jar app.jar


# ---------- Stage 2: run the jar ----------
# Small JRE-only image: no compiler, no Maven, no source code.
FROM eclipse-temurin:21-jre
WORKDIR /app

# openssl creates the JWT key pair on first start (see ENTRYPOINT).
RUN apt-get update \
 && apt-get install -y --no-install-recommends openssl \
 && rm -rf /var/lib/apt/lists/*

COPY --from=build /build/app.jar app.jar

ENV JWT_PRIVATE_KEY=file:/app/keys/private_key.pem \
    JWT_PUBLIC_KEY=file:/app/keys/public_key.pem

EXPOSE 8080

# Generate the RS256 key pair once (kept in the "jwt-keys" volume), then start Spring Boot.
ENTRYPOINT ["sh", "-c", "\
mkdir -p /app/keys && \
if [ ! -f /app/keys/private_key.pem ]; then \
  echo 'Generating JWT key pair...' && \
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out /app/keys/private_key.pem && \
  openssl pkey -in /app/keys/private_key.pem -pubout -out /app/keys/public_key.pem; \
fi && \
exec java -jar /app/app.jar"]
