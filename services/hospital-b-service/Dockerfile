FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system healthbridge \
    && useradd --system --gid healthbridge --home-dir /app --shell /usr/sbin/nologin healthbridge

COPY --chown=healthbridge:healthbridge services/hospital-b-service/target/hospital-b-service-*.jar /app/hospital-b-service.jar

USER healthbridge

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "/app/hospital-b-service.jar"]
