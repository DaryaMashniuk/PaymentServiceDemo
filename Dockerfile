FROM maven:3.9.11-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline -B

COPY src src

RUN ./mvnw clean verify -DskipTests


FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd \
        --system \
        --uid 1001 \
        spring

COPY --from=builder \
     /app/target/*.jar \
     app.jar

RUN chown spring:spring app.jar

USER spring

EXPOSE 8080

ENTRYPOINT [
    "java",
    "-XX:+UseContainerSupport",
    "-jar",
    "app.jar"
]