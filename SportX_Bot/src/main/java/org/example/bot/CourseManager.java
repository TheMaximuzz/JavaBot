package org.example.bot;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CourseManager {

    private final DatabaseConnection dbConnection;

    public CourseManager(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    public String getCoursesAsString() throws SQLException {
        dbConnection.connect();
        String query = "SELECT course_id, course_name, course_description FROM courses";
        ResultSet resultSet = dbConnection.select(query);
        StringBuilder courses = new StringBuilder();

        while (resultSet.next()) {
            courses.append(String.format("<b>%s</b>\n<b>Описание:</b> <i>%s</i>\n\n",
                    resultSet.getString("course_name"),
                    resultSet.getString("course_description")));
        }

        dbConnection.disconnect();
        return courses.toString();
    }

    public List<Course> getCourses() throws SQLException {
        dbConnection.connect();
        String query = "SELECT course_id, course_name, course_description FROM courses";
        ResultSet resultSet = dbConnection.select(query);
        List<Course> courses = new ArrayList<>();

        while (resultSet.next()) {
            courses.add(new Course(
                    resultSet.getInt("course_id"),
                    resultSet.getString("course_name"),
                    resultSet.getString("course_description")
            ));
        }

        dbConnection.disconnect();
        return courses;
    }

    public String getWorkoutsAsString(long userId) throws SQLException {
        dbConnection.connect();
        String query = "SELECT course_id FROM user_profiles WHERE user_id = ?";
        ResultSet resultSet = dbConnection.select(query, userId);
        int courseId = 0;

        if (resultSet.next()) {
            courseId = resultSet.getInt("course_id");
        }

        query = "SELECT workout_id, workout_name, workout_description FROM workouts WHERE course_id = ?";
        resultSet = dbConnection.select(query, courseId);
        StringBuilder workouts = new StringBuilder();

        while (resultSet.next()) {
            workouts.append(String.format("<b>%s</b>\n<b>Описание:</b> <i>%s</i>\n\n",
                    resultSet.getString("workout_name"),
                    resultSet.getString("workout_description")));
        }

        dbConnection.disconnect();
        return workouts.toString();
    }

    public List<Workout> getWorkouts(long userId) throws SQLException {
        dbConnection.connect();
        String query = "SELECT course_id FROM user_profiles WHERE user_id = ?";
        ResultSet resultSet = dbConnection.select(query, userId);
        int courseId = 0;

        if (resultSet.next()) {
            courseId = resultSet.getInt("course_id");
        }

        query = "SELECT workout_id, workout_name, workout_description FROM workouts WHERE course_id = ?";
        resultSet = dbConnection.select(query, courseId);
        List<Workout> workouts = new ArrayList<>();

        while (resultSet.next()) {
            workouts.add(new Workout(
                    resultSet.getInt("workout_id"),
                    resultSet.getString("workout_name"),
                    resultSet.getString("workout_description")
            ));
        }

        dbConnection.disconnect();
        return workouts;
    }

    public String getExercisesAsString(int workoutId) throws SQLException {
        dbConnection.connect();
        String query = "SELECT exercise_id, exercise_name, repetitions, sets FROM exercises WHERE workout_id = ?";
        ResultSet resultSet = dbConnection.select(query, workoutId);
        StringBuilder exercises = new StringBuilder();

        while (resultSet.next()) {
            exercises.append(String.format("<b>%s</b>\n<b>Повторения:</b> %d, <b>Подходы:</b> %d\n\n",
                    resultSet.getString("exercise_name"),
                    resultSet.getInt("repetitions"),
                    resultSet.getInt("sets")));
        }

        dbConnection.disconnect();
        return exercises.toString();
    }

    public void markWorkoutAsCompleted(long userId, int workoutId) throws SQLException {
        dbConnection.connect();
        String query = "INSERT INTO completed_workouts (user_id, workout_id, completed) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE completed = ?";
        dbConnection.update(query, userId, workoutId, true, true);
        dbConnection.disconnect();
    }

    public boolean isWorkoutCompleted(long userId, int workoutId) throws SQLException {
        dbConnection.connect();
        String query = "SELECT completed FROM completed_workouts WHERE user_id = ? AND workout_id = ?";
        ResultSet resultSet = dbConnection.select(query, userId, workoutId);
        boolean completed = false;
        if (resultSet.next()) {
            completed = resultSet.getBoolean("completed");
        }
        dbConnection.disconnect();
        return completed;
    }

    public List<Workout> getWorkoutsWithCompletionStatus(long userId) throws SQLException {
        dbConnection.connect();
        String query = "SELECT course_id FROM user_profiles WHERE user_id = ?";
        ResultSet resultSet = dbConnection.select(query, userId);
        int courseId = 0;

        if (resultSet.next()) {
            courseId = resultSet.getInt("course_id");
        }

        query = "SELECT workout_id, workout_name, workout_description FROM workouts WHERE course_id = ?";
        resultSet = dbConnection.select(query, courseId);
        List<Workout> workouts = new ArrayList<>();

        while (resultSet.next()) {
            Workout workout = new Workout(
                    resultSet.getInt("workout_id"),
                    resultSet.getString("workout_name"),
                    resultSet.getString("workout_description")
            );
            workout.setCompleted(isWorkoutCompleted(userId, workout.getId()));
            workouts.add(workout);
        }

        dbConnection.disconnect();
        return workouts;
    }

    public void resetCompletedWorkouts(long userId) throws SQLException {
        dbConnection.connect();
        String query = "DELETE FROM completed_workouts WHERE user_id = ?";
        dbConnection.delete(query, userId);
        dbConnection.disconnect();
    }

    public List<String> getCoursesList() throws SQLException {
        dbConnection.connect();
        String query = "SELECT course_name FROM courses";
        ResultSet resultSet = dbConnection.select(query);
        List<String> courses = new ArrayList<>();

        while (resultSet.next()) {
            courses.add(resultSet.getString("course_name"));
        }

        dbConnection.disconnect();
        return courses;
    }

    public void selectCourse(long userId, int courseId) throws SQLException {
        dbConnection.connect();
        String query = "UPDATE user_profiles SET course_id = ? WHERE user_id = ?";
        dbConnection.update(query, courseId, userId);
        dbConnection.disconnect();
    }

    public List<String> getWorkoutsList(int courseId) throws SQLException {
        dbConnection.connect();
        String query = "SELECT workout_name FROM workouts WHERE course_id = ?";
        ResultSet resultSet = dbConnection.select(query, courseId);
        List<String> workouts = new ArrayList<>();

        while (resultSet.next()) {
            workouts.add(resultSet.getString("workout_name"));
        }

        dbConnection.disconnect();
        return workouts;
    }

    public List<String> getExercisesList(int workoutId) throws SQLException {
        dbConnection.connect();
        String query = "SELECT exercise_name FROM exercises WHERE workout_id = ?";
        ResultSet resultSet = dbConnection.select(query, workoutId);
        List<String> exercises = new ArrayList<>();

        while (resultSet.next()) {
            exercises.add(resultSet.getString("exercise_name"));
        }

        dbConnection.disconnect();
        return exercises;
    }

    public int getCourseIdForUser(long userId) throws SQLException {
        dbConnection.connect();
        String query = "SELECT course_id FROM user_profiles WHERE user_id = ?";
        ResultSet resultSet = dbConnection.select(query, userId);
        int courseId = 0;

        if (resultSet.next()) {
            courseId = resultSet.getInt("course_id");
        }

        dbConnection.disconnect();
        return courseId;
    }

    public void updateUserCourse(long userId, int courseId) throws SQLException {
        dbConnection.connect();
        String query = "UPDATE user_profiles SET course_id = ? WHERE user_id = ?";
        try (PreparedStatement statement = dbConnection.connection.prepareStatement(query)) {
            statement.setInt(1, courseId);
            statement.setLong(2, userId);
            statement.executeUpdate();
        } finally {
            dbConnection.disconnect();
        }
    }
}
