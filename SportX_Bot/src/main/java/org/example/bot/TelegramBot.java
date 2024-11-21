package org.example.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.methods.ParseMode;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.Arrays;

import java.sql.SQLException;

public class TelegramBot extends TelegramLongPollingBot {

    private String botUsername;
    private String botToken;

    private final Map<String, BiConsumer<String, StringBuilder>> commandMap = new HashMap<>();
    private final StringBuilder helpText = new StringBuilder();

    private DatabaseManager databaseManager = new DatabaseManager(this);

    private final List<String> commandsRequiringAuth = Arrays.asList(
            "/viewprofile",
            "/deleteprofile",
            "/viewcourses",
            "/selectcourse",
            "/viewworkouts",
            "/viewexercises",
            "/logout",
            "/editprofile"
    );

    public TelegramBot() {
        loadConfig();
        registerDefaultCommands();
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

    public void registerCommand(String command, String description, BiConsumer<String, StringBuilder> action) {
        commandMap.put(command, action);
        helpText.append(command).append(" - ").append(description).append("\n");
    }

    private void registerDefaultCommands() {
        registerCommand("/start", "Начало работы с ботом", (chatId, builder) -> {
            builder.append("Приветствую тебя в нашем фитнес-боте!" + Icon.BICEPS.get() + Icon.TADA.get() + "\nМы поможем тебе похудеть или же набрать мышечную массу! Расскажем все тонкости фитнеса" + Icon.HAT.get());
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
            builder.append("Пожалуйста, введите ваш логин:");
            databaseManager.setUserState(Long.parseLong(chatId), UserState.CREATE_PROFILE_LOGIN);
            databaseManager.addUserProfile(Long.parseLong(chatId), new UserProfile());
            LoggerUtil.logInfo(Long.parseLong(chatId), "Пользователь начал создание профиля.");
        });

        registerCommand("/viewprofile", "Посмотреть данные профиля", (chatId, builder) -> {
            try {
                if (databaseManager.isUserLoggedIn(Long.parseLong(chatId))) {
                    builder.append(databaseManager.getUserProfileAsString(Long.parseLong(chatId)));
                    LoggerUtil.logInfo(Long.parseLong(chatId), "Пользователь просмотрел свой профиль.");
                } else {
                    builder.append("Ошибка. Зарегистрируйтесь или войдите в аккаунт.");
                }
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

        registerCommand("/viewcourses", "Посмотреть программы тренировок", (chatId, builder) -> {
            try {
                builder.append(databaseManager.getCoursesAsString());
                LoggerUtil.logInfo(Long.parseLong(chatId), "Пользователь просмотрел программы тренировок.");
            } catch (SQLException e) {
                builder.append("Ошибка при получении программ тренировок. Попробуйте позже.");
                LoggerUtil.logError(Long.parseLong(chatId), "Ошибка при получении программ тренировок: " + e.getMessage());
            }
        });

        registerCommand("/selectcourse", "Выбрать программу тренировок", (chatId, builder) -> {
            try {
                List<Course> courses = databaseManager.getCourses();
                InlineKeyboardMarkup keyboardMarkup = InlineKeyboardManager.getCourseSelectionKeyboard(courses);
                sendMsgWithInlineKeyboard(chatId, "Пожалуйста, выберите программу тренировок:", keyboardMarkup);
                databaseManager.setUserState(Long.parseLong(chatId), UserState.SELECT_COURSE); // Устанавливаем состояние пользователя
                LoggerUtil.logInfo(Long.parseLong(chatId), "Пользователь начал выбор программы тренировок.");
            } catch (SQLException e) {
                builder.append("Ошибка при получении программ тренировок. Попробуйте позже.");
                LoggerUtil.logError(Long.parseLong(chatId), "Ошибка при получении программ тренировок: " + e.getMessage());
            }
        });

        registerCommand("/viewworkouts", "Посмотреть тренировки в выбранной программе", (chatId, builder) -> {
            try {
                String workouts = databaseManager.getWorkoutsAsString(Long.parseLong(chatId));
                builder.append(workouts);
                LoggerUtil.logInfo(Long.parseLong(chatId), "Пользователь просмотрел тренировки в выбранной программе.");
            } catch (SQLException e) {
                builder.append("Ошибка при получении тренировок. Попробуйте позже.");
                LoggerUtil.logError(Long.parseLong(chatId), "Ошибка при получении тренировок: " + e.getMessage());
            }
        });

        registerCommand("/viewexercises", "Посмотреть упражнения в тренировке", (chatId, builder) -> {
            try {
                List<Workout> workouts = databaseManager.getWorkoutsWithCompletionStatus(Long.parseLong(chatId));
                InlineKeyboardMarkup keyboardMarkup = InlineKeyboardManager.getWorkoutSelectionKeyboard(workouts);
                sendMsgWithInlineKeyboard(chatId, "Пожалуйста, выберите курс тренировок", keyboardMarkup);
                LoggerUtil.logInfo(Long.parseLong(chatId), "Пользователь начал просмотр упражнений в тренировке.");
            } catch (SQLException e) {
                builder.append("Ошибка при получении тренировок. Попробуйте позже.");
                LoggerUtil.logError(Long.parseLong(chatId), "Ошибка при получении тренировок: " + e.getMessage());
            }
        });

        registerCommand("/login", "Войти в аккаунт", (chatId, builder) -> {
            builder.append("Пожалуйста, введите ваш логин:");
            databaseManager.setUserState(Long.parseLong(chatId), UserState.LOGIN_LOGIN);
            LoggerUtil.logInfo(Long.parseLong(chatId), "Пользователь начал процесс входа.");
        });

        registerCommand("/logout", "Выйти из профиля", (chatId, builder) -> {
            try {
                long telegramChatId = Long.parseLong(chatId);
                databaseManager.logoutUser(telegramChatId);
                builder.append("Вы успешно вышли из всех сессий.");
                LoggerUtil.logInfo(telegramChatId, "Пользователь вышел из всех сессий.");
            } catch (SQLException e) {
                builder.append("Ошибка при выходе из профиля. Попробуйте позже.");
                LoggerUtil.logError(Long.parseLong(chatId), "Ошибка при выходе из профиля: " + e.getMessage());
            }
        });

        registerCommand("/editprofile", "Редактировать профиль", (chatId, builder) -> {
            try {
                if (databaseManager.isUserLoggedIn(Long.parseLong(chatId))) {
                    builder.append("Выберите, что хотите изменить:");
                    sendMsgWithInlineKeyboard(chatId, builder.toString(), InlineKeyboardManager.getEditProfileKeyboard());
                    databaseManager.setUserState(Long.parseLong(chatId), UserState.EDIT_PROFILE_LOGIN);
                } else {
                    builder.append("Ошибка. Зарегистрируйтесь или войдите в аккаунт.");
                }
            } catch (SQLException e) {
                builder.append("Ошибка при получении профиля. Попробуйте позже.");
                LoggerUtil.logError(Long.parseLong(chatId), "Ошибка при получении профиля: " + e.getMessage());
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

            LoggerUtil.logInfo(userId, "Пользователь отправил команду: " + command);

            try {
                boolean isLoggedIn = databaseManager.isUserLoggedIn(userId);
                boolean isProfileExists = databaseManager.isProfileExists(userId);

                if (!isProfileExists || !isLoggedIn) {
                    if (commandsRequiringAuth.contains(command)) {
                        sendMsg(String.valueOf(userId), "Ошибка. Зарегистрируйтесь или войдите в аккаунт.");
                    } else {
                        handleCommand(userId, command, text);
                    }
                } else {
                    handleCommand(userId, command, text);
                }
            } catch (SQLException e) {
                sendMsg(String.valueOf(userId), "Ошибка при выполнении команды. Попробуйте позже.");
                LoggerUtil.logError(userId, "Ошибка при выполнении команды: " + e.getMessage());
            }
        } else if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            String callbackData = callbackQuery.getData();
            long userId = callbackQuery.getMessage().getChatId();

            if (callbackData.startsWith("edit_")) {
                Map<String, UserState> editStateMap = Map.of(
                        "edit_login", UserState.EDIT_PROFILE_LOGIN,
                        "edit_password", UserState.EDIT_PROFILE_PASSWORD,
                        "edit_nickname", UserState.EDIT_PROFILE_NICKNAME,
                        "edit_age", UserState.EDIT_PROFILE_AGE,
                        "edit_height", UserState.EDIT_PROFILE_HEIGHT,
                        "edit_weight", UserState.EDIT_PROFILE_WEIGHT
                );

                UserState state = editStateMap.get(callbackData);
                if (state != null) {
                    databaseManager.setUserState(userId, state);
                    String message = getMessageForState(state);
                    sendMsg(String.valueOf(userId), message);
                }
            } else if (databaseManager.getUserState(userId) == UserState.SELECT_COURSE) {
                try {
                    int courseId = Integer.parseInt(callbackData);
                    databaseManager.updateUserCourse(userId, courseId);
                    databaseManager.resetCompletedWorkouts(userId); // Сброс выполненных тренировок при смене программы
                    sendMsg(String.valueOf(userId), "Курс успешно выбран!");
                    databaseManager.removeUserState(userId); // Удаляем состояние пользователя
                    LoggerUtil.logInfo(userId, "Пользователь выбрал курс: " + courseId);
                } catch (NumberFormatException | SQLException e) {
                    sendMsg(String.valueOf(userId), "Ошибка при выборе курса. Попробуйте позже.");
                    LoggerUtil.logError(userId, "Ошибка при выборе курса: " + e.getMessage());
                }
            } else if (callbackData.startsWith("workout_")) {
                try {
                    int workoutId = Integer.parseInt(callbackData.split("_")[1]);
                    StringBuilder exercises = new StringBuilder();
                    exercises.append(databaseManager.getExercisesAsString(workoutId));
                    InlineKeyboardMarkup keyboardMarkup = InlineKeyboardManager.getCompleteWorkoutKeyboard(workoutId);
                    sendMsgWithInlineKeyboard(String.valueOf(userId), exercises.toString(), keyboardMarkup);
                    LoggerUtil.logInfo(userId, "Пользователь просмотрел упражнения в тренировке: " + workoutId);
                } catch (NumberFormatException | SQLException e) {
                    sendMsg(String.valueOf(userId), "Ошибка при получении упражнений. Попробуйте позже.");
                    LoggerUtil.logError(userId, "Ошибка при получении упражнений: " + e.getMessage());
                }
            } else if (callbackData.startsWith("complete_")) {
                try {
                    int workoutId = Integer.parseInt(callbackData.split("_")[1]);
                    databaseManager.markWorkoutAsCompleted(userId, workoutId);
                    sendMsg(String.valueOf(userId), "Тренировка отмечена как выполненная!");
                    LoggerUtil.logInfo(userId, "Пользователь отметил тренировку как выполненную: " + workoutId);
                } catch (NumberFormatException | SQLException e) {
                    sendMsg(String.valueOf(userId), "Ошибка при отметке тренировки. Попробуйте позже.");
                    LoggerUtil.logError(userId, "Ошибка при отметке тренировки: " + e.getMessage());
                }
            }
        }
    }

    private void handleCommand(long userId, String command, String text) {
        UserState userState = databaseManager.getUserState(userId);

        if (userState != null) {
            databaseManager.handleProfileCreationOrLogin(userId, text);
        } else {
            try {
                boolean isLoggedIn = databaseManager.isUserLoggedIn(userId);
                if (!isLoggedIn) {
                    if (commandsRequiringAuth.contains(command)) {
                        sendMsg(String.valueOf(userId), "Ошибка. Зарегистрируйтесь или войдите в аккаунт.");
                    } else {
                        BiConsumer<String, StringBuilder> action = commandMap.getOrDefault(command, (id, builder) -> {
                            builder.append("Неизвестная команда. Используйте /help для списка команд.");
                        });

                        StringBuilder responseBuilder = new StringBuilder();
                        action.accept(String.valueOf(userId), responseBuilder);
                        sendMsg(String.valueOf(userId), responseBuilder.toString());
                        LoggerUtil.logInfo(userId, "Пользователь выполнил команду: " + command);
                    }
                } else {
                    BiConsumer<String, StringBuilder> action = commandMap.getOrDefault(command, (id, builder) -> {
                        builder.append("Неизвестная команда. Используйте /help для списка команд.");
                    });

                    StringBuilder responseBuilder = new StringBuilder();
                    action.accept(String.valueOf(userId), responseBuilder);
                    sendMsg(String.valueOf(userId), responseBuilder.toString());
                    LoggerUtil.logInfo(userId, "Пользователь выполнил команду: " + command);
                }
            } catch (SQLException e) {
                sendMsg(String.valueOf(userId), "Ошибка при выполнении команды. Попробуйте позже.");
                LoggerUtil.logError(userId, "Ошибка при выполнении команды: " + e.getMessage());
            }
        }
    }


    // Метод для отправки сообщений
    public void sendMsg(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode(ParseMode.HTML); // Используем HTML для разметки
        try {
            execute(message);
        } catch (TelegramApiException e) {
            LoggerUtil.logError(Long.parseLong(chatId), "Ошибка при отправке сообщения: " + e.getMessage());
        }
    }

    public void sendMsgWithInlineKeyboard(String chatId, String text, InlineKeyboardMarkup keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode(ParseMode.HTML);
        message.setReplyMarkup(keyboardMarkup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            LoggerUtil.logError(Long.parseLong(chatId), "Ошибка при отправке сообщения с инлайн-клавиатурой: " + e.getMessage());
        }
    }

    // возвращает карту commandMap, которая содержит команды и связанные с ними действия.
    public Map<String, BiConsumer<String, StringBuilder>> getCommandMap() {
        return commandMap;
    }

    // Метод для установки DatabaseManager
    public void setDatabaseManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public String viewCourses(long userId) {
        StringBuilder responseBuilder = new StringBuilder();
        try {
            List<String> coursesList = databaseManager.getCoursesList();
            for (String course : coursesList) {
                responseBuilder.append(course).append("\n");
            }
        } catch (SQLException e) {
            responseBuilder.append("Ошибка при получении курсов. Попробуйте позже.");
            LoggerUtil.logError(userId, "Ошибка при получении курсов: " + e.getMessage());
        }
        return responseBuilder.toString();
    }

    public String selectCourse(long userId, int courseId) {
        try {
            databaseManager.selectCourse(userId, courseId);
            return "Курс выбран успешно";
        } catch (SQLException e) {
            LoggerUtil.logError(userId, "Ошибка при выборе курса: " + e.getMessage());
            return "Ошибка при выборе курса. Попробуйте позже.";
        }
    }

    public String viewWorkouts(long userId) {
        StringBuilder responseBuilder = new StringBuilder();
        try {
            // Retrieve the courseId associated with the userId
            int courseId = databaseManager.getCourseIdForUser(userId);
            if (courseId == 0) {
                responseBuilder.append("Пользователь не выбрал курс.");
            } else {
                List<String> workoutsList = databaseManager.getWorkoutsList(courseId);
                for (String workout : workoutsList) {
                    responseBuilder.append(workout).append("\n");
                }
            }
        } catch (SQLException e) {
            responseBuilder.append("Ошибка при получении тренировок. Попробуйте позже.");
            LoggerUtil.logError(userId, "Ошибка при получении тренировок: " + e.getMessage());
        }
        return responseBuilder.toString();
    }

    public String viewExercises(long userId, int workoutId) {
        StringBuilder responseBuilder = new StringBuilder();
        try {
            List<String> exercisesList = databaseManager.getExercisesList(workoutId);
            for (String exercise : exercisesList) {
                responseBuilder.append(exercise).append("\n");
            }
        } catch (SQLException e) {
            responseBuilder.append("Ошибка при получении упражнений. Попробуйте позже.");
            LoggerUtil.logError(userId, "Ошибка при получении упражнений: " + e.getMessage());
        }
        return responseBuilder.toString();
    }

    private static final Map<UserState, UserState> nextStateMap = new HashMap<UserState, UserState>() {{
        put(UserState.EDIT_PROFILE_LOGIN, UserState.EDIT_PROFILE_PASSWORD);
        put(UserState.EDIT_PROFILE_PASSWORD, UserState.EDIT_PROFILE_NICKNAME);
        put(UserState.EDIT_PROFILE_NICKNAME, UserState.EDIT_PROFILE_AGE);
        put(UserState.EDIT_PROFILE_AGE, UserState.EDIT_PROFILE_HEIGHT);
        put(UserState.EDIT_PROFILE_HEIGHT, UserState.EDIT_PROFILE_WEIGHT);
    }};

    private UserState getNextState(UserState currentState) {
        return nextStateMap.get(currentState);
    }

    private static final Map<UserState, String> messageMap = new HashMap<UserState, String>() {{
        put(UserState.EDIT_PROFILE_PASSWORD, "Введите новое значение для пароля:");
        put(UserState.EDIT_PROFILE_LOGIN, "Введите новое значение для логина:");
        put(UserState.EDIT_PROFILE_NICKNAME, "Введите новое значение для никнейма:");
        put(UserState.EDIT_PROFILE_AGE, "Введите новое значение для возраста:");
        put(UserState.EDIT_PROFILE_HEIGHT, "Введите новое значение для роста (в см):");
        put(UserState.EDIT_PROFILE_WEIGHT, "Введите новое значение для веса (в кг):");
    }};

    private String getMessageForState(UserState state) {
        return messageMap.get(state);
    }
}
