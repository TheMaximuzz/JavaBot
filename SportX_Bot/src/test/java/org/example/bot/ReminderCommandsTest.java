package org.example.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

public class ReminderCommandsTest {

    private static final Logger logger = LoggerFactory.getLogger(ReminderCommandsTest.class);

    @Mock
    private DatabaseConnection dbConnection;

    @Mock
    private ReminderManager reminderManager;

    @InjectMocks
    private TelegramBot telegramBot;

    @BeforeEach
    public void setUp() {
        logger.info("Setting up test environment");
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testDeleteReminderCommand() throws SQLException {
        logger.info("Starting testDeleteReminderCommand");
        long chatId = 123456L;
        int reminderId = 1;
        String input = String.valueOf(reminderId);

        Update update = mock(Update.class);
        Message message = mock(Message.class);
        when(update.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(chatId);
        when(message.getText()).thenReturn(input);

        telegramBot.setUserState(chatId, UserState.DELETE_REMINDER);
        telegramBot.onUpdateReceived(update);

        verify(reminderManager, times(1)).deleteReminder(reminderId);
        verify(telegramBot, times(1)).sendMsg(String.valueOf(chatId), "Напоминание удалено успешно!");
        logger.info("testDeleteReminderCommand completed successfully");
    }

    @Test
    public void testViewRemindersCommand() throws SQLException {
        logger.info("Starting testViewRemindersCommand");
        long chatId = 123456L;
        List<String> reminders = Arrays.asList("Номер: 1, Описание: Тренировка, Дата: 15-12-2024 10:00");

        Update update = mock(Update.class);
        Message message = mock(Message.class);
        when(update.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(chatId);
        when(message.getText()).thenReturn("/viewreminders");

        when(reminderManager.getReminders(chatId)).thenReturn(reminders);

        telegramBot.onUpdateReceived(update);

        verify(reminderManager, times(1)).getReminders(chatId);
        verify(telegramBot, times(1)).sendMsg(String.valueOf(chatId), "Номер: 1, Описание: Тренировка, Дата: 15-12-2024 10:00\n");
        logger.info("testViewRemindersCommand completed successfully");
    }

    @Test
    public void testAddReminderCommand() throws SQLException {
        logger.info("Starting testAddReminderCommand");
        long chatId = 123456L;
        String input = "Тренировка 15-12-2024 10:00";

        Update update = mock(Update.class);
        Message message = mock(Message.class);
        when(update.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(chatId);
        when(message.getText()).thenReturn(input);

        telegramBot.setUserState(chatId, UserState.ADD_REMINDER);
        telegramBot.onUpdateReceived(update);

        verify(reminderManager, times(1)).addReminder(eq(chatId), eq("Тренировка"), eq("2024-12-15 10:00:00"));
        verify(telegramBot, times(1)).sendMsg(String.valueOf(chatId), "Напоминание добавлено успешно!");
        logger.info("testAddReminderCommand completed successfully");
    }
}
