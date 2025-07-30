FROM maven:3.9.5-eclipse-temurin-21 AS build

WORKDIR /app

# Копируем корневой pom.xml и pom.xml модуля
COPY pom.xml .
COPY gateway/pom.xml ./gateway/

# Загружаем зависимости для модуля gateway
RUN mvn dependency:go-offline -B -f gateway/pom.xml

# Копируем исходный код gateway
COPY gateway/src ./gateway/src

# Собираем проект
RUN mvn clean package -f gateway/pom.xml -DskipTests

# Финальный образ
FROM eclipse-temurin:21-jre

WORKDIR /app

# Копируем jar из предыдущего этапа
COPY --from=build /app/gateway/target/*.jar app.jar

# Открываем порт
EXPOSE 8080

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "app.jar"]