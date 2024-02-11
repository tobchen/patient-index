FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY ./pom.xml .
RUN mvn dependency:go-offline
COPY ./src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:21.0.1_12-jre-alpine
COPY --from=build /app/target/*.jar /app/main.jar
ENTRYPOINT [ "java", "-jar", "/app/main.jar" ]
