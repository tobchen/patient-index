java -javaagent:../otel/opentelemetry-javaagent.jar \
    -Dotel.service.name=patient-index-feed \
    -Dotel.traces.exporter=otlp \
    -Dotel.metrics.exporter=none \
    -Dotel.logs.exporter=none \
    -Dspring.datasource.url=jdbc:postgresql://localhost:5442/postgres \
    -Dspring.datasource.username=postgres -Dspring.datasource.password=password \
    -jar target/patient-index-feed-0.0.1-SNAPSHOT.jar
