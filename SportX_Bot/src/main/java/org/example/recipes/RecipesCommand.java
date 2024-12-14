package org.example.recipes;

import okhttp3.OkHttpClient;
import org.example.bot.DatabaseManager;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RecipesCommand {

    private TranslateService translateService;
    private final SpoonacularAPI spoonacularAPI;
    private final DatabaseManager databaseManager;
    private final RecipeParser recipeParser; // Новый класс
    private boolean waitingForIngredients = false;
    private static final Logger logger = Logger.getLogger(RecipesCommand.class.getName());

    public RecipesCommand(SpoonacularAPI api, DatabaseManager dbManager) {
        this.spoonacularAPI = api;
        this.databaseManager = dbManager;
        this.recipeParser = new RecipeParser();
        OkHttpClient okHttpClient = new OkHttpClient();
        this.translateService = new TranslateService(okHttpClient);
    }

    public SendMessage getContent(long userId, String userInput) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(userId));

        if (userInput == null || userInput.isEmpty()) {
            message.setText("Не удалось обработать ваш запрос. Попробуйте снова.");
            return message;
        }

        try {
            return findRecipes(userInput, userId);
        } catch (Exception e) {
            message.setText("Произошла ошибка при запросе рецептов. Попробуйте позже.");
            logger.log(Level.SEVERE, "Ошибка при запросе рецептов: ", e);
        }

        return message;
    }

    public SendMessage askForIngredients(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Пожалуйста, укажите ингредиенты, которые у вас есть, через запятую. Например:\nпомидоры, сыр, курица");
        return message;
    }

    private SendMessage findRecipes(String ingredientsInput, long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        try {
            String response = spoonacularAPI.searchRecipes(translateService.translateToEnglish(ingredientsInput), "", "");

            List<String> recipeTitles = recipeParser.parseRecipeTitles(response);
            List<Integer> recipeIds = recipeParser.parseRecipeIds(response);

            if (recipeTitles.isEmpty()) {
                message.setText("К сожалению, рецепты не найдены. Попробуйте уточнить ингредиенты.");
            } else {
                message.setText("Рецепты, которые могут вам подойти:\n" + translateService.translateFromEnglish(String.join("\n ", recipeTitles)));
                message.setReplyMarkup(createRecipeSelectionKeyboard(recipeTitles, recipeIds));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при запросе рецептов", e);
            message.setText("Произошла ошибка. Проверьте данные и попробуйте снова.");
        }
        return message;
    }

    public InlineKeyboardMarkup createRecipeSelectionKeyboard(List<String> recipeTitles, List<Integer> recipeIds) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (int i = 0; i < recipeTitles.size(); i++) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(translateService.translateFromEnglish(recipeTitles.get(i)));
            button.setCallbackData("recipe_" + recipeIds.get(i));
            rows.add(List.of(button));
        }
        inlineKeyboardMarkup.setKeyboard(rows);
        return inlineKeyboardMarkup;
    }

    public String getRecipeDetails(int recipeId) {
        try {
            String response = spoonacularAPI.getRecipeInformation(recipeId);
            return recipeParser.getRecipeDetails(response);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при получении деталей рецепта", e);
            return "Произошла ошибка при получении деталей рецепта. Попробуйте позже.";
        }
    }
}