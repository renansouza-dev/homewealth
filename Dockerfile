# -----------------------------------------------------
# STAGE 1 — JLINK: Create minimal Java runtime
# -----------------------------------------------------
ARG ALPINE_VERSION=3.23.4
FROM alpine:${ALPINE_VERSION} AS jlink

ENV JAVA_HOME=/opt/jdk/jdk-25.0.3+9
ENV PATH="${JAVA_HOME}/bin:${PATH}"

ADD --checksum=sha256:51c2415b370aac7c3796b0c4663c8fcf91bc22d76f03df95b25fa5667cb5fdd8 \
    https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25.0.3%2B9/OpenJDK25U-jdk_x64_alpine-linux_hotspot_25.0.3_9.tar.gz \
    /opt/jdk/jdk.tar.gz

RUN tar -xzf /opt/jdk/jdk.tar.gz -C /opt/jdk/ && \
    rm /opt/jdk/jdk.tar.gz && \
    jlink --compress=zip-6 \
          --no-header-files \
          --no-man-pages \
          --module-path "${JAVA_HOME}/jmods" \
          --add-modules java.base,java.logging,java.desktop,java.management,java.naming,java.security.jgss,java.instrument,java.sql,jdk.unsupported,java.compiler \
          --output /springboot-runtime
# -----------------------------------------------------
# STAGE 2 — BUILD: Compile the JAR
# -----------------------------------------------------
FROM maven:3-eclipse-temurin-25-alpine AS builder

WORKDIR /app

COPY pom.xml                  .
COPY specifications/pom.xml   specifications/pom.xml
COPY transactions/pom.xml     transactions/pom.xml

RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -pl transactions -am -q

COPY specifications/openapi   specifications/openapi
COPY transactions/src         transactions/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl transactions -am -Dmaven.test.skip=true clean package -q

# -----------------------------------------------------
# STAGE 3 — LAYERS: Extract JAR layers for caching
# -----------------------------------------------------
FROM jlink AS layers

WORKDIR /app

COPY --from=builder /app/transactions/target/transactions*.jar app.jar

RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# -----------------------------------------------------
# STAGE 4 — RUNTIME: Minimal Alpine + jlink JRE
# -----------------------------------------------------
ARG ALPINE_VERSION=3.23.4
FROM alpine:${ALPINE_VERSION}

RUN apk add --no-cache curl \
 && addgroup -S appgroup \
 && adduser -S -D -H -G appgroup appuser

# Copy minimal JRE from jlink stage
COPY --from=jlink /springboot-runtime /opt/jdk

ENV PATH="/opt/jdk/bin:${PATH}"

WORKDIR /opt/app
RUN chown appuser:appgroup /opt/app

COPY --from=layers --chmod=550 --chown=appuser:appgroup /app/extracted/dependencies/          ./
COPY --from=layers --chmod=550 --chown=appuser:appgroup /app/extracted/spring-boot-loader/    ./
COPY --from=layers --chmod=550 --chown=appuser:appgroup /app/extracted/snapshot-dependencies/ ./
COPY --from=layers --chmod=550 --chown=appuser:appgroup /app/extracted/application/           ./

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

USER appuser
EXPOSE 8081
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "org.springframework.boot.loader.launch.JarLauncher"]