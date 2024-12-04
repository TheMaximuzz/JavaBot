package org.example.bot;

import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.*;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class TestOfLogsAndEditProfile {

    @Mock
    private DatabaseManager databaseManager;

    @Mock
    private TelegramBot telegramBot;

    // захват вывода, который обычно перенаправляется в консоль
    private final ByteArrayOutputStream logContent = new ByteArrayOutputStream();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this); // Инициализация моков
        System.setOut(new PrintStream(logContent)); // Перенаправляем вывод в лог
        telegramBot = new TelegramBot(); // Создаем экземпляр TelegramBot
        telegramBot.setDatabaseManager(databaseManager); // Инъектируем мок DatabaseManager
    }

    private void clearLogFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LoggerUtil.getLogFilePath()))) {
            writer.write("");  // Очищаем лог файл перед каждым тестом
        } catch (IOException e) {
            System.err.println("Ошибка при очистке лог файла: " + e.getMessage());
        }
    }


    @Test
    public void testLogoutFunction() throws SQLException {
        long userId = 123456L;

        // Мокаем logoutUser, чтобы не вызывать реальную базу данных
        doNothing().when(databaseManager).logoutUser(anyLong());

        // Выполняем выход пользователя
        databaseManager.logoutUser(userId);

        // Логируем выход
        LoggerUtil.logInfo(userId, "User " + userId + " logged out");

        // Чтение и проверка логов
        String logOutput = readLogFile();
        assertTrue(logOutput.contains("User " + userId + " logged out"),
                "Лог должен содержать сообщение о выходе пользователя. Log content: " + logOutput);
    }

    @Test
    public void testLogsIn() {
        long userId = 123456L;
        String input = "TestInput";

        // Мокаем handleProfileCreationOrLogin, чтобы не вызывать реальную логику
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

    @Test
    public void testEditProfileCommand() throws SQLException {
        long userId = 123456L;
        String chatId = "123456";
        String command = "/editprofile";

        // Мокаем методы DatabaseManager
        when(databaseManager.isUserLoggedIn(anyLong())).thenReturn(true);
        when(databaseManager.isProfileExists(anyLong())).thenReturn(true);
        doNothing().when(databaseManager).setUserState(anyLong(), any(UserState.class));

        // Мокаем методы TelegramBot
        doNothing().when(telegramBot).sendMsgWithInlineKeyboard(anyString(), anyString(), any(InlineKeyboardMarkup.class));

        // Создаем мок Update и Message
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn(command);
        when(message.getChatId()).thenReturn(userId);

        // Вызываем метод onUpdateReceived
        telegramBot.onUpdateReceived(update);

        // Проверяем, что состояние пользователя установлено в EDIT_PROFILE_LOGIN
        verify(databaseManager, times(1)).setUserState(userId, UserState.EDIT_PROFILE_LOGIN);

        // Проверяем, что отправлено сообщение с кнопками для редактирования профиля
        verify(telegramBot, times(1)).sendMsgWithInlineKeyboard(chatId, anyString(), any(InlineKeyboardMarkup.class));

        // Мокаем CallbackQuery для выбора редактирования логина
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        when(callbackQuery.getData()).thenReturn("edit_login");
        when(callbackQuery.getMessage()).thenReturn(message);

        // Создаем новый Update для CallbackQuery
        Update callbackUpdate = mock(Update.class);
        when(callbackUpdate.hasCallbackQuery()).thenReturn(true);
        when(callbackUpdate.getCallbackQuery()).thenReturn(callbackQuery);

        // Вызываем метод onUpdateReceived с CallbackQuery
        telegramBot.onUpdateReceived(callbackUpdate);

        // Проверяем, что состояние пользователя установлено в EDIT_PROFILE_LOGIN
        verify(databaseManager, times(1)).setUserState(userId, UserState.EDIT_PROFILE_LOGIN);

        // Проверяем, что отправлено сообщение с запросом нового логина
        verify(telegramBot, times(1)).sendMsg(chatId, "Введите новое значение для логина:");

        // Мокаем обновление логина
        String newLogin = "new_login";
        when(databaseManager.getUserState(userId)).thenReturn(UserState.EDIT_PROFILE_LOGIN);
        doNothing().when(databaseManager).handleProfileCreationOrLogin(userId, newLogin);

        // Создаем новый Update для нового логина
        Update newLoginUpdate = mock(Update.class);
        when(newLoginUpdate.hasMessage()).thenReturn(true);
        when(newLoginUpdate.getMessage()).thenReturn(message);
        when(message.getText()).thenReturn(newLogin);

        // Вызываем метод onUpdateReceived с новым логином
        telegramBot.onUpdateReceived(newLoginUpdate);

        // Проверяем, что логин обновлен
        verify(databaseManager, times(1)).handleProfileCreationOrLogin(userId, newLogin);
        verify(telegramBot, times(1)).sendMsg(chatId, "Логин обновлен.");
    }
}
