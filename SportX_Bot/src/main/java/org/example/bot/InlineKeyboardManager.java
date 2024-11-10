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


    public static InlineKeyboardMarkup getSkipButtonKeyboard() {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardRows = new ArrayList<>();

        InlineKeyboardButton skipButton = new InlineKeyboardButton();
        skipButton.setText(Icon.SKIP.get());
        skipButton.setCallbackData("skip");
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(skipButton);
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }
}