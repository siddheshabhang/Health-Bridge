FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system healthbridge \
    && useradd --system --gid healthbridge --home-dir /app --shell /usr/sbin/nologin healthbridge

COPY --chown=healthbridge:healthbridge services/admin-reporting-service/target/admin-reporting-service-*.jar /app/admin-reporting-service.jar

USER healthbridge

EXPOSE 8088

ENTRYPOINT ["java", "-jar", "/app/admin-reporting-service.jar"]
