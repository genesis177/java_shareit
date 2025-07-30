FROM maven:3.9.5-eclipse-temurin-21 AS build

WORKDIR /app

# Копируем все pom.xml файлы
COPY pom.xml .
COPY server/pom.xml ./server/
COPY gateway/pom.xml ./gateway/

# Загружаем зависимости для конкретного модуля
RUN mvn dependency:go-offline -B -f server/pom.xml

# Копируем исходный код
COPY server/src ./server/src

# Собираем проект
RUN mvn clean package -f server/pom.xml -DskipTests

# Финальный образ
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/server/target/*.jar app.jar

EXPOSE 9090

ENTRYPOINT ["java", "-jar", "app.jar"]