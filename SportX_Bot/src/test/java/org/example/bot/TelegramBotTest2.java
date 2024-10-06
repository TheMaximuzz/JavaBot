package org.example.bot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
public class TelegramBotTest2 {

    // Инициализация контейнера PostgreSQL для тестов
    @Container
    private PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    private DatabaseManager databaseManager;
    private TelegramBot telegramBot;

    @BeforeEach
    public void setUp() {
        // Создаем экземпляр TelegramBot
        telegramBot = new TelegramBot();

        // Инициализация DatabaseManager с экземпляром TelegramBot
        databaseManager = new DatabaseManager(telegramBot);

        // Переопределяем параметры подключения к базе данных для тестов
        databaseManager.setDbConfig(postgreSQLContainer.getJdbcUrl(), postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword());
    }

    @AfterEach
    public void tearDown() throws SQLException {
        // Очистка базы данных после каждого теста
        databaseManager.disconnect();
    }

    @Test
    public void testCreateUserProfile() throws SQLException {
        // Подключаемся к базе данных
        databaseManager.connect();

        // Создаем новый профиль пользователя
        long userId = 123456L;
        String nickname = "TestUser";
        int age = 25;
        int height = 180;
        int weight = 75;

        databaseManager.createUserProfile(userId, nickname, age, height, weight);

        // Проверяем, что профиль был создан
        String profile = databaseManager.getUserProfileAsString(userId);
        assertNotNull(profile, "Профиль должен быть создан");
        assertTrue(profile.contains("TestUser"), "Профиль должен содержать никнейм");
    }

    @Test
    public void testDeleteUserProfile() throws SQLException {
        // Подключаемся к базе данных
        databaseManager.connect();

        // Создаем новый профиль пользователя
        long userId = 123456L;
        databaseManager.createUserProfile(userId, "TestUser", 25, 180, 75);

        // Удаляем профиль пользователя
        String deleteMessage = databaseManager.deleteUserProfileAsString(userId);
        assertEquals("Ваш профиль успешно удален.", deleteMessage);

        // Проверяем, что профиль был удалён
        String profile = databaseManager.getUserProfileAsString(userId);
        assertNull(profile, "Профиль должен быть удалён");
    }

    @Test
    public void testProfileAlreadyExists() throws SQLException {
        // Подключаемся к базе данных
        databaseManager.connect();

        // Создаем новый профиль пользователя
        long userId = 123456L;
        databaseManager.createUserProfile(userId, "TestUser", 25, 180, 75);

        // Проверяем, что профиль существует
        assertTrue(databaseManager.isProfileExists(userId), "Профиль должен существовать");

        // Пытаемся создать тот же профиль снова, должна быть ошибка
        SQLException exception = assertThrows(SQLException.class, () -> {
            databaseManager.createUserProfile(userId, "TestUser", 25, 180, 75);
        });

        assertTrue(exception.getMessage().contains("already exists"), "Должно быть сообщение, что профиль уже существует");
    }
}
