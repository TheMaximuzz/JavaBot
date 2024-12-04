package org.example.bot;

import org.example.recipes.RecipesCommand;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class StateHandler {

    private final TelegramBot bot;
    private final UserProfileManager userProfileManager;
    private final SessionManager sessionManager;
    private final CourseManager courseManager;
    private final DatabaseConnection dbConnection;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Map<Long, UserState> userStates = new HashMap<>();
    private final Map<UserState, BiConsumer<Long, String>> stateHandlers = new HashMap<>();
    private final RecipesCommand recipesCommand;

    public StateHandler(TelegramBot bot, UserProfileManager userProfileManager, SessionManager sessionManager, CourseManager courseManager, DatabaseConnection dbConnection, RecipesCommand recipesCommand) {
        this.bot = bot;
        this.userProfileManager = userProfileManager;
        this.sessionManager = sessionManager;
        this.courseManager = courseManager;
        this.dbConnection = dbConnection;
        this.recipesCommand = recipesCommand; // Добавьте это
        initializeStateHandlers();
    }


    private void initializeStateHandlers() {
        stateHandlers.put(UserState.LOGIN_LOGIN, (userId, input) -> {
            try {
                handleLogin(userId, input);
                bot.sendMsg(String.valueOf(userId), "Введите ваш пароль:");
                userStates.put(userId, UserState.LOGIN_PASSWORD);
            } catch (SQLException e) {
                bot.sendMsg(String.valueOf(userId), "Ошибка при входе. Попробуйте позже.");
                userStates.remove(userId);
                e.printStackTrace();
            }
        });

        stateHandlers.put(UserState.LOGIN_PASSWORD, (userId, input) -> {
            try {
                handleLoginPassword(userId, input);
            } catch (SQLException e) {
                bot.sendMsg(String.valueOf(userId), "Ошибка при входе. Попробуйте позже.");
                userStates.remove(userId);
                e.printStackTrace();
            }
        });

        stateHandlers.put(UserState.CREATE_PROFILE_LOGIN, (userId, input) -> {
            try {
                handleLogin(userId, input);
                bot.sendMsg(String.valueOf(userId), "Введите ваш пароль:");
                userStates.put(userId, UserState.CREATE_PROFILE_PASSWORD);
            } catch (SQLException e) {
                bot.sendMsg(String.valueOf(userId), "Ошибка при создании профиля. Попробуйте позже.");
                userStates.remove(userId);
                e.printStackTrace();
            }
        });

        stateHandlers.put(UserState.CREATE_PROFILE_PASSWORD, (userId, input) -> {
            try {
                handlePassword(userId, input);
                bot.sendMsg(String.valueOf(userId), "Введите ваш никнейм:");
                userStates.put(userId, UserState.ENTER_NICKNAME);
            } catch (SQLException e) {
                bot.sendMsg(String.valueOf(userId), "Ошибка при создании профиля. Попробуйте позже.");
                e.printStackTrace();
            }
        });

        stateHandlers.put(UserState.ENTER_NICKNAME, (userId, input) -> {
            try {
                handleNickname(userId, input);
                bot.sendMsg(String.valueOf(userId), "Введите ваш возраст:");
                userStates.put(userId, UserState.ENTER_AGE);
            } catch (SQLException e) {
                bot.sendMsg(String.valueOf(userId), "Ошибка при сохранении никнейма. Попробуйте позже.");
                e.printStackTrace();
            }
        });

        stateHandlers.put(UserState.ENTER_AGE, (userId, input) -> {
            try {
                handleAge(userId, input);
                bot.sendMsg(String.valueOf(userId), "Введите ваш рост (в см):");
                userStates.put(userId, UserState.ENTER_HEIGHT);
            } catch (NumberFormatException e) {
                bot.sendMsg(String.valueOf(userId), "Пожалуйста, введите корректный возраст.");
            } catch (SQLException e) {
                bot.sendMsg(String.valueOf(userId), "Ошибка при сохранении возраста. Попробуйте позже.");
                e.printStackTrace();
            }
        });

        stateHandlers.put(UserState.ENTER_HEIGHT, (userId, input) -> {
            try {
                handleHeight(userId, input);
                bot.sendMsg(String.valueOf(userId), "Введите ваш вес (в кг):");
                userStates.put(userId, UserState.ENTER_WEIGHT);
            } catch (NumberFormatException e) {
                bot.sendMsg(String.valueOf(userId), "Пожалуйста, введите корректный рост.");
            } catch (SQLException e) {
                bot.sendMsg(String.valueOf(userId), "Ошибка при сохранении роста. Попробуйте позже.");
                e.printStackTrace();
            }
        });

        stateHandlers.put(UserState.ENTER_WEIGHT, (userId, input) -> {
            try {
                handleWeight(userId, input);
                bot.sendMsg(String.valueOf(userId), "Ваш профиль успешно создан!");
                userStates.remove(userId);
            } catch (NumberFormatException e) {
                bot.sendMsg(String.valueOf(userId), "Пожалуйста, введите корректный вес.");
            } catch (SQLException e) {
                bot.sendMsg(String.valueOf(userId), "Ошибка при сохранении веса. Попробуйте позже.");
                e.printStackTrace();
            }
        });

        stateHandlers.put(UserState.SELECT_COURSE, (userId, input) -> {
            try {
                int courseId = Integer.parseInt(input);
                courseManager.updateUserCourse(userId, courseId);
                bot.sendMsg(String.valueOf(userId), "Программа тренировок успешно выбрана!");
                userStates.remove(userId);
            } catch (NumberFormatException e) {
                bot.sendMsg(String.valueOf(userId), "Пожалуйста, введите корректный ID программы тренировок.");
            } catch (SQLException e) {
                bot.sendMsg(String.valueOf(userId), "Ошибка при выборе программы тренировок. Попробуйте позже.");
                e.printStackTrace();
            }
        });

        stateHandlers.put(UserState.VIEW_EXERCISES, (userId, input) -> {
            try {
                int workoutId = Integer.parseInt(input);
                StringBuilder exercises = new StringBuilder();
                exercises.append(courseManager.getExercisesAsString(workoutId));
                bot.sendMsg(String.valueOf(userId), exercises.toString());
                userStates.remove(userId);
            } catch (NumberFormatException e) {
                bot.sendMsg(String.valueOf(userId), "Пожалуйста, введите корректный ID тренировки.");
            } catch (SQLException e) {
                bot.sendMsg(String.valueOf(userId), "Ошибка при получении упражнений. Попробуйте позже.");
                e.printStackTrace();
            }
        });

        stateHandlers.put(UserState.EDIT_PROFILE_LOGIN, (userId, input) -> {
            try {
                userProfileManager.updateUserLogin(userId, input);
                bot.sendMsg(String.valueOf(userId), "Логин обновлен.");
                userStates.remove(userId); // Удаляем состояние пользователя после изменения поля
            } catch (SQLException e) {
                bot.sendMsg(String.valueOf(userId), "Ошибка при обновлении логина. Попробуйте позже.");
                e.printStackTrace();
            }
        });

        stateHandlers.put(UserState.EDIT_PROFILE_PASSWORD, (userId, input) -> {
            try {
                userProfileManager.updateUserPassword(userId, passwordEncoder.encode(input));
                bot.sendMsg(String.valueOf(userId), "Пароль обновлен.");
                userStates.remove(userId); // Удаляем состояние пользователя после изменения поля
            } catch (SQLException e) {
                bot.sendMsg(String.valueOf(userId), "Ошибка при обновлении пароля. Попробуйте позже.");
                e.printStackTrace();
            }
        });

        stateHandlers.put(UserState.EDIT_PROFILE_NICKNAME, (userId, input) -> {
            try {
                userProfileManager.updateUserNickname(userId, input);
                bot.sendMsg(String.valueOf(userId), "Никнейм обновлен.");
                userStates.remove(userId); // Удаляем состояние пользователя после изменения поля
            } catch (SQLException e) {
                bot.sendMsg(String.valueOf(userId), "Ошибка при обновлении никнейма. Попробуйте позже.");
                e.printStackTrace();
            }
        });

        stateHandlers.put(UserState.EDIT_PROFILE_AGE, (userId, input) -> {
            try {
                userProfileManager.updateUserAge(userId, Integer.parseInt(input));
                bot.sendMsg(String.valueOf(userId), "Возраст обновлен.");
                userStates.remove(userId); // Удаляем состояние пользователя после изменения поля
            } catch (NumberFormatException e) {
                bot.sendMsg(String.valueOf(userId), "Пожалуйста, введите корректный возраст.");
            } catch (SQLException e) {
                bot.sendMsg(String.valueOf(userId), "Ошибка при обновлении возраста. Попробуйте позже.");
                e.printStackTrace();
            }
        });

        stateHandlers.put(UserState.EDIT_PROFILE_HEIGHT, (userId, input) -> {
            try {
                userProfileManager.updateUserHeight(userId, Integer.parseInt(input));
                bot.sendMsg(String.valueOf(userId), "Рост обновлен.");
                userStates.remove(userId); // Удаляем состояние пользователя после изменения поля
            } catch (NumberFormatException e) {
                bot.sendMsg(String.valueOf(userId), "Пожалуйста, введите корректный рост.");
            } catch (SQLException e) {
                bot.sendMsg(String.valueOf(userId), "Ошибка при обновлении роста. Попробуйте позже.");
                e.printStackTrace();
            }
        });

        stateHandlers.put(UserState.EDIT_PROFILE_WEIGHT, (userId, input) -> {
            try {
                userProfileManager.updateUserWeight(userId, Integer.parseInt(input));
                bot.sendMsg(String.valueOf(userId), "Вес обновлен.");
                userStates.remove(userId); // Удаляем состояние пользователя после изменения поля
            } catch (NumberFormatException e) {
                bot.sendMsg(String.valueOf(userId), "Пожалуйста, введите корректный вес.");
            } catch (SQLException e) {
                bot.sendMsg(String.valueOf(userId), "Ошибка при обновлении веса. Попробуйте позже.");
                e.printStackTrace();
            }
        });

        stateHandlers.put(UserState.ENTER_INGREDIENTS, (userId, input) -> {
            try {
                // Передача входного текста напрямую в RecipesCommand
                SendMessage message = recipesCommand.getContent(userId, input);
                bot.sendMsgWithInlineKeyboard(String.valueOf(userId), message.getText(), (InlineKeyboardMarkup) message.getReplyMarkup());
            } catch (Exception e) {
                bot.sendMsg(String.valueOf(userId), "Ошибка при обработке ингредиентов. Попробуйте позже.");
                LoggerUtil.logError(userId, "Ошибка в RecipesCommand: " + e.getMessage());
            }
        });
    }

    public void handleProfileCreationOrLogin(long telegramChatId, String input) {
        UserState state = userStates.get(telegramChatId);
        BiConsumer<Long, String> handler = stateHandlers.get(state);

        if (handler != null) {
            handler.accept(telegramChatId, input);
        } else {
            userStates.remove(telegramChatId);
        }

        if (state == UserState.ENTER_WEIGHT) {
            try {
                UserProfile profile = userProfileManager.getUserProfile(telegramChatId);
                userProfileManager.createUserProfile(telegramChatId, profile.getLogin(), profile.getPassword(), profile.getNickname(), profile.getAge(), profile.getHeight(), profile.getWeight(), telegramChatId);
                sessionManager.createSession(telegramChatId, telegramChatId);
                userStates.remove(telegramChatId);
            } catch (SQLException e) {
                bot.sendMsg(String.valueOf(telegramChatId), "Ошибка при сохранении профиля. Попробуйте позже.");
                LoggerUtil.logError(telegramChatId, "Ошибка при сохранении профиля: " + e.getMessage());
            }
        }
    }

    public void setUserState(long userId, UserState state) {
        LoggerUtil.logInfo(userId, "StateHandler.setUserState вызван с состоянием: " + state);
        userStates.put(userId, state);
    }

    public void removeUserState(long userId) {
        userStates.remove(userId);
    }

    public UserState getUserState(long userId) {
        return userStates.get(userId);
    }

    private void handleLogin(long userId, String input) throws SQLException {
        UserProfile profile = userProfileManager.getOrCreateUserProfile(userId);
        profile.setLogin(input);
        userProfileManager.updateUserProfile(profile);
    }

    private void handlePassword(long userId, String input) throws SQLException {
        UserProfile profile = userProfileManager.getOrCreateUserProfile(userId);
        profile.setPassword(passwordEncoder.encode(input));
        userProfileManager.updateUserProfile(profile);
    }

    private void handleLoginPassword(long telegramChatId, String input) throws SQLException {
        UserProfile profile = userProfileManager.getOrCreateUserProfile(telegramChatId);
        profile.setPassword(passwordEncoder.encode(input));

        long userId = validateLogin(profile.getLogin(), input);
        if (userId != -1) {
            sessionManager.createSession(userId, telegramChatId);
            bot.sendMsg(String.valueOf(telegramChatId), "Вы успешно вошли в аккаунт!");
            userStates.remove(telegramChatId);
        } else {
            bot.sendMsg(String.valueOf(telegramChatId), "Ошибка логина или пароля. Попробуйте снова.");
            userStates.put(telegramChatId, UserState.LOGIN_LOGIN);
        }
    }

    private void handleNickname(long userId, String input) throws SQLException {
        UserProfile profile = userProfileManager.getOrCreateUserProfile(userId);
        profile.setNickname(input);
        userProfileManager.updateUserProfile(profile);
    }

    private void handleAge(long userId, String input) throws SQLException {
        UserProfile profile = userProfileManager.getOrCreateUserProfile(userId);
        profile.setAge(Integer.parseInt(input));
        userProfileManager.updateUserProfile(profile);
    }

    private void handleHeight(long userId, String input) throws SQLException {
        UserProfile profile = userProfileManager.getOrCreateUserProfile(userId);
        profile.setHeight(Integer.parseInt(input));
        userProfileManager.updateUserProfile(profile);
    }

    private void handleWeight(long userId, String input) throws SQLException {
        UserProfile profile = userProfileManager.getOrCreateUserProfile(userId);
        profile.setWeight(Integer.parseInt(input));
        userProfileManager.updateUserProfile(profile);
    }

    private long validateLogin(String login, String password) throws SQLException {
        String query = "SELECT user_id, password FROM user_profiles WHERE login = ?";
        dbConnection.connect();
        try (PreparedStatement statement = dbConnection.connection.prepareStatement(query)) {
            statement.setString(1, login);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                long userId = resultSet.getLong("user_id");
                String storedPassword = resultSet.getString("password");
                if (passwordEncoder.matches(password, storedPassword)) { // Проверяем пароль
                    return userId;
                }
            }
        } finally {
            dbConnection.disconnect();
        }
        return -1;
    }
}
