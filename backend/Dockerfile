# ── Stage 1: Install Maven (cached unless base image changes) ─────────────
FROM eclipse-temurin:21-jdk-alpine AS maven-base
RUN apk add --no-cache maven

# ── Stage 2: Download dependencies (cached unless pom.xml changes) ────────
FROM maven-base AS deps
WORKDIR /app
COPY pom.xml .
# This layer is only re-run when pom.xml changes — saves ~2 min on code-only changes
RUN mvn dependency:go-offline -q

# ── Stage 3: Build (only re-runs when src/ changes) ───────────────────────
FROM deps AS build
COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Stage 4: Run (lightweight JRE only) ────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
