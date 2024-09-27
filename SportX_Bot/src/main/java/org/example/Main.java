package org.example;

import org.example.bot.TelegramBot; // указываем на использование класса класса TelegramBot
import org.telegram.telegrambots.meta.TelegramBotsApi; // Импорт класса из библиотеки TelegramBots API
import org.telegram.telegrambots.meta.exceptions.TelegramApiException; // Импорт класса исключений, которые выводятся при ошибках
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession; // Импорт класса для реализации сессии бота

//Останутся ли до конца проекта эти импорты? Основная часть импортов будет из TelegramBots?

public class Main {
    public static void main(String[] args) throws TelegramApiException { //throws указывает на то, что метод может выбросить несколько ошибок
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class); // создаем объект, который отвечает за управление ботом
        telegramBotsApi.registerBot(new TelegramBot()); // регистрируем бота в системе TelegramBots API.
        //Если мы подключим другие API, надо будет прописывать подобное, что зарегать бота в API?
    }
}
//В pom.xml мы ничего не трогаем, кроме dependencies?
