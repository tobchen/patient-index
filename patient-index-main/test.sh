java -javaagent:../otel/opentelemetry-javaagent.jar \
    -Dotel.service.name=patient-index-main \
    -Dotel.traces.exporter=otlp \
    -Dotel.metrics.exporter=none \
    -Dotel.logs.exporter=none \
    -Dspring.datasource.url=jdbc:postgresql://localhost:5432/postgres \
    -Dspring.datasource.username=postgres -Dspring.datasource.password=password \
    -Dserver.port=8080 \
    -jar target/patient-index-main-0.0.1-SNAPSHOT.jar
