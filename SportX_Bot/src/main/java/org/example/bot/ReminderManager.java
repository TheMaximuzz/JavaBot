package org.example.bot;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReminderManager {

    private final DatabaseConnection dbConnection;

    public ReminderManager(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    public void addReminder(long userId, String description, String dateTime) throws SQLException {
        String query = "INSERT INTO reminders (user_id, description, date_time) VALUES (?, ?, ?)";
        try {
            dbConnection.connect(); // Убедитесь, что соединение установлено
            try (PreparedStatement statement = dbConnection.connection.prepareStatement(query)) {
                statement.setLong(1, userId);
                statement.setString(2, description);
                statement.setString(3, dateTime);
                statement.executeUpdate();
            }
        } finally {
            dbConnection.disconnect();
        }
    }

    public void deleteReminder(int reminderId) throws SQLException {
        String query = "DELETE FROM reminders WHERE id = ?";
        try {
            dbConnection.connect();
            try (PreparedStatement statement = dbConnection.connection.prepareStatement(query)) {
                statement.setInt(1, reminderId);
                statement.executeUpdate();
            }
        } finally {
            dbConnection.disconnect();
        }
    }

    public List<String> getReminders(long userId) throws SQLException {
        List<String> reminders = new ArrayList<>();
        String query = "SELECT id, description, date_time FROM reminders WHERE user_id = ?";
        try {
            dbConnection.connect();
            try (PreparedStatement statement = dbConnection.connection.prepareStatement(query)) {
                statement.setLong(1, userId);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String description = resultSet.getString("description");
                    String dateTime = resultSet.getString("date_time");

                    SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
                    try {
                        Date parsedDate = dbDateFormat.parse(dateTime);
                        String formattedDateTime = displayDateFormat.format(parsedDate);
                        reminders.add("Номер: " + id + ", Описание: " + description + ", Дата: " + formattedDateTime);
                    } catch (ParseException e) {
                        LoggerUtil.logError(userId, "Ошибка при парсинге даты: " + e.getMessage());
                        reminders.add("Номер: " + id + ", Описание: " + description + ", Дата: " + dateTime + " (Ошибка при парсинге даты)");
                    }
                }
            }
        } finally {
            dbConnection.disconnect();
        }
        return reminders;
    }
}
