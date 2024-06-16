ARG MAVEN_VERSION=3
ARG JAVA_VERSION=21

FROM maven:${MAVEN_VERSION}-eclipse-temurin-${JAVA_VERSION}-alpine AS build
WORKDIR /app
COPY ./pom.xml .
RUN mvn dependency:go-offline
COPY ./src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine
COPY --from=build /app/target/*.jar /app/main.jar
EXPOSE 8080
CMD [ "java", "-jar", "/app/main.jar" ]
