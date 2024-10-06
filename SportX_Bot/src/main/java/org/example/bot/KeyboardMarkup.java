package org.example.bot;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeyboardMarkup {

    private static final Map<String, String> buttonTextToCommandMap = new HashMap<>();

    static {
        buttonTextToCommandMap.put("Создать профиль", "/createprofile");
        buttonTextToCommandMap.put("Посмотреть профиль", "/viewprofile");
        buttonTextToCommandMap.put("Удалить профиль", "/deleteprofile");
        buttonTextToCommandMap.put("Информация о боте", "/info");
    }

    public static ReplyKeyboardMarkup getMainMenuKeyboard() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Создать профиль");
        row1.add("Посмотреть профиль");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Удалить профиль");
        row2.add("Информация о боте");

        keyboard.add(row1);
        keyboard.add(row2);

        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }

    public static String mapButtonTextToCommand(String text) {
        return buttonTextToCommandMap.getOrDefault(text, text);
    }
}
