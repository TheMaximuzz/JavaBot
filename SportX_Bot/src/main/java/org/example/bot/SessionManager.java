package org.example.bot;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SessionManager {

    private final DatabaseConnection dbConnection;

    public SessionManager(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    public boolean isSessionActive(long telegramChatId) throws SQLException {
        dbConnection.connect();
        String query = "SELECT COUNT(*) FROM user_sessions WHERE telegram_chat_id = ?";
        ResultSet resultSet = dbConnection.select(query, telegramChatId);
        boolean isActive = false;

        if (resultSet.next()) {
            isActive = resultSet.getInt(1) > 0;
        }

        dbConnection.disconnect();
        return isActive;
    }

    public void createSession(long userId, long telegramChatId) throws SQLException {
        dbConnection.connect();
        String query = "INSERT INTO user_sessions (user_id, telegram_chat_id) VALUES (?, ?)";
        try (PreparedStatement statement = dbConnection.connection.prepareStatement(query)) {
            statement.setLong(1, userId);
            statement.setLong(2, telegramChatId);
            statement.executeUpdate();
        } finally {
            dbConnection.disconnect();
        }
    }

    public void logoutUser(long telegramChatId) throws SQLException {
        dbConnection.connect();
        String query = "DELETE FROM user_sessions WHERE telegram_chat_id = ?";
        try (PreparedStatement statement = dbConnection.connection.prepareStatement(query)) {
            statement.setLong(1, telegramChatId);
            statement.executeUpdate();
        } finally {
            dbConnection.disconnect();
        }
    }
}
