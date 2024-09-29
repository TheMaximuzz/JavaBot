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
import java.util.HashMap;
import java.util.Map;

public class TelegramBot extends TelegramLongPollingBot {

    private String botUsername;
    private String botToken;

    // таблица команд: ключ - команда, значение - реализация команды
    private final Map<String, BiConsumer<String, StringBuilder>> commandMap = new HashMap<>();
    private final StringBuilder helpText = new StringBuilder();

    private DatabaseManager databaseManager = new DatabaseManager();

    // Карта для хранения состояний пользователей
    private Map<Long, UserState> userStates = new HashMap<>();
    // Карта для временного хранения данных профиля пользователей
    private Map<Long, UserProfile> userProfiles = new HashMap<>();

    private enum UserState {
        ENTER_NICKNAME,
        ENTER_AGE,
        ENTER_HEIGHT,
        ENTER_WEIGHT,
        COMPLETED
    }

    // Временный класс для хранения данных профиля пользователя
    private class UserProfile {
        String nickname;
        int age;
        int height;
        int weight;
    }

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
            userStates.put(Long.parseLong(chatId), UserState.ENTER_NICKNAME);
            userProfiles.put(Long.parseLong(chatId), new UserProfile()); // Создаем пустой профиль для пользователя
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
            String chatId = message.getChatId().toString();
            long userId = message.getChatId();

            // Проверяем, находится ли пользователь в процессе создания профиля
            if (userStates.containsKey(userId)) {
                handleProfileCreation(userId, text, chatId);
            } else {
                // Выполняем команду
                BiConsumer<String, StringBuilder> action = commandMap.getOrDefault(text, (id, builder) -> {
                    builder.append("Неизвестная команда. Используйте /help для списка команд.");
                });

                StringBuilder responseBuilder = new StringBuilder();
                action.accept(chatId, responseBuilder);
                sendMsg(chatId, responseBuilder.toString());
            }
        }
    }

    private void handleProfileCreation(long userId, String text, String chatId) {
        try {
            UserState state = userStates.get(userId);
            UserProfile profile = userProfiles.get(userId); // Получаем профиль пользователя
            switch (state) {
                case ENTER_NICKNAME:
                    profile.nickname = text; // Сохраняем никнейм
                    sendMsg(chatId, "Введите ваш возраст:");
                    userStates.put(userId, UserState.ENTER_AGE);
                    break;
                case ENTER_AGE:
                    try {
                        profile.age = Integer.parseInt(text); // Сохраняем возраст
                        sendMsg(chatId, "Введите ваш рост (в см):");
                        userStates.put(userId, UserState.ENTER_HEIGHT);
                    } catch (NumberFormatException e) {
                        sendMsg(chatId, "Пожалуйста, введите корректный возраст.");
                    }
                    break;
                case ENTER_HEIGHT:
                    try {
                        profile.height = Integer.parseInt(text); // Сохраняем рост
                        sendMsg(chatId, "Введите ваш вес (в кг):");
                        userStates.put(userId, UserState.ENTER_WEIGHT);
                    } catch (NumberFormatException e) {
                        sendMsg(chatId, "Пожалуйста, введите корректный рост.");
                    }
                    break;
                case ENTER_WEIGHT:
                    try {
                        profile.weight = Integer.parseInt(text); // Сохраняем вес
                        sendMsg(chatId, "Ваш профиль успешно создан!");
                        userStates.put(userId, UserState.COMPLETED);

                        // Сохранение данных профиля в базу данных
                        databaseManager.connect();
                        databaseManager.createUserProfile(userId, profile.nickname, profile.age, profile.height, profile.weight);
                        databaseManager.disconnect();

                        // Очистка данных профиля и состояния пользователя после завершения
                        userProfiles.remove(userId);
                        userStates.remove(userId);
                    } catch (NumberFormatException e) {
                        sendMsg(chatId, "Пожалуйста, введите корректный вес.");
                    }
                    break;
                default:
                    sendMsg(chatId, "Процесс создания профиля завершен.");
                    userStates.remove(userId); // Убираем состояние, когда профиль завершен
                    break;
            }
        } catch (SQLException e) {
            sendMsg(chatId, "Произошла ошибка при создании профиля. Попробуйте позже.");
            e.printStackTrace();
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

    public Map<String, BiConsumer<String, StringBuilder>> getCommandMap() {
        return commandMap;
    }
}
