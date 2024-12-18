package org.example.bot;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

public class SessionManager {

    private final DatabaseConnection dbConnection;
    private static final Logger logger = Logger.getLogger(SessionManager.class.getName());

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
        //logger.info("Сессия для " + telegramChatId + " активна: " + isActive);
        return isActive;
    }

    public void createSession(long userId, long telegramChatId) throws SQLException {
        dbConnection.connect();
        String query = "INSERT INTO user_sessions (user_id, telegram_chat_id) VALUES (?, ?)";
        try (PreparedStatement statement = dbConnection.connection.prepareStatement(query)) {
            statement.setLong(1, userId);
            statement.setLong(2, telegramChatId);
            statement.executeUpdate();
            logger.info("Сессия создана для пользователя: " + userId + " с chatId: " + telegramChatId);
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
