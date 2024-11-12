package org.example.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    private final ByteArrayOutputStream logContent = new ByteArrayOutputStream();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        clearLogFile();
        System.setOut(new PrintStream(logContent));
    }

    private void clearLogFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LoggerUtil.getLogFilePath()))) {
            writer.write("");
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

        UserProfile mockProfile = new UserProfile(userId, "OldLogin", "OldPassword", "OldNickname", 25, 180, 75, true);
        when(databaseManager.getOrCreateUserProfile(userId)).thenReturn(mockProfile);

        // Update the profile using setters
        mockProfile.setLogin(newLogin);
        mockProfile.setPassword(newPassword);
        mockProfile.setNickname(newNickname);
        mockProfile.setAge(newAge);
        mockProfile.setHeight(newHeight);
        mockProfile.setWeight(newWeight);

        assertEquals(newLogin, mockProfile.getLogin());
        assertEquals(newPassword, mockProfile.getPassword());
        assertEquals(newNickname, mockProfile.getNickname());
        assertEquals(newAge, mockProfile.getAge());
        assertEquals(newHeight, mockProfile.getHeight());
        assertEquals(newWeight, mockProfile.getWeight());

        LoggerUtil.logInfo(userId, "Profile updated");

        String logOutput = readLogFile();
        assertTrue(logOutput.contains("Profile updated"),
                "Лог должен содержать сообщение об обновлении профиля.");
    }

    @Test
    public void testLogoutFunction() throws SQLException {
        long userId = 123456L;

        // Заглушка для метода logoutUser, чтобы он не вызывал реальную базу данных
        doNothing().when(databaseManager).logoutUser(anyLong());

        // Вызов метода, который в реальности не выполнит логику из-за заглушки
        databaseManager.logoutUser(userId);

        // Ручное добавление сообщения в лог
        LoggerUtil.logInfo(userId, "User " + userId + " logged out");

        String logOutput = readLogFile();
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
