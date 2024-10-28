package org.example.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TestOfInteractionWithCourses {

    @Mock
    private DatabaseManager databaseManager;

    @InjectMocks
    private TelegramBot telegramBot;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testViewCourses() throws SQLException {
        long userId = 123456L;

        // Настраиваем поведение мока для получения списка курсов
        when(databaseManager.getCoursesList()).thenReturn(List.of("Course 1", "Course 2", "Course 3"));

        String coursesList = telegramBot.viewCourses(userId);
        assertNotNull(coursesList, "Список курсов не должен быть null");
        assertTrue(coursesList.contains("Course 1"), "Список должен содержать Course 1");
        assertTrue(coursesList.contains("Course 2"), "Список должен содержать Course 2");

        // Проверка, что метод был вызван один раз
        verify(databaseManager, times(1)).getCoursesList();
    }

    @Test
    public void testSelectCourse() throws SQLException {
        long userId = 123456L;
        int courseId = 1;

        // Настраиваем поведение мока для выбора курса
        doNothing().when(databaseManager).selectCourse(userId, courseId);

        String selectCourseMessage = telegramBot.selectCourse(userId, courseId);
        assertEquals("Курс выбран успешно", selectCourseMessage, "Сообщение должно быть о успешном выборе курса");

        // Проверка, что метод был вызван один раз
        verify(databaseManager, times(1)).selectCourse(userId, courseId);
    }

    @Test
    public void testViewWorkouts() throws SQLException {
        long userId = 123456L;
        int courseId = 1;

        // Настраиваем поведение мока для получения courseId для пользователя
        when(databaseManager.getCourseIdForUser(userId)).thenReturn(courseId);

        // Настраиваем поведение мока для получения списка тренировок
        when(databaseManager.getWorkoutsList(courseId)).thenReturn(List.of("Workout 1", "Workout 2"));

        String workoutsList = telegramBot.viewWorkouts(userId);
        assertNotNull(workoutsList, "Список тренировок не должен быть null");
        assertTrue(workoutsList.contains("Workout 1"), "Список должен содержать Workout 1");
        assertTrue(workoutsList.contains("Workout 2"), "Список должен содержать Workout 2");

        // Проверка, что методы были вызваны один раз
        verify(databaseManager, times(1)).getCourseIdForUser(userId);
        verify(databaseManager, times(1)).getWorkoutsList(courseId);
    }

    @Test
    public void testViewExercises() throws SQLException {
        long userId = 123456L;
        int workoutId = 1;

        // Настраиваем поведение мока для получения списка упражнений
        when(databaseManager.getExercisesList(workoutId)).thenReturn(List.of("Exercise 1", "Exercise 2"));

        String exercisesList = telegramBot.viewExercises(userId, workoutId);
        assertNotNull(exercisesList, "Список упражнений не должен быть null");
        assertTrue(exercisesList.contains("Exercise 1"), "Список должен содержать Exercise 1");
        assertTrue(exercisesList.contains("Exercise 2"), "Список должен содержать Exercise 2");

        // Проверка, что метод был вызван один раз
        verify(databaseManager, times(1)).getExercisesList(workoutId);
    }
}
