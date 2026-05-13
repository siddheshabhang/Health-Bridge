FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system healthbridge \
    && useradd --system --gid healthbridge --home-dir /app --shell /usr/sbin/nologin healthbridge

COPY --chown=healthbridge:healthbridge services/hospital-a-service/target/hospital-a-service-*.jar /app/hospital-a-service.jar

USER healthbridge

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "/app/hospital-a-service.jar"]
