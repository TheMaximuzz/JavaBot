package org.example.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TelegramBotTest2 {

    @Mock
    private DatabaseManager databaseManager;  // Мокаем класс DatabaseManager

    @InjectMocks
    private TelegramBot telegramBot;  // Подключаем мокнутый DatabaseManager к нашему боту

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCreateUserProfile() throws SQLException {
        long userId = 123456L;
        String nickname = "TestUser";
        int age = 25;
        int height = 180;
        int weight = 75;

        // Настраиваем поведение моков
        doNothing().when(databaseManager).createUserProfile(userId, nickname, age, height, weight);
        when(databaseManager.getUserProfileAsString(userId)).thenReturn("TestUser, age 25");

        databaseManager.createUserProfile(userId, nickname, age, height, weight);

        String profile = databaseManager.getUserProfileAsString(userId);
        assertNotNull(profile, "Профиль должен быть создан");
        assertTrue(profile.contains("TestUser"), "Профиль должен содержать никнейм");

        // Проверка, что методы были вызваны
        verify(databaseManager, times(1)).createUserProfile(userId, nickname, age, height, weight);
        verify(databaseManager, times(1)).getUserProfileAsString(userId);
    }

    @Test
    public void testDeleteUserProfile() throws SQLException {
        long userId = 123456L;

        // Настраиваем поведение моков
        when(databaseManager.deleteUserProfileAsString(userId)).thenReturn("Ваш профиль успешно удален.");
        when(databaseManager.getUserProfileAsString(userId)).thenReturn(null);

        String deleteMessage = databaseManager.deleteUserProfileAsString(userId);
        assertEquals("Ваш профиль успешно удален.", deleteMessage);

        String profile = databaseManager.getUserProfileAsString(userId);
        assertNull(profile, "Профиль должен быть удалён");

        verify(databaseManager, times(1)).deleteUserProfileAsString(userId);
        verify(databaseManager, times(1)).getUserProfileAsString(userId);
    }

    @Test
    public void testProfileAlreadyExists() throws SQLException {
        long userId = 123456L;

        when(databaseManager.isProfileExists(userId)).thenReturn(true);
        doThrow(new SQLException("Profile already exists")).when(databaseManager)
                .createUserProfile(userId, "TestUser", 25, 180, 75);

        assertTrue(databaseManager.isProfileExists(userId), "Профиль должен существовать");

        SQLException exception = assertThrows(SQLException.class, () -> {
            databaseManager.createUserProfile(userId, "TestUser", 25, 180, 75);
        });

        assertTrue(exception.getMessage().contains("already exists"), "Должно быть сообщение, что профиль уже существует");

        verify(databaseManager, times(1)).isProfileExists(userId);
        verify(databaseManager, times(1)).createUserProfile(userId, "TestUser", 25, 180, 75);
    }
}
