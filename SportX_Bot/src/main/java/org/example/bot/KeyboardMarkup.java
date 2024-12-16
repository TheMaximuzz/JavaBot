package org.example.bot;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeyboardMarkup {

    private static final Map<String, String> buttonTextToCommandMap = new HashMap<>();

    static {
        buttonTextToCommandMap.put("/start", "/start");
        buttonTextToCommandMap.put("/authors", "/authors");
        buttonTextToCommandMap.put("/help", "/help");
        buttonTextToCommandMap.put("/info", "/info");
        buttonTextToCommandMap.put("/createprofile", "/createprofile");
        buttonTextToCommandMap.put("/viewprofile", "/viewprofile");
        buttonTextToCommandMap.put("/deleteprofile", "/deleteprofile");
        buttonTextToCommandMap.put("/viewcourses", "/viewcourses");
        buttonTextToCommandMap.put("/selectcourse", "/selectcourse");
        buttonTextToCommandMap.put("/viewworkouts", "/viewworkouts");
        buttonTextToCommandMap.put("/viewexercises", "/viewexercises");
        buttonTextToCommandMap.put("/login", "/login");
        buttonTextToCommandMap.put("/logout", "/logout");
        buttonTextToCommandMap.put("/editprofile", "/editprofile");
        buttonTextToCommandMap.put("/recipes", "/recipes");
        buttonTextToCommandMap.put("/addreminder", "/addreminder");
        buttonTextToCommandMap.put("/deletereminder", "/deletereminder");
        buttonTextToCommandMap.put("/viewreminders", "/viewreminders");
    }

    public static ReplyKeyboardMarkup getMainMenuKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("/start"));
        row1.add(new KeyboardButton("/authors"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("/help"));
        row2.add(new KeyboardButton("/info"));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("/createprofile"));
        row3.add(new KeyboardButton("/login"));

        KeyboardRow row4 = new KeyboardRow();
        row4.add(new KeyboardButton("/viewprofile"));
        row4.add(new KeyboardButton("/deleteprofile"));

        KeyboardRow row5 = new KeyboardRow();
        row5.add(new KeyboardButton("/viewcourses"));
        row5.add(new KeyboardButton("/selectcourse"));

        KeyboardRow row6 = new KeyboardRow();
        row6.add(new KeyboardButton("/viewworkouts"));
        row6.add(new KeyboardButton("/viewexercises"));

        KeyboardRow row7 = new KeyboardRow();
        row7.add(new KeyboardButton("/editprofile"));
        row7.add(new KeyboardButton("/logout"));

        keyboardRows.add(row1);
        keyboardRows.add(row2);
        keyboardRows.add(row3);
        keyboardRows.add(row4);
        keyboardRows.add(row5);
        keyboardRows.add(row6);
        keyboardRows.add(row7);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    public static ReplyKeyboardMarkup getCourseSelectionKeyboard(List<Course> courses) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        for (Course course : courses) {
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(course.getName()));
            keyboardRows.add(row);
        }

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    public static String mapButtonTextToCommand(String text) {
        return buttonTextToCommandMap.getOrDefault(text, "/unknown");
    }
}