# Используем базовый образ OpenJDK
FROM openjdk:17-jdk-alpine

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем файлы проекта в контейнер
COPY . /app

# Устанавливаем зависимости
RUN apk add --no-cache maven
RUN mvn clean install

# Указываем команду для запуска приложения
CMD ["java", "-jar", "target/SportX_Bot-1.0-SNAPSHOT.jar"]
