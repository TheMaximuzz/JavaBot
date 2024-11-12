package org.example.bot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;



public class DatabaseManager {

    private Connection connection;
    private String url;
    private String username;
    private String password;
    private Map<Long, UserProfile> userProfiles = new HashMap<>();
    private Map<Long, UserState> userStates = new HashMap<>();
    private Map<UserState, BiConsumer<Long, String>> stateHandlers = new HashMap<>();
    private TelegramBot bot;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


    public DatabaseManager(TelegramBot bot) {
        this.bot = bot;
        loadDbConfig();
        initializeStateHandlers();
    }

    private void loadDbConfig() {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream("db_config.properties")) {
            properties.load(input);
            this.url = properties.getProperty("db.url");
            this.username = properties.getProperty("db.username");
            this.password = properties.getProperty("db.password");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setDbConfig(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public void connect() throws SQLException {
        connection = DriverManager.getConnection(url, username, password);
    }

    public void disconnect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }


    public void createUserProfile(long userId, String login, String password, String nickname, Integer age, Integer height, Integer weight, long telegramChatId) throws SQLException {
        connect();
        String query = "INSERT INTO user_profiles (user_id, login, password, nickname, age, height, weight, telegram_chat_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, userId);
            statement.setString(2, login);
            statement.setString(3, passwordEncoder.encode(password));
            statement.setString(4, nickname);
            statement.setInt(5, age);
            statement.setInt(6, height);
            statement.setInt(7, weight);
            statement.setLong(8, telegramChatId);
            statement.executeUpdate();
        } finally {
            disconnect();
        }
    }

    public boolean isUserLoggedIn(long telegramChatId) throws SQLException {
        return isSessionActive(telegramChatId);
    }


    public boolean isProfileExists(long userId) throws SQLException {
        String query = "SELECT COUNT(*) FROM user_profiles WHERE user_id = ?";
        connect();
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, userId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        } finally {
            disconnect();
        }
        return false;
    }


    public long validateLogin(String login, String password) throws SQLException {
        String query = "SELECT user_id, password FROM user_profiles WHERE login = ?";
        connect();
        try (PreparedStatement statement = connection.prepareStatement(query)) {
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
            disconnect();
        }
        return -1;
    }



    public void handleLogin(long userId, String input) throws SQLException {
        UserProfile profile = getOrCreateUserProfile(userId);
        profile.setLogin(input);
        updateUserProfile(profile);
    }

    public void handlePassword(long userId, String input) throws SQLException {
        UserProfile profile = getOrCreateUserProfile(userId);
        profile.setPassword(passwordEncoder.encode(input));
        updateUserProfile(profile);
    }

    public void handleLoginPassword(long telegramChatId, String input) throws SQLException {
        UserProfile profile = getOrCreateUserProfile(telegramChatId);
        profile.setPassword(passwordEncoder.encode(input));

        long userId = validateLogin(profile.getLogin(), input);
        if (userId != -1) {
            createSession(userId, telegramChatId);
            bot.sendMsg(String.valueOf(telegramChatId), "Вы успешно вошли в аккаунт!");
            userStates.remove(telegramChatId);
        } else {
            bot.sendMsg(String.valueOf(telegramChatId), "Ошибка логина или пароля. Попробуйте снова.");
            userStates.put(telegramChatId, UserState.LOGIN_LOGIN);
        }
    }

    public void createSession(long userId, long telegramChatId) throws SQLException {
        connect();
        String query = "INSERT INTO user_sessions (user_id, telegram_chat_id) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, userId);
            statement.setLong(2, telegramChatId);
            statement.executeUpdate();
        } finally {
            disconnect();
        }
    }

    public boolean isSessionActive(long telegramChatId) throws SQLException {
        connect();
        String query = "SELECT COUNT(*) FROM user_sessions WHERE telegram_chat_id = ?";
        ResultSet resultSet = select(query, telegramChatId);
        boolean isActive = false;

        if (resultSet.next()) {
            isActive = resultSet.getInt(1) > 0;
        }

        disconnect();
        return isActive;
    }


    public void logoutAllSessions(long userId) throws SQLException {
        connect();
        String query = "DELETE FROM user_sessions WHERE user_id = ?";
        delete(query, userId);
        disconnect();
    }



    public void handleNickname(long userId, String input) throws SQLException {
        UserProfile profile = getOrCreateUserProfile(userId);
        profile.setNickname(input);
        updateUserProfile(profile);
    }

    public void handleAge(long userId, String input) throws SQLException {
        UserProfile profile = getOrCreateUserProfile(userId);
        profile.setAge(Integer.parseInt(input));
        updateUserProfile(profile);
    }

    public void handleHeight(long userId, String input) throws SQLException {
        UserProfile profile = getOrCreateUserProfile(userId);
        profile.setHeight(Integer.parseInt(input));
        updateUserProfile(profile);
    }

    public void handleWeight(long userId, String input) throws SQLException {
        UserProfile profile = getOrCreateUserProfile(userId);
        profile.setWeight(Integer.parseInt(input));
        updateUserProfile(profile);
    }

    private UserProfile getOrCreateUserProfile(long userId) {
        return userProfiles.computeIfAbsent(userId, k -> new UserProfile());
    }

    public void insert(String query, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            setParameters(statement, parameters);
            statement.executeUpdate();
        }
    }

    public ResultSet select(String query, Object... parameters) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(query);
        setParameters(statement, parameters);
        return statement.executeQuery();
    }

    public void update(String query, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            setParameters(statement, parameters);
            statement.executeUpdate();
        }
    }

    public void delete(String query, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            setParameters(statement, parameters);
            statement.executeUpdate();
        }
    }

    private void setParameters(PreparedStatement statement, Object... parameters) throws SQLException {
        for (int i = 0; i < parameters.length; i++) {
            statement.setObject(i + 1, parameters[i]);
        }
    }

    public ResultSet getUserProfileFromDB(long userId) throws SQLException {
        String query = "SELECT login, password, nickname, age, height, weight FROM user_profiles WHERE user_id = ?";
        return select(query, userId);
    }

    public String getUserProfileAsString(long telegramChatId) throws SQLException {
        connect();
        String query = "SELECT up.login, up.password, up.nickname, up.age, up.height, up.weight " +
                "FROM user_profiles up " +
                "JOIN user_sessions us ON up.user_id = us.user_id " +
                "WHERE us.telegram_chat_id = ?";
        ResultSet resultSet = select(query, telegramChatId);
        StringBuilder profile = new StringBuilder();

        if (resultSet.next()) {
            profile.append(String.format("<b>Ваш профиль:</b>\n<b>Логин:</b> <i>%s</i>\n<b>Никнейм:</b> <i>%s</i>\n<b>Возраст:</b> <i>%d</i>\n<b>Рост:</b> <i>%d</i>\n<b>Вес:</b> <i>%d</i>",
                    resultSet.getString("login"),
                    resultSet.getString("nickname"),
                    resultSet.getInt("age"),
                    resultSet.getInt("height"),
                    resultSet.getInt("weight")));
        } else {
            profile.append("Профиль не найден.");
        }

        disconnect();
        return profile.toString();
    }


    public void deleteUserProfile(long userId) throws SQLException {
        String query = "DELETE FROM user_profiles WHERE user_id = ?";
        delete(query, userId);
    }


    public String deleteUserProfileAsString(long userId) throws SQLException {
        String result;

        connect();
        if (isProfileExists(userId)) {
            disconnect();

            connect();
            deleteUserProfile(userId);
            result = "Ваш профиль успешно удален.";
        } else {
            result = "Профиль не найден.";
        }
        disconnect();

        return result;
    }


    public void updateTelegramChatId(long userId, long telegramChatId) throws SQLException {
        connect();
        String query = "UPDATE user_profiles SET telegram_chat_id = ? WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, telegramChatId);
            statement.setLong(2, userId);
            statement.executeUpdate();
        } finally {
            disconnect();
        }
    }



    public void updateUserLogin(long userId, String login) throws SQLException {
        connect();
        String query = "UPDATE user_profiles SET login = ? WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, login);
            statement.setLong(2, userId);
            statement.executeUpdate();
        } finally {
            disconnect();
        }
    }

    public void updateUserPassword(long userId, String password) throws SQLException {
        connect();
        String query = "UPDATE user_profiles SET password = ? WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, passwordEncoder.encode(password));
            statement.setLong(2, userId);
            statement.executeUpdate();
        } finally {
            disconnect();
        }
    }

    public void updateUserNickname(long userId, String nickname) throws SQLException {
        connect();
        String query = "UPDATE user_profiles SET nickname = ? WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, nickname);
            statement.setLong(2, userId);
            statement.executeUpdate();
        } finally {
            disconnect();
        }
    }

    public void updateUserAge(long userId, int age) throws SQLException {
        connect();
        String query = "UPDATE user_profiles SET age = ? WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, age);
            statement.setLong(2, userId);
            statement.executeUpdate();
        } finally {
            disconnect();
        }
    }

    public void updateUserHeight(long userId, int height) throws SQLException {
        connect();
        String query = "UPDATE user_profiles SET height = ? WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, height);
            statement.setLong(2, userId);
            statement.executeUpdate();
        } finally {
            disconnect();
        }
    }

    public void updateUserWeight(long userId, int weight) throws SQLException {
        connect();
        String query = "UPDATE user_profiles SET weight = ? WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, weight);
            statement.setLong(2, userId);
            statement.executeUpdate();
        } finally {
            disconnect();
        }
    }


    public void updateUserProfile(UserProfile profile) throws SQLException {
        connect();
        String query = "UPDATE user_profiles SET login = ?, password = ?, nickname = ?, age = ?, height = ?, weight = ? WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, profile.getLogin());
            statement.setString(2, profile.getPassword());
            statement.setString(3, profile.getNickname());
            statement.setInt(4, profile.getAge());
            statement.setInt(5, profile.getHeight());
            statement.setInt(6, profile.getWeight());
            statement.setLong(7, profile.getUserId());
            statement.executeUpdate();
        } finally {
            disconnect();
        }
    }

    public void handleProfileCreation(long userId, String input) {
        UserState state = userStates.get(userId);
        BiConsumer<Long, String> handler = stateHandlers.get(state);

        if (handler != null) {
            handler.accept(userId, input);
        } else {
            userStates.remove(userId);
        }

        // После завершения ввода всех данных профиля, сохраняем профиль в базу данных
        if (state == UserState.ENTER_WEIGHT) {
            try {
                UserProfile profile = getUserProfile(userId);
                createUserProfile(userId, profile.getLogin(), profile.getPassword(), profile.getNickname(), profile.getAge(), profile.getHeight(), profile.getWeight(), userId);
                createSession(userId, userId); // Создаем новую сессию
                userStates.remove(userId); // Удаляем состояние пользователя после завершения создания профиля
            } catch (SQLException e) {
                bot.sendMsg(String.valueOf(userId), "Ошибка при сохранении профиля. Попробуйте позже.");
                LoggerUtil.logError(userId, "Ошибка при сохранении профиля: " + e.getMessage());
            }
        }
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
                updateUserCourse(userId, courseId);
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
                exercises.append(getExercisesAsString(workoutId));
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
                updateUserLogin(userId, input);
                bot.sendMsg(String.valueOf(userId), "Логин обновлен.");
                userStates.remove(userId); // Удаляем состояние пользователя после изменения поля
            } catch (SQLException e) {
                bot.sendMsg(String.valueOf(userId), "Ошибка при обновлении логина. Попробуйте позже.");
                e.printStackTrace();
            }
        });

        stateHandlers.put(UserState.EDIT_PROFILE_PASSWORD, (userId, input) -> {
            try {
                updateUserPassword(userId, input);
                bot.sendMsg(String.valueOf(userId), "Пароль обновлен.");
                userStates.remove(userId); // Удаляем состояние пользователя после изменения поля
            } catch (SQLException e) {
                bot.sendMsg(String.valueOf(userId), "Ошибка при обновлении пароля. Попробуйте позже.");
                e.printStackTrace();
            }
        });

        stateHandlers.put(UserState.EDIT_PROFILE_NICKNAME, (userId, input) -> {
            try {
                updateUserNickname(userId, input);
                bot.sendMsg(String.valueOf(userId), "Никнейм обновлен.");
                userStates.remove(userId); // Удаляем состояние пользователя после изменения поля
            } catch (SQLException e) {
                bot.sendMsg(String.valueOf(userId), "Ошибка при обновлении никнейма. Попробуйте позже.");
                e.printStackTrace();
            }
        });

        stateHandlers.put(UserState.EDIT_PROFILE_AGE, (userId, input) -> {
            try {
                updateUserAge(userId, Integer.parseInt(input));
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
                updateUserHeight(userId, Integer.parseInt(input));
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
                updateUserWeight(userId, Integer.parseInt(input));
                bot.sendMsg(String.valueOf(userId), "Вес обновлен.");
                userStates.remove(userId); // Удаляем состояние пользователя после изменения поля
            } catch (NumberFormatException e) {
                bot.sendMsg(String.valueOf(userId), "Пожалуйста, введите корректный вес.");
            } catch (SQLException e) {
                bot.sendMsg(String.valueOf(userId), "Ошибка при обновлении веса. Попробуйте позже.");
                e.printStackTrace();
            }
        });


    }


    public void setUserState(long userId, UserState state) {
        userStates.put(userId, state);
    }

    public void removeUserState(long userId) {
        userStates.remove(userId);
    }

    public UserState getUserState(long userId) {
        return userStates.get(userId);
    }

    public void addUserProfile(long userId, UserProfile profile) {
        userProfiles.put(userId, profile);
    }

    public UserProfile getUserProfile(long userId) {
        return userProfiles.get(userId);
    }

    public String getCoursesAsString() throws SQLException {
        connect();
        String query = "SELECT course_id, course_name, course_description FROM courses";
        ResultSet resultSet = select(query);
        StringBuilder courses = new StringBuilder();

        while (resultSet.next()) {
            courses.append(String.format("<b>%s</b>\n<b>Описание:</b> <i>%s</i>\n\n",
                    resultSet.getString("course_name"),
                    resultSet.getString("course_description")));
        }

        disconnect();
        return courses.toString();
    }

    public List<Course> getCourses() throws SQLException {
        connect();
        String query = "SELECT course_id, course_name, course_description FROM courses";
        ResultSet resultSet = select(query);
        List<Course> courses = new ArrayList<>();

        while (resultSet.next()) {
            courses.add(new Course(
                    resultSet.getInt("course_id"),
                    resultSet.getString("course_name"),
                    resultSet.getString("course_description")
            ));
        }

        disconnect();
        return courses;
    }

    public void updateUserCourse(long userId, int courseId) throws SQLException {
        connect();
        String query = "UPDATE user_profiles SET course_id = ? WHERE user_id = ?";
        update(query, courseId, userId);
        disconnect();
    }

    public String getWorkoutsAsString(long userId) throws SQLException {
        connect();
        String query = "SELECT course_id FROM user_profiles WHERE user_id = ?";
        ResultSet resultSet = select(query, userId);
        int courseId = 0;

        if (resultSet.next()) {
            courseId = resultSet.getInt("course_id");
        }

        query = "SELECT workout_id, workout_name, workout_description FROM workouts WHERE course_id = ?";
        resultSet = select(query, courseId);
        StringBuilder workouts = new StringBuilder();

        while (resultSet.next()) {
            workouts.append(String.format("<b>%s</b>\n<b>Описание:</b> <i>%s</i>\n\n",
                    resultSet.getString("workout_name"),
                    resultSet.getString("workout_description")));
        }

        disconnect();
        return workouts.toString();
    }

    public List<Workout> getWorkouts(long userId) throws SQLException {
        connect();
        String query = "SELECT course_id FROM user_profiles WHERE user_id = ?";
        ResultSet resultSet = select(query, userId);
        int courseId = 0;

        if (resultSet.next()) {
            courseId = resultSet.getInt("course_id");
        }

        query = "SELECT workout_id, workout_name, workout_description FROM workouts WHERE course_id = ?";
        resultSet = select(query, courseId);
        List<Workout> workouts = new ArrayList<>();

        while (resultSet.next()) {
            workouts.add(new Workout(
                    resultSet.getInt("workout_id"),
                    resultSet.getString("workout_name"),
                    resultSet.getString("workout_description")
            ));
        }

        disconnect();
        return workouts;
    }

    public String getExercisesAsString(int workoutId) throws SQLException {
        connect();
        String query = "SELECT exercise_id, exercise_name, repetitions, sets FROM exercises WHERE workout_id = ?";
        ResultSet resultSet = select(query, workoutId);
        StringBuilder exercises = new StringBuilder();

        while (resultSet.next()) {
            exercises.append(String.format("<b>%s</b>\n<b>Повторения:</b> %d, <b>Подходы:</b> %d\n\n",
                    resultSet.getString("exercise_name"),
                    resultSet.getInt("repetitions"),
                    resultSet.getInt("sets")));
        }

        disconnect();
        return exercises.toString();
    }

    public void markWorkoutAsCompleted(long userId, int workoutId) throws SQLException {
        connect();
        String query = "INSERT INTO completed_workouts (user_id, workout_id, completed) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE completed = ?";
        update(query, userId, workoutId, true, true);
        disconnect();
    }

    public boolean isWorkoutCompleted(long userId, int workoutId) throws SQLException {
        connect();
        String query = "SELECT completed FROM completed_workouts WHERE user_id = ? AND workout_id = ?";
        ResultSet resultSet = select(query, userId, workoutId);
        boolean completed = false;
        if (resultSet.next()) {
            completed = resultSet.getBoolean("completed");
        }
        disconnect();
        return completed;
    }

    public List<Workout> getWorkoutsWithCompletionStatus(long userId) throws SQLException {
        connect();
        String query = "SELECT course_id FROM user_profiles WHERE user_id = ?";
        ResultSet resultSet = select(query, userId);
        int courseId = 0;

        if (resultSet.next()) {
            courseId = resultSet.getInt("course_id");
        }

        query = "SELECT workout_id, workout_name, workout_description FROM workouts WHERE course_id = ?";
        resultSet = select(query, courseId);
        List<Workout> workouts = new ArrayList<>();

        while (resultSet.next()) {
            Workout workout = new Workout(
                    resultSet.getInt("workout_id"),
                    resultSet.getString("workout_name"),
                    resultSet.getString("workout_description")
            );
            workout.setCompleted(isWorkoutCompleted(userId, workout.getId()));
            workouts.add(workout);
        }

        disconnect();
        return workouts;
    }

    public void resetCompletedWorkouts(long userId) throws SQLException {
        connect();
        String query = "DELETE FROM completed_workouts WHERE user_id = ?";
        delete(query, userId);
        disconnect();
    }

    // Метод для получения списка курсов
    public List<String> getCoursesList() throws SQLException {
        connect();
        String query = "SELECT course_name FROM courses";
        ResultSet resultSet = select(query);
        List<String> courses = new ArrayList<>();

        while (resultSet.next()) {
            courses.add(resultSet.getString("course_name"));
        }

        disconnect();
        return courses;
    }

    // Метод для выбора курса
    public void selectCourse(long userId, int courseId) throws SQLException {
        connect();
        String query = "UPDATE user_profiles SET course_id = ? WHERE user_id = ?";
        update(query, courseId, userId);
        disconnect();
    }

    // Метод для получения списка тренировок по courseId
    public List<String> getWorkoutsList(int courseId) throws SQLException {
        connect();
        String query = "SELECT workout_name FROM workouts WHERE course_id = ?";
        ResultSet resultSet = select(query, courseId);
        List<String> workouts = new ArrayList<>();

        while (resultSet.next()) {
            workouts.add(resultSet.getString("workout_name"));
        }

        disconnect();
        return workouts;
    }

    // Метод для получения списка упражнений по workoutId
    public List<String> getExercisesList(int workoutId) throws SQLException {
        connect();
        String query = "SELECT exercise_name FROM exercises WHERE workout_id = ?";
        ResultSet resultSet = select(query, workoutId);
        List<String> exercises = new ArrayList<>();

        while (resultSet.next()) {
            exercises.add(resultSet.getString("exercise_name"));
        }

        disconnect();
        return exercises;
    }

    public int getCourseIdForUser(long userId) throws SQLException {
        connect();
        String query = "SELECT course_id FROM user_profiles WHERE user_id = ?";
        ResultSet resultSet = select(query, userId);
        int courseId = 0;

        if (resultSet.next()) {
            courseId = resultSet.getInt("course_id");
        }

        disconnect();
        return courseId;
    }

    public void logoutUser(long telegramChatId) throws SQLException {
        connect();
        String query = "DELETE FROM user_sessions WHERE telegram_chat_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, telegramChatId);
            statement.executeUpdate();
        } finally {
            disconnect();
        }
    }


    public void handleProfileCreationOrLogin(long telegramChatId, String input) {
        UserState state = userStates.get(telegramChatId);
        BiConsumer<Long, String> handler = stateHandlers.get(state);

        if (handler != null) {
            handler.accept(telegramChatId, input);
        } else {
            userStates.remove(telegramChatId);
        }

        // После завершения ввода всех данных профиля, сохраняем профиль в базу данных
        if (state == UserState.ENTER_WEIGHT) {
            try {
                UserProfile profile = getUserProfile(telegramChatId);
                createUserProfile(telegramChatId, profile.getLogin(), profile.getPassword(), profile.getNickname(), profile.getAge(), profile.getHeight(), profile.getWeight(), telegramChatId);
                createSession(telegramChatId, telegramChatId); // Создаем новую сессию
                userStates.remove(telegramChatId); // Удаляем состояние пользователя после завершения создания профиля
            } catch (SQLException e) {
                bot.sendMsg(String.valueOf(telegramChatId), "Ошибка при сохранении профиля. Попробуйте позже.");
                LoggerUtil.logError(telegramChatId, "Ошибка при сохранении профиля: " + e.getMessage());
            }
        }
    }


}