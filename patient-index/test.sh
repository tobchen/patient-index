java -javaagent:../otel/opentelemetry-javaagent.jar \
    -Dotel.service.name=patient-index \
    -Dotel.traces.exporter=otlp \
    -Dotel.metrics.exporter=none \
    -Dotel.logs.exporter=none \
    -Dspring.datasource.url=jdbc:postgresql://localhost:5432/postgres \
    -Dspring.datasource.username=postgres -Dspring.datasource.password=password \
    -Dspring.jpa.hibernate.ddl-auto=update \
    -Dserver.port=8080 \
    -jar target/patient-index-0.0.1-SNAPSHOT.jar
