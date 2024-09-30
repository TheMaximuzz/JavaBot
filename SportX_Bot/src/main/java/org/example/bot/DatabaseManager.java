package org.example.bot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseManager {
    private Connection connection;

    public void connect() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/mydbjavabot";
        String username = "root1";
        String password = "root";
        connection = DriverManager.getConnection(url, username, password);
    }

    //закрываем соединение с бд, если оно установлено
    public void disconnect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    // Метод для создания профиля пользователя
    public void createUserProfile(long userId, String nickname, int age, int height, int weight) throws SQLException {
        String query = "INSERT INTO user_profiles (user_id, nickname, age, height, weight) VALUES (?, ?, ?, ?, ?)"; //SQL запрос
        insert(query, userId, nickname, age, height, weight);
    }

    // Проверка, существует ли профиль с определенным userID
    public boolean isProfileExists(long userId) throws SQLException {
        String query = "SELECT COUNT(*) FROM user_profiles WHERE user_id = ?";
        ResultSet resultSet = select(query, userId);
        if (resultSet.next()) { //Если > 0, то профиль существует
            return resultSet.getInt(1) > 0;
        }
        return false;
    }

    // Метод для выполнения запроса вставления данных
    public void insert(String query, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            setParameters(statement, parameters);
            statement.executeUpdate();
        }
    }

    // Метод для выполнения запроса на выборку данных. Возвращает результат в виде ResultSet
    public ResultSet select(String query, Object... parameters) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(query);
        setParameters(statement, parameters);
        return statement.executeQuery();
    }

    // Метод для выполнения запроса обновления данных
    public void update(String query, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            setParameters(statement, parameters);
            statement.executeUpdate();
        }
    }

    // Метод для выполнения DELETE запроса
    public void delete(String query, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            setParameters(statement, parameters);
            statement.executeUpdate();
        }
    }

    // Метод для установки параметров на места плейсхолдеров (пропусков в команде) в SQL запросах
    private void setParameters(PreparedStatement statement, Object... parameters) throws SQLException {
        for (int i = 0; i < parameters.length; i++) {
            statement.setObject(i + 1, parameters[i]);
        }
    }
}


