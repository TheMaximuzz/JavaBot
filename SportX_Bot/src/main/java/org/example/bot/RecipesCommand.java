package org.example.bot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

// Обработка команд
public class RecipesCommand {

    private final SpoonacularAPI spoonacularAPI;
    private final DatabaseManager databaseManager;
    private boolean waitingForIngredients = false;
    private static final Logger logger = Logger.getLogger(RecipesCommand.class.getName());

    public RecipesCommand(SpoonacularAPI api, DatabaseManager dbManager) {
        this.spoonacularAPI = api;
        this.databaseManager = dbManager;
    }

    public SendMessage getContent(long userId, String userInput) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(userId));

        if (userInput == null || userInput.isEmpty()) {
            message.setText("Не удалось обработать ваш запрос. Попробуйте снова.");
            return message;
        }
        try {
            // Состояние пользователя: ввод ингредиентов
            return findRecipes(userInput, userId);
        } catch (Exception e) {
            message.setText("Произошла ошибка при запросе рецептов. Попробуйте позже.");
            LoggerUtil.logError(userId, "Ошибка при запросе рецептов: " + e.getMessage());
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
            String response = spoonacularAPI.searchRecipes(ingredientsInput, "", "");

            List<String> recipeTitles = parseRecipeTitles(response);
            List<Integer> recipeIds = parseRecipeIds(response);

            if (recipeTitles.isEmpty()) {
                message.setText("К сожалению, рецепты не найдены. Попробуйте уточнить ингредиенты.");
            } else {
                message.setText("Рецепты, которые могут вам подойти:\n" + String.join("\n", recipeTitles));
                message.setReplyMarkup(createRecipeSelectionKeyboard(recipeTitles, recipeIds));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при запросе рецептов", e);
            message.setText("Произошла ошибка. Проверьте данные и попробуйте снова.");
        }
        return message;
    }

    // Здесь извлекаем ID рецептов из ответа API
    public List<Integer> parseRecipeIds(String jsonResponse) {
        List<Integer> recipeIds = new ArrayList<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonResponse);
            JsonNode resultsNode = rootNode.path("results");
            for (JsonNode result : resultsNode) {
                recipeIds.add(result.path("id").asInt());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при парсинге JSON для ID рецептов", e);
        }
        return recipeIds;
    }

    public List<String> parseRecipeTitles(String jsonResponse) {
        List<String> recipeTitles = new ArrayList<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonResponse);
            JsonNode resultsNode = rootNode.path("results");
            for (JsonNode result : resultsNode) {
                recipeTitles.add(result.path("title").asText());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при парсинге JSON для названий рецептов", e);
        }
        return recipeTitles;
    }

    public InlineKeyboardMarkup createRecipeSelectionKeyboard(List<String> recipeTitles, List<Integer> recipeIds) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (int i = 0; i < recipeTitles.size(); i++) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(recipeTitles.get(i)); // название рецепта
            button.setCallbackData("recipe_" + recipeIds.get(i)); // ID рецепта
            rows.add(List.of(button)); // кнопочка
        }
        inlineKeyboardMarkup.setKeyboard(rows);
        return inlineKeyboardMarkup;
    }

    public String getRecipeDetails(int recipeId) {
        try {
            // Запрашиваем детали рецепта у SpoonacularAPI
            String response = spoonacularAPI.getRecipeInformation(recipeId);

            // Парсим детали рецепта
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);

            String title = rootNode.path("title").asText();
            String instructions = rootNode.path("instructions").asText();

            // Заменяем html-теги на символы и строки
            if (instructions != null) {
                instructions = instructions.replaceAll("<ol>", "\n").replaceAll("</ol>", "")
                        .replaceAll("<li>", "- ").replaceAll("</li>", "\n")
                        .replaceAll("<b>", "").replaceAll("</b>", "")
                        .replaceAll("<i>", "").replaceAll("</i>", "");
            }

            if (instructions == null || instructions.isEmpty()) {
                instructions = "Инструкции отсутствуют.";
            }

            return "Вы выбрали рецепт: " + title + "\n\nРуководство к действию:\n" + instructions;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при получении деталей рецепта", e);
            return "Произошла ошибка при получении деталей рецепта. Попробуйте позже.";
        }
    }
}
