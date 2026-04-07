FROM eclipse-temurin:21-jre

WORKDIR /app

COPY employee-launcher/target/otel-employee.jar otel-employee.jar

ENTRYPOINT java -jar otel-employee.jar