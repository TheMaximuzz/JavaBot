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

import java.sql.SQLException;

public class TelegramBot extends TelegramLongPollingBot {

    private String botUsername;
    private String botToken;

    // таблица команд: ключ - команда, значение - реализация команды
    private final Map<String, BiConsumer<String, StringBuilder>> commandMap = new HashMap<>();
    private final StringBuilder helpText = new StringBuilder();

    private DatabaseManager databaseManager = new DatabaseManager(this);

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
        commandMap.put(command, action); //когда команда будет вызвана, действие будет выполнено.
        helpText.append(command).append(" - ").append(description).append("\n"); //добавляет команду и её описание в текст справки
    }

    private void registerDefaultCommands() {
        registerCommand("/start", "Начало работы с ботом", (chatId, builder) -> {
            builder.append("Доброго времени суток! Используйте /help для списка команд.");
        });

        registerCommand("/authors", "Информация об авторах", (chatId, builder) -> {
            builder.append("Бот был разработан командой гениев: Богдан Богатырев и Максим Наторин");
        });

        registerCommand("/help", "Список команд", (chatId, builder) -> {
            builder.append("Доступные команды:\n").append(helpText.toString());
        });

        registerCommand("/info", "Информация о боте", (chatId, builder) -> {
            builder.append("Бот для фитнеса и здоровья: \nЭтот бот поможет пользователям следить за своим здоровьем, предлагать тренировки, рецепты и советы по питанию.");
        });
        registerCommand("/createprofile", "Создать профиль", (chatId, builder) -> {
            builder.append("Пожалуйста, введите ваш никнейм:");
            databaseManager.setUserState(Long.parseLong(chatId), UserState.ENTER_NICKNAME);
            databaseManager.addUserProfile(Long.parseLong(chatId), new UserProfile()); // Создаем пустой профиль для пользователя
        });

        registerCommand("/viewprofile", "Посмотреть данные профиля", (chatId, builder) -> {
            try {
                builder.append(databaseManager.getUserProfileAsString(Long.parseLong(chatId)));
            } catch (SQLException e) {
                builder.append("Ошибка при получении профиля. Попробуйте позже.");
                e.printStackTrace();
            }
        });

        registerCommand("/deleteprofile", "Удалить профиль", (chatId, builder) -> {
            try {
                builder.append(databaseManager.deleteUserProfileAsString(Long.parseLong(chatId)));
            } catch (SQLException e) {
                builder.append("Ошибка при удалении профиля. Попробуйте позже.");
                e.printStackTrace();
            }
        });
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
            long userId = message.getChatId();

            if (databaseManager.getUserState(userId) != null) {
                databaseManager.handleProfileCreation(userId, text);
            } else {
                // Выполняем команду
                BiConsumer<String, StringBuilder> action = commandMap.getOrDefault(text, (id, builder) -> {
                    builder.append("Неизвестная команда. Используйте /help для списка команд.");
                });

                StringBuilder responseBuilder = new StringBuilder();
                action.accept(String.valueOf(userId), responseBuilder);
                sendMsg(String.valueOf(userId), responseBuilder.toString());
            }
        }
    }

    // Метод для отправки сообщений
    public void sendMsg(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    //возвращает карту commandMap, которая содержит команды и связанные с ними действия.
    public Map<String, BiConsumer<String, StringBuilder>> getCommandMap() {
        return commandMap;
    }
}
