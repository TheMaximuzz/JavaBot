package org.example.bot;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class InlineKeyboardManager {

    public static InlineKeyboardMarkup getCourseSelectionKeyboard(List<Course> courses) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardRows = new ArrayList<>();

        for (Course course : courses) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(course.getName());
            button.setCallbackData(String.valueOf(course.getId()));
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboardRows.add(row);
        }

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    public static InlineKeyboardMarkup getWorkoutSelectionKeyboard(List<Workout> workouts) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardRows = new ArrayList<>();

        for (Workout workout : workouts) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(workout.getName() + (workout.isCompleted() ? Icon.CHECK.get() + " " : ""));
            button.setCallbackData("workout_" + workout.getId());
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboardRows.add(row);
        }

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    public static InlineKeyboardMarkup getCompleteWorkoutKeyboard(int workoutId) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardRows = new ArrayList<>();

        InlineKeyboardButton completeButton = new InlineKeyboardButton();
        completeButton.setText(Icon.CHECK.get());
        completeButton.setCallbackData("complete_" + workoutId);
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(completeButton);
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }


    public static InlineKeyboardMarkup getEditProfileKeyboard() {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("Логин", "edit_login"));
        row1.add(createButton("Пароль", "edit_password"));

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createButton("Никнейм", "edit_nickname"));
        row2.add(createButton("Возраст", "edit_age"));

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createButton("Рост", "edit_height"));
        row3.add(createButton("Вес", "edit_weight"));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    private static InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }
}