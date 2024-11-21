package org.example.bot;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class UserProfileManager {

    private final DatabaseConnection dbConnection;
    private final Map<Long, UserProfile> userProfiles = new HashMap<>();

    public UserProfileManager(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    public boolean isProfileExists(long userId) throws SQLException {
        String query = "SELECT COUNT(*) FROM user_profiles WHERE user_id = ?";
        dbConnection.connect();
        try (PreparedStatement statement = dbConnection.connection.prepareStatement(query)) {
            statement.setLong(1, userId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        } finally {
            dbConnection.disconnect();
        }
        return false;
    }

    public void addUserProfile(long userId, UserProfile profile) {
        userProfiles.put(userId, profile);
    }

    public UserProfile getUserProfile(long userId) {
        return userProfiles.get(userId);
    }

    public void updateUserProfile(UserProfile profile) throws SQLException {
        dbConnection.connect();
        String query = "UPDATE user_profiles SET login = ?, password = ?, nickname = ?, age = ?, height = ?, weight = ? WHERE user_id = ?";
        try (PreparedStatement statement = dbConnection.connection.prepareStatement(query)) {
            statement.setString(1, profile.getLogin());
            statement.setString(2, profile.getPassword());
            statement.setString(3, profile.getNickname());
            statement.setInt(4, profile.getAge());
            statement.setInt(5, profile.getHeight());
            statement.setInt(6, profile.getWeight());
            statement.setLong(7, profile.getUserId());
            statement.executeUpdate();
        } finally {
            dbConnection.disconnect();
        }
    }

    public void createUserProfile(long userId, String login, String password, String nickname, Integer age, Integer height, Integer weight, long telegramChatId) throws SQLException {
        dbConnection.connect();
        String query = "INSERT INTO user_profiles (user_id, login, password, nickname, age, height, weight, telegram_chat_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = dbConnection.connection.prepareStatement(query)) {
            statement.setLong(1, userId);
            statement.setString(2, login);
            statement.setString(3, password);
            statement.setString(4, nickname);
            statement.setInt(5, age);
            statement.setInt(6, height);
            statement.setInt(7, weight);
            statement.setLong(8, telegramChatId);
            statement.executeUpdate();
        } finally {
            dbConnection.disconnect();
        }
    }

    public void deleteUserProfile(long userId) throws SQLException {
        String query = "DELETE FROM user_profiles WHERE user_id = ?";
        dbConnection.delete(query, userId);
    }

    public String deleteUserProfileAsString(long userId) throws SQLException {
        String result;

        dbConnection.connect();
        if (isProfileExists(userId)) {
            dbConnection.disconnect();

            dbConnection.connect();
            deleteUserProfile(userId);
            result = "Ваш профиль успешно удален.";
        } else {
            result = "Профиль не найден.";
        }
        dbConnection.disconnect();

        return result;
    }

    public String getUserProfileAsString(long telegramChatId) throws SQLException {
        dbConnection.connect();
        String query = "SELECT up.login, up.password, up.nickname, up.age, up.height, up.weight " +
                "FROM user_profiles up " +
                "JOIN user_sessions us ON up.user_id = us.user_id " +
                "WHERE us.telegram_chat_id = ?";
        ResultSet resultSet = dbConnection.select(query, telegramChatId);
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

        dbConnection.disconnect();
        return profile.toString();
    }

    public UserProfile getOrCreateUserProfile(long userId) {
        return userProfiles.computeIfAbsent(userId, k -> new UserProfile());
    }

    public void updateUserCourse(long userId, int courseId) throws SQLException {
        dbConnection.connect();
        String query = "UPDATE user_profiles SET course_id = ? WHERE user_id = ?";
        try (PreparedStatement statement = dbConnection.connection.prepareStatement(query)) {
            statement.setInt(1, courseId);
            statement.setLong(2, userId);
            statement.executeUpdate();
        } finally {
            dbConnection.disconnect();
        }
    }

    public void updateUserLogin(long userId, String login) throws SQLException {
        dbConnection.connect();
        String query = "UPDATE user_profiles SET login = ? WHERE user_id = ?";
        try (PreparedStatement statement = dbConnection.connection.prepareStatement(query)) {
            statement.setString(1, login);
            statement.setLong(2, userId);
            statement.executeUpdate();
        } finally {
            dbConnection.disconnect();
        }
    }

    public void updateUserPassword(long userId, String password) throws SQLException {
        dbConnection.connect();
        String query = "UPDATE user_profiles SET password = ? WHERE user_id = ?";
        try (PreparedStatement statement = dbConnection.connection.prepareStatement(query)) {
            statement.setString(1, password);
            statement.setLong(2, userId);
            statement.executeUpdate();
        } finally {
            dbConnection.disconnect();
        }
    }

    public void updateUserNickname(long userId, String nickname) throws SQLException {
        dbConnection.connect();
        String query = "UPDATE user_profiles SET nickname = ? WHERE user_id = ?";
        try (PreparedStatement statement = dbConnection.connection.prepareStatement(query)) {
            statement.setString(1, nickname);
            statement.setLong(2, userId);
            statement.executeUpdate();
        } finally {
            dbConnection.disconnect();
        }
    }

    public void updateUserAge(long userId, int age) throws SQLException {
        dbConnection.connect();
        String query = "UPDATE user_profiles SET age = ? WHERE user_id = ?";
        try (PreparedStatement statement = dbConnection.connection.prepareStatement(query)) {
            statement.setInt(1, age);
            statement.setLong(2, userId);
            statement.executeUpdate();
        } finally {
            dbConnection.disconnect();
        }
    }

    public void updateUserHeight(long userId, int height) throws SQLException {
        dbConnection.connect();
        String query = "UPDATE user_profiles SET height = ? WHERE user_id = ?";
        try (PreparedStatement statement = dbConnection.connection.prepareStatement(query)) {
            statement.setInt(1, height);
            statement.setLong(2, userId);
            statement.executeUpdate();
        } finally {
            dbConnection.disconnect();
        }
    }

    public void updateUserWeight(long userId, int weight) throws SQLException {
        dbConnection.connect();
        String query = "UPDATE user_profiles SET weight = ? WHERE user_id = ?";
        try (PreparedStatement statement = dbConnection.connection.prepareStatement(query)) {
            statement.setInt(1, weight);
            statement.setLong(2, userId);
            statement.executeUpdate();
        } finally {
            dbConnection.disconnect();
        }
    }
}
