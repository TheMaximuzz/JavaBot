package org.example.bot;

import java.sql.SQLException;
import java.util.List;

public class DatabaseManager {

    private final DatabaseConnection dbConnection;
    private final UserProfileManager userProfileManager;
    private final CourseManager courseManager;
    private final SessionManager sessionManager;
    private final StateHandler stateHandler;
    private final TelegramBot bot;

    public DatabaseManager(TelegramBot bot) {
        this.bot = bot;
        this.dbConnection = new DatabaseConnection();
        this.userProfileManager = new UserProfileManager(this.dbConnection);
        this.courseManager = new CourseManager(this.dbConnection);
        this.sessionManager = new SessionManager(this.dbConnection);
        this.stateHandler = new StateHandler(this.bot, this.userProfileManager, this.sessionManager, this.courseManager, this.dbConnection);
    }

    public void connect() throws SQLException {
        dbConnection.connect();
    }

    public void disconnect() throws SQLException {
        dbConnection.disconnect();
    }

    public boolean isUserLoggedIn(long telegramChatId) throws SQLException {
        return sessionManager.isSessionActive(telegramChatId);
    }

    public boolean isProfileExists(long userId) throws SQLException {
        return userProfileManager.isProfileExists(userId);
    }

    public void handleProfileCreationOrLogin(long telegramChatId, String input) {
        stateHandler.handleProfileCreationOrLogin(telegramChatId, input);
    }

    public void setUserState(long userId, UserState state) {
        stateHandler.setUserState(userId, state);
    }

    public void removeUserState(long userId) {
        stateHandler.removeUserState(userId);
    }

    public UserState getUserState(long userId) {
        return stateHandler.getUserState(userId);
    }

    public void addUserProfile(long userId, UserProfile profile) {
        userProfileManager.addUserProfile(userId, profile);
    }

    public UserProfile getUserProfile(long userId) {
        return userProfileManager.getUserProfile(userId);
    }

    public String getUserProfileAsString(long telegramChatId) throws SQLException {
        return userProfileManager.getUserProfileAsString(telegramChatId);
    }

    public String deleteUserProfileAsString(long userId) throws SQLException {
        return userProfileManager.deleteUserProfileAsString(userId);
    }

    public String getCoursesAsString() throws SQLException {
        return courseManager.getCoursesAsString();
    }

    public List<Course> getCourses() throws SQLException {
        return courseManager.getCourses();
    }

    public String getWorkoutsAsString(long userId) throws SQLException {
        return courseManager.getWorkoutsAsString(userId);
    }

    public List<Workout> getWorkouts(long userId) throws SQLException {
        return courseManager.getWorkouts(userId);
    }

    public String getExercisesAsString(int workoutId) throws SQLException {
        return courseManager.getExercisesAsString(workoutId);
    }

    public void markWorkoutAsCompleted(long userId, int workoutId) throws SQLException {
        courseManager.markWorkoutAsCompleted(userId, workoutId);
    }

    public boolean isWorkoutCompleted(long userId, int workoutId) throws SQLException {
        return courseManager.isWorkoutCompleted(userId, workoutId);
    }

    public List<Workout> getWorkoutsWithCompletionStatus(long userId) throws SQLException {
        return courseManager.getWorkoutsWithCompletionStatus(userId);
    }

    public void resetCompletedWorkouts(long userId) throws SQLException {
        courseManager.resetCompletedWorkouts(userId);
    }

    public List<String> getCoursesList() throws SQLException {
        return courseManager.getCoursesList();
    }

    public void selectCourse(long userId, int courseId) throws SQLException {
        courseManager.selectCourse(userId, courseId);
    }

    public List<String> getWorkoutsList(int courseId) throws SQLException {
        return courseManager.getWorkoutsList(courseId);
    }

    public List<String> getExercisesList(int workoutId) throws SQLException {
        return courseManager.getExercisesList(workoutId);
    }

    public int getCourseIdForUser(long userId) throws SQLException {
        return courseManager.getCourseIdForUser(userId);
    }

    public void updateUserCourse(long userId, int courseId) throws SQLException {
        courseManager.updateUserCourse(userId, courseId);
    }

    public void logoutUser(long telegramChatId) throws SQLException {
        sessionManager.logoutUser(telegramChatId);
    }
}