package org.example.bot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DatabaseManager {

    private Connection connection;
    private String url;
    private String username;
    private String password;

    public DatabaseManager() {
        loadDbConfig();
    }

    // Метод для загрузки данных подключения из файла db_config.properties
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

    // Метод для подключения к базе данных
    public void connect() throws SQLException {
        connection = DriverManager.getConnection(url, username, password);
    }

    // Метод для закрытия соединения
    public void disconnect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
    // Метод для создания профиля пользователя с проверкой существования
    public void createUserProfile(long userId, String nickname, int age, int height, int weight) throws SQLException {

        String query = "INSERT INTO user_profiles (user_id, nickname, age, height, weight) VALUES (?, ?, ?, ?, ?)"; //SQL запрос
        insert(query, userId, nickname, age, height, weight);
    }




    // Проверка, существует ли профиль с определенным userID
    public boolean isProfileExists(long userId) throws SQLException {
        String query = "SELECT COUNT(*) FROM user_profiles WHERE user_id = ?";
        connect(); // Подключаемся к базе данных перед выполнением запроса
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, userId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0; // Если количество больше 0, профиль существует
            }
        } finally {
            disconnect(); // Закрываем соединение в любом случае (успех/ошибка)
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

    // Метод для получения данных профиля пользователя
    public ResultSet getUserProfile(long userId) throws SQLException {
        String query = "SELECT nickname, age, height, weight FROM user_profiles WHERE user_id = ?";
        return select(query, userId);
    }

    // Метод для получения данных профиля пользователя как строки
    public String getUserProfileAsString(long userId) throws SQLException {
        connect();
        ResultSet resultSet = getUserProfile(userId);
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

    // Метод для удаления профиля пользователя
    public void deleteUserProfile(long userId) throws SQLException {
        String query = "DELETE FROM user_profiles WHERE user_id = ?";
        delete(query, userId);
    }
    // Метод для удаления профиля пользователя как результат в виде строки
    public String deleteUserProfileAsString(long userId) throws SQLException {
        String result;

        connect();  // Подключаемся для проверки профиля
        if (isProfileExists(userId)) {
            disconnect();  // Закрываем после проверки

            connect();  // Подключаемся снова для удаления
            deleteUserProfile(userId);
            result = "Ваш профиль успешно удален.";
        } else {
            result = "Профиль не найден.";
        }
        disconnect();  // Закрываем после удаления или ошибки

        return result;
    }







}
