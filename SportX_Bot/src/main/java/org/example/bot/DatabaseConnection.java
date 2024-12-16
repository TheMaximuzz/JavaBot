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
import java.io.FileNotFoundException;

public class DatabaseConnection {

    protected Connection connection;
    protected String url;
    protected String username;
    protected String password;

    public DatabaseConnection() {
        loadDbConfig();
    }

    public void loadDbConfig() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("db_config.properties")) {
            if (input == null) {
                throw new FileNotFoundException("db_config.properties not found");
            }
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
}
