package org.example;

import org.example.bot.Icon;
import org.example.bot.LoggerUtil;
import org.example.bot.TelegramBot;
import org.example.bot.DatabaseManager;
import org.example.bot.KeyboardMarkup;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main{
    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TelegramBot());
    }
}