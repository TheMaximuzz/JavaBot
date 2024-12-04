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

    // захват вывода, который обычно перенаправляется в консоль
    private final ByteArrayOutputStream logContent = new ByteArrayOutputStream();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        clearLogFile();
        System.setOut(new PrintStream(logContent)); // Перенаправляем вывод в лог
    }

    private void clearLogFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LoggerUtil.getLogFilePath()))) {
            writer.write("");
        } catch (IOException e) {
            System.err.println("Ошибка при очистке лог файла: " + e.getMessage());
        }
    }


    @Test
    public void testLogoutFunction() throws SQLException {
        long userId = 123456L;

        doNothing().when(databaseManager).logoutUser(anyLong());
        databaseManager.logoutUser(userId);

        LoggerUtil.logInfo(userId, "User " + userId + " logged out");

        String logOutput = readLogFile();
        assertTrue(logOutput.contains("User " + userId + " logged out"),
                "Лог должен содержать сообщение о выходе пользователя. Log content: " + logOutput);
    }

    @Test
    public void testLogsIn() {
        long userId = 123456L;
        String input = "TestInput";

        doNothing().when(databaseManager).handleProfileCreationOrLogin(userId, input);

        // Вызываем метод
        databaseManager.handleProfileCreationOrLogin(userId, input);

        // Проверка, что метод был вызван один раз
        verify(databaseManager, times(1)).handleProfileCreationOrLogin(userId, input);
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

    /* Изменения в тестах
    1. проверка, что при вызове профиля у нас переход в новое состояние.
    2. проверка, что при вызове этой команды, у нас появляется список того, что можно отредактировать и что этот способ полный
    3. не надо мокать сам тг бот, надо чекнуть, что когда приходят данные на какое-то, но тут уже можно через handle, не вызывая на прямую */

    // Тест 1: Проверка перехода в состояние при вызове "/editprofile"
    @Test
    public void testEditProfileCommand() throws SQLException {
        TelegramBot bot = new TelegramBot();
        DatabaseManager mockDatabaseManager = mock(DatabaseManager.class);
        bot.setDatabaseManager(mockDatabaseManager);

        long userId = 12345L;
        String chatId = String.valueOf(userId);

        // мы залогинились
        when(mockDatabaseManager.isUserLoggedIn(userId)).thenReturn(true);

        StringBuilder responseBuilder = new StringBuilder();
        bot.getCommandMap().get("/editprofile").accept(chatId, responseBuilder);

        // как раз проверка на состояние
        verify(mockDatabaseManager).setUserState(userId, UserState.EDIT_PROFILE_LOGIN);

        String expectedResponse = "Выберите, что хотите изменить:";
        assertTrue(responseBuilder.toString().contains(expectedResponse));
    }

    // Тест 3: Проверка того, что данные принимаются и устанавливаются верно
    @Test
    public void testEditUserProfileAttributes() throws SQLException {
        long userId = 123456L;
        UserProfile existingProfile = new UserProfile(userId, "OldLogin", "OldPassword", "OldNickname", 25, 180, 75, true);

        when(databaseManager.getUserProfile(userId)).thenReturn(existingProfile);

        UserProfile profileToUpdate = databaseManager.getUserProfile(userId);
        profileToUpdate.setLogin("NewLogin");
        profileToUpdate.setPassword("NewPassword123");
        profileToUpdate.setNickname("UpdatedNickname");
        profileToUpdate.setAge(28);
        profileToUpdate.setHeight(185);
        profileToUpdate.setWeight(80);

        databaseManager.addUserProfile(userId, profileToUpdate);
        verify(databaseManager, times(1)).addUserProfile(eq(userId), eq(profileToUpdate));

        LoggerUtil.logInfo(userId, "Profile updated");

        String logOutput = readLogFile();
        assertTrue(logOutput.contains("Profile updated"),
                "Лог должен содержать сообщение об обновлении профиля.");
    }

}
