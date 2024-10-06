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
    // Карта для обработки состояний регистрации профиля
    private Map<UserState, BiConsumer<Long, String>> stateHandlers = new HashMap<>();



    private enum UserState {
        ENTER_NICKNAME,
        ENTER_AGE,
        ENTER_HEIGHT,
        ENTER_WEIGHT,
        COMPLETED
    }

    // Временный класс для хранения данных профиля пользователя
    class UserProfile {
        String nickname;
        int age;
        int height;
        int weight;
    }

    public TelegramBot() {
        loadConfig();
        registerDefaultCommands(); // тут регистрация команд по умолчанию
        initializeStateHandlers();  // Инициализация хэндлеров состояния
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
            userStates.put(Long.parseLong(chatId), UserState.ENTER_NICKNAME);
            userProfiles.put(Long.parseLong(chatId), new UserProfile()); // Создаем пустой профиль для пользователя
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

            if (userStates.containsKey(userId)) {
                handleProfileCreation(userId, text);
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

    /*
    stateHandlers: это карта, которая связывает состояния пользователя с действиями, которые должны быть выполнены на каждом этапе.
userStates: карта, которая хранит текущее состояние каждого пользователя (например, ждет ввода никнейма, возраста и т.д.).
userProfiles: временное хранилище для информации профиля пользователя, которая собирается поэтапно.
databaseManager: объект для работы с базой данных, который используется для подключения, отключения и выполнения запросов.
     */

    private void handleProfileCreation(long userId, String input) {
        UserState state = userStates.get(userId);
        BiConsumer<Long, String> handler = stateHandlers.get(state);

        if (handler != null) {
            handler.accept(userId, input);
        } else {
            sendMsg(String.valueOf(userId), "Процесс создания профиля завершен.");
            userStates.remove(userId);
        }
    }

    private void initializeStateHandlers() {
        stateHandlers.put(UserState.ENTER_NICKNAME, (userId, input) -> {
            try {
                databaseManager.handleNickname(userId, input);
                sendMsg(String.valueOf(userId), "Введите ваш возраст:");
                userStates.put(userId, UserState.ENTER_AGE);
            } catch (SQLException e) {
                sendMsg(String.valueOf(userId), "У вас уже есть профиль. Регистрация отменена.");
                userStates.remove(userId);
                e.printStackTrace();
            }
        });

        stateHandlers.put(UserState.ENTER_AGE, (userId, input) -> {
            try {
                databaseManager.handleAge(userId, input);
                sendMsg(String.valueOf(userId), "Введите ваш рост (в см):");
                userStates.put(userId, UserState.ENTER_HEIGHT);
            } catch (NumberFormatException e) {
                sendMsg(String.valueOf(userId), "Пожалуйста, введите корректный возраст.");
            }
        });

        stateHandlers.put(UserState.ENTER_HEIGHT, (userId, input) -> {
            try {
                databaseManager.handleHeight(userId, input);
                sendMsg(String.valueOf(userId), "Введите ваш вес (в кг):");
                userStates.put(userId, UserState.ENTER_WEIGHT);
            } catch (NumberFormatException e) {
                sendMsg(String.valueOf(userId), "Пожалуйста, введите корректный рост.");
            }
        });

        stateHandlers.put(UserState.ENTER_WEIGHT, (userId, input) -> {
            try {
                databaseManager.handleWeight(userId, input);
                sendMsg(String.valueOf(userId), "Ваш профиль успешно создан!");
                userStates.remove(userId);
            } catch (NumberFormatException e) {
                sendMsg(String.valueOf(userId), "Пожалуйста, введите корректный вес.");
            } catch (SQLException e) {
                sendMsg(String.valueOf(userId), "Ошибка при сохранении профиля. Попробуйте позже.");
                e.printStackTrace();
            }
        });
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

//возвращает карту commandMap, которая содержит команды и связанные с ними действия.
    public Map<String, BiConsumer<String, StringBuilder>> getCommandMap() {
        return commandMap;
    }
}
