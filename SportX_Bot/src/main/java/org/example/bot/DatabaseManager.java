package org.example.bot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;

public class DatabaseManager {

    private Connection connection;
    private String url;
    private String username;
    private String password;
    private Map<Long, UserProfile> userProfiles = new HashMap<>();
    private Map<Long, UserState> userStates = new HashMap<>();
    private Map<UserState, BiConsumer<Long, String>> stateHandlers = new HashMap<>();
    private TelegramBot bot;

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

    public void connect() throws SQLException {
        connection = DriverManager.getConnection(url, username, password);
    }

    public void disconnect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    public void createUserProfile(long userId, String nickname, int age, int height, int weight) throws SQLException {
        connect();
        String query = "INSERT INTO user_profiles (user_id, nickname, age, height, weight) VALUES (?, ?, ?, ?, ?)";
        insert(query, userId, nickname, age, height, weight);
        disconnect();
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

    public void handleNickname(long userId, String input) throws SQLException {
        if (isProfileExists(userId)) {
            throw new SQLException("Профиль уже существует");
        } else {
            UserProfile profile = getOrCreateUserProfile(userId);
            profile.nickname = input;
        }
    }

    public void handleAge(long userId, String input) throws NumberFormatException {
        UserProfile profile = getOrCreateUserProfile(userId);
        profile.age = Integer.parseInt(input);
    }

    public void handleHeight(long userId, String input) throws NumberFormatException {
        UserProfile profile = getOrCreateUserProfile(userId);
        profile.height = Integer.parseInt(input);
    }

    public void handleWeight(long userId, String input) throws NumberFormatException, SQLException {
        UserProfile profile = getOrCreateUserProfile(userId);
        profile.weight = Integer.parseInt(input);
        createUserProfile(userId, profile.nickname, profile.age, profile.height, profile.weight);
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
        String query = "SELECT nickname, age, height, weight FROM user_profiles WHERE user_id = ?";
        return select(query, userId);
    }

    public String getUserProfileAsString(long userId) throws SQLException {
        connect();
        ResultSet resultSet = getUserProfileFromDB(userId);
        StringBuilder profile = new StringBuilder();

        if (resultSet.next()) {
            profile.append(String.format("Ваш профиль:\nНикнейм: %s\nВозраст: %d\nРост: %d\nВес: %d",
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

    public void handleProfileCreation(long userId, String input) {
        UserState state = userStates.get(userId);
        BiConsumer<Long, String> handler = stateHandlers.get(state);

        if (handler != null) {
            handler.accept(userId, input);
        } else {
            userStates.remove(userId);
        }
    }

    private void initializeStateHandlers() {
        stateHandlers.put(UserState.ENTER_NICKNAME, (userId, input) -> {
            try {
                handleNickname(userId, input);
                bot.sendMsg(String.valueOf(userId), "Введите ваш возраст:");
                userStates.put(userId, UserState.ENTER_AGE);
            } catch (SQLException e) {
                bot.sendMsg(String.valueOf(userId), "У вас уже есть профиль. Регистрация отменена.");
                userStates.remove(userId);
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
            }
        });

        stateHandlers.put(UserState.ENTER_HEIGHT, (userId, input) -> {
            try {
                handleHeight(userId, input);
                bot.sendMsg(String.valueOf(userId), "Введите ваш вес (в кг):");
                userStates.put(userId, UserState.ENTER_WEIGHT);
            } catch (NumberFormatException e) {
                bot.sendMsg(String.valueOf(userId), "Пожалуйста, введите корректный рост.");
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
                bot.sendMsg(String.valueOf(userId), "Ошибка при сохранении профиля. Попробуйте позже.");
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
}
