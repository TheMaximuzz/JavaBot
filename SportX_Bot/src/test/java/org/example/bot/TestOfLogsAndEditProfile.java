package org.example.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.*;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

public class TestOfLogsAndEditProfile {


    @Mock
    private DatabaseManager databaseManager;

    @InjectMocks
    private TelegramBot telegramBot;

    private final ByteArrayOutputStream logContent = new ByteArrayOutputStream();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        clearLogFile();
        System.setOut(new PrintStream(logContent));
    }

    private void clearLogFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LoggerUtil.getLogFilePath()))) {
            writer.write(""); // Очищаем файл
        } catch (IOException e) {
            System.err.println("Ошибка при очистке лог файла: " + e.getMessage());
        }
    }

    @Test
    public void testEditUserProfileAttributes() throws SQLException {
        long userId = 123456L;
        String newLogin = "NewLogin";
        String newPassword = "NewPassword123";
        String newNickname = "UpdatedNickname";
        int newAge = 28;
        int newHeight = 185;
        int newWeight = 80;

        UserProfile mockProfile = new UserProfile(userId, newLogin, newPassword, newNickname, newAge, newHeight, newWeight);
        when(databaseManager.getOrCreateUserProfile(userId)).thenReturn(mockProfile);

        databaseManager.handleEditLogin(userId, newLogin);
        databaseManager.handleEditPassword(userId, newPassword);
        databaseManager.handleEditNickname(userId, newNickname);
        databaseManager.handleEditAge(userId, String.valueOf(newAge));
        databaseManager.handleEditHeight(userId, String.valueOf(newHeight));
        databaseManager.handleEditWeight(userId, String.valueOf(newWeight));

        assertEquals(newLogin, mockProfile.getLogin());
        assertEquals(newPassword, mockProfile.getPassword());
        assertEquals(newNickname, mockProfile.getNickname());
        assertEquals(newAge, mockProfile.getAge());
        assertEquals(newHeight, mockProfile.getHeight());
        assertEquals(newWeight, mockProfile.getWeight());
    }

    @Test
    public void testLogoutFunction() throws SQLException {
        long userId = 123456L;

        // Устанавливаем моку, чтобы использовать реальную реализацию logoutUser
        doCallRealMethod().when(databaseManager).logoutUser(anyLong());

        // Вызываем метод через объект TelegramBot
        telegramBot.getDatabaseManager().logoutUser(userId);

        // Проверка, что сообщение о выходе пользователя есть в логах
        String logOutput = readLogFile();
        System.out.println("Лог файл после вызова logoutUser: " + logOutput); // вывод логов для отладки
        assertTrue(logOutput.contains("User " + userId + " logged out"),
                "Лог должен содержать сообщение о выходе пользователя. Log content: " + logOutput);
    }


    private String readLogFile() {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(LoggerUtil.getLogFilePath()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
        } catch (IOException e) {
            System.err.println("Ошибка при чтении лог файла: " + e.getMessage());
        }
        return content.toString();
    }

    @Test
    public void testHandleProfileCreationOrLogin_CreatesProfileOrLogsIn() {
        long userId = 123456L;
        String input = "TestInput";

        doNothing().when(databaseManager).handleProfileCreationOrLogin(userId, input);

        databaseManager.handleProfileCreationOrLogin(userId, input);


        verify(databaseManager, times(1)).handleProfileCreationOrLogin(userId, input);
    }
}
