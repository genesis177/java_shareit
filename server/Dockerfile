FROM maven:3.9.5-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY server/pom.xml ./server/
COPY gateway/pom.xml ./gateway/

RUN mvn dependency:go-offline -B -f server/pom.xml

COPY server/src ./server/src

RUN mvn clean package -f server/pom.xml -DskipTests

FROM eclipse-temurin:21-jre

# Устанавливаем curl для health check
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /app/server/target/*.jar app.jar

EXPOSE 9090

ENTRYPOINT ["java", "-jar", "app.jar"]