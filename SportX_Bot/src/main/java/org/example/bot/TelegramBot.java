package org.example.bot;

// Импортируйте LoggerUtil
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

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

    private final Map<String, BiConsumer<String, StringBuilder>> commandMap = new HashMap<>();
    private final StringBuilder helpText = new StringBuilder();

    private DatabaseManager databaseManager = new DatabaseManager(this);

    public TelegramBot() {
        loadConfig();
        registerDefaultCommands(); // регистрация команд по умолчанию
    }

    private void loadConfig() {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            properties.load(input);
            this.botToken = properties.getProperty("bot.token");
            this.botUsername = properties.getProperty("bot.username");
        } catch (IOException e) {
            LoggerUtil.logError(0, "Ошибка при загрузке конфигурации: " + e.getMessage()); // Передаем 0, т.к. userId неизвестен
        }
    }

    // метод для регистрации команды
    public void registerCommand(String command, String description, BiConsumer<String, StringBuilder> action) {
        commandMap.put(command, action);
        helpText.append(command).append(" - ").append(description).append("\n");
    }

    private void registerDefaultCommands() {
        registerCommand("/start", "Начало работы с ботом", (chatId, builder) -> {
            builder.append("Доброго времени суток! Используйте /help для списка команд.");
            sendMsgWithKeyboard(chatId, builder.toString(), KeyboardMarkup.getMainMenuKeyboard());
            LoggerUtil.logInfo(Long.parseLong(chatId), "Пользователь начал взаимодействие с ботом.");
        });

        registerCommand("/authors", "Информация об авторах", (chatId, builder) -> {
            builder.append("Бот был разработан командой гениев: Богдан Богатырев и Максим Наторин");
            LoggerUtil.logInfo(Long.parseLong(chatId), "Пользователь запросил информацию об авторах.");
        });

        registerCommand("/help", "Список команд", (chatId, builder) -> {
            builder.append("Доступные команды:\n").append(helpText.toString());
            LoggerUtil.logInfo(Long.parseLong(chatId), "Пользователь запросил список команд.");
        });

        registerCommand("/info", "Информация о боте", (chatId, builder) -> {
            builder.append("Бот для фитнеса и здоровья: \nЭтот бот поможет пользователям следить за своим здоровьем, предлагать тренировки, рецепты и советы по питанию.");
            LoggerUtil.logInfo(Long.parseLong(chatId), "Пользователь запросил информацию о боте.");
        });

        registerCommand("/createprofile", "Создать профиль", (chatId, builder) -> {
            builder.append("Пожалуйста, введите ваш никнейм:");
            databaseManager.setUserState(Long.parseLong(chatId), UserState.ENTER_NICKNAME);
            databaseManager.addUserProfile(Long.parseLong(chatId), new UserProfile());
            LoggerUtil.logInfo(Long.parseLong(chatId), "Пользователь начал создание профиля.");
        });

        registerCommand("/viewprofile", "Посмотреть данные профиля", (chatId, builder) -> {
            try {
                builder.append(databaseManager.getUserProfileAsString(Long.parseLong(chatId)));
                LoggerUtil.logInfo(Long.parseLong(chatId), "Пользователь просмотрел свой профиль.");
            } catch (SQLException e) {
                builder.append("Ошибка при получении профиля. Попробуйте позже.");
                LoggerUtil.logError(Long.parseLong(chatId), "Ошибка при получении профиля: " + e.getMessage());
            }
        });

        registerCommand("/deleteprofile", "Удалить профиль", (chatId, builder) -> {
            try {
                builder.append(databaseManager.deleteUserProfileAsString(Long.parseLong(chatId)));
                LoggerUtil.logInfo(Long.parseLong(chatId), "Пользователь удалил свой профиль.");
            } catch (SQLException e) {
                builder.append("Ошибка при удалении профиля. Попробуйте позже.");
                LoggerUtil.logError(Long.parseLong(chatId), "Ошибка при удалении профиля: " + e.getMessage());
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

            String command = KeyboardMarkup.mapButtonTextToCommand(text);

            if (databaseManager.getUserState(userId) != null) {
                databaseManager.handleProfileCreation(userId, text);
            } else {
                // Выполняем команду
                BiConsumer<String, StringBuilder> action = commandMap.getOrDefault(command, (id, builder) -> {
                    builder.append("Неизвестная команда. Используйте /help для списка команд.");
                });

                StringBuilder responseBuilder = new StringBuilder();
                action.accept(String.valueOf(userId), responseBuilder);
                sendMsg(String.valueOf(userId), responseBuilder.toString());
                LoggerUtil.logInfo(userId, "Пользователь выполнил команду: " + command);
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
            LoggerUtil.logError(Long.parseLong(chatId), "Ошибка при отправке сообщения: " + e.getMessage());
        }
    }

    // Метод для отправки сообщений с клавиатурой
    public void sendMsgWithKeyboard(String chatId, String text, ReplyKeyboardMarkup keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(keyboardMarkup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            LoggerUtil.logError(Long.parseLong(chatId), "Ошибка при отправке сообщения с клавиатурой: " + e.getMessage());
        }
    }

    // возвращает карту commandMap, которая содержит команды и связанные с ними действия.
    public Map<String, BiConsumer<String, StringBuilder>> getCommandMap() {
        return commandMap;
    }
}
