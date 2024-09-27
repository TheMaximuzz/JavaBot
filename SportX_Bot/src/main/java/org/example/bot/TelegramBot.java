package org.example.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;

public class TelegramBot extends TelegramLongPollingBot {

    private String botUsername;
    private String botToken;

    // таблица команд: ключ - команда, значение - реализация команды
    private final Map<String, BiConsumer<String, StringBuilder>> commandMap = new HashMap<>();
    private final StringBuilder helpText = new StringBuilder();

    public TelegramBot() {
        loadConfig();
        registerDefaultCommands(); // тут регистрация команд по умолчанию
    }

    private void loadConfig() {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            properties.load(input);
            this.botToken = properties.getProperty("bot.token");
            this.botUsername = properties.getProperty("bot.username");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // метод для регистрации команды
    public void registerCommand(String command, String description, BiConsumer<String, StringBuilder> action) {
        commandMap.put(command, action);
        helpText.append(command).append(" - ").append(description).append("\n");
    }

    private void registerDefaultCommands() {
        registerCommand("/start", "Начало работы с ботом", (chatId, builder) ->
                sendMsg(chatId, "Доброго времени суток! Используйте /help для списка команд."));

        registerCommand("/authors", "Информация об авторах", (chatId, builder) ->
                sendMsg(chatId, "Бот был разработан командой гениев: Богдан Богатырев и Максим Наторин"));

        registerCommand("/help", "Список команд", (chatId, builder) ->
                sendMsg(chatId, "Доступные команды:\n" + builder.toString()));

        registerCommand("/info", "Информация о боте", (chatId, builder) ->
                sendMsg(chatId, "Бот для фитнеса и здоровья: \n" +
                        "Этот бот поможет пользователям следить за своим здоровьем, предлагать тренировки, рецепты и советы по питанию."));
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String text = message.getText();
            String chatId = message.getChatId().toString();

            // если команда E --> она выполняется
            BiConsumer<String, StringBuilder> action = commandMap.getOrDefault(text, (id, builder) -> sendMsg(id, "Неизвестная команда. Используйте /help для списка команд."));
            action.accept(chatId, helpText);
        }
    }

    // Метод для отправки сообщений
    private void sendMsg(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
