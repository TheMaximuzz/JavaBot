package org.example.bot;

public class UserProfile {
    private long userId;
    private String login;
    private String password;
    private String nickname;
    private int age;
    private int height;
    private int weight;
    private boolean isLoggedIn;

    // Конструкторы, геттеры и сеттеры

    public UserProfile() {
        // Пустой конструктор
    }

    public UserProfile(long userId, String login, String password, String nickname, int age, int height, int weight, boolean isLoggedIn) {
        this.userId = userId;
        this.login = login;
        this.password = password;
        this.nickname = nickname;
        this.age = age;
        this.height = height;
        this.weight = weight;
        this.isLoggedIn = isLoggedIn;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
    }
}