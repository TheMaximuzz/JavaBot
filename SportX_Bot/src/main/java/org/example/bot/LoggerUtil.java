package org.example.bot;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggerUtil {

    private static final String LOG_FILE_PATH = "bot_activity.log";

    public static String getLogFilePath() {
        return LOG_FILE_PATH;
    }

    // Метод для логирования информации
    public static void logInfo(long userId, String message) {
        log("INFO", userId, message);
    }

    // Метод для логирования ошибок
    public static void logError(long userId, String message) {
        log("ERROR", userId, message);
    }

    // Общий метод для записи логов
    private static void log(String level, long userId, String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String logMessage = String.format("[%s] [%s] [User ID: %d] %s", timestamp, level, userId, message);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE_PATH, true))) {
            writer.write(logMessage);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Ошибка при записи в лог файл: " + e.getMessage());
        }
    }
}
