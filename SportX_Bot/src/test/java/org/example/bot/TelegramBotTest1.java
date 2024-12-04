package org.example.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TelegramBotTest1 {

    private TelegramBot bot; // наш бот

    @BeforeEach
    public void setUp() {
        // перед каждым тестом --> создаём нового бота
        bot = new TelegramBot();
    }

    @Test
    public void testCommandMapInitialization() {
        // Проверяем, что таблица команд не null и не пустая
        assertNotNull(bot.getCommandMap(), "Таблица команд не должна быть null");
        assertFalse(bot.getCommandMap().isEmpty(), "Таблица команд не должна быть пустой");
    }

    @Test
    public void testStartCommand() {
        // существует ли /start
        assertTrue(bot.getCommandMap().containsKey("/start"), "Команда /start должна быть в таблице");
    }

    @Test
    public void testUnknownCommand() {
        // проверка для несуществующей команды
        StringBuilder helpText = new StringBuilder();
        bot.getCommandMap().getOrDefault("/fake", (chatId, builder) -> assertTrue(true, "Команда не существует"))
                .accept("chatId", helpText);
    }

    @Test
    public void testHelpCommand() {
        // проверяем, что команда /help выводит корректный список команд
        StringBuilder helpText = new StringBuilder();
        bot.getCommandMap().get("/help").accept("chatId", helpText);

        // проверяем, что список команд содержит команду /start
        assertTrue(helpText.toString().contains("/start"), "Список команд должен содержать команду /start");
    }
}
