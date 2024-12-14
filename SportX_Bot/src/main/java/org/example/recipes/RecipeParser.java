package org.example.recipes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RecipeParser {

    private static final Logger logger = Logger.getLogger(RecipeParser.class.getName());
    OkHttpClient okHttpClient = new OkHttpClient();
    private final TranslateService translateService = new TranslateService(okHttpClient);

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

    public String getRecipeDetails(String jsonResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonResponse);

            String title = rootNode.path("title").asText();
            String instructions = rootNode.path("instructions").asText();

            if (instructions != null) {
                instructions = instructions.replaceAll("<ol>", "\n").replaceAll("</ol>", "")
                        .replaceAll("<li>", "- ").replaceAll("</li>", "\n")
                        .replaceAll("<b>", "").replaceAll("</b>", "")
                        .replaceAll("<i>", "").replaceAll("</i>", "");
            }

            if (instructions == null || instructions.isEmpty()) {
                instructions = "Инструкции отсутствуют.";
            }

            return "Вы выбрали рецепт: " + translateService.translateFromEnglish(title) + "\n\nРуководство к действию:\n" + divAndTranslate(instructions);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при парсинге деталей рецепта", e);
            return "Произошла ошибка при обработке данных рецепта.";
        }
    }

    public String divAndTranslate(String str) {
        if (str.length() > 499) {
            List<String> parts = new ArrayList<>();
            String[] sentences = str.split("(?<=[.!?])\\s*");

            StringBuilder currentPart = new StringBuilder();
            for (String sentence : sentences) {
                if (currentPart.length() + sentence.length() > 499) {
                    parts.add(currentPart.toString().trim());
                    currentPart.setLength(0);
                }
                currentPart.append(sentence).append(" ");
            }
            if (!currentPart.isEmpty()) {
                parts.add(currentPart.toString().trim());
            }

            StringBuilder result = new StringBuilder();
            for (String part : parts) {
                result.append(translateService.translateFromEnglish(part)).append(" ");
            }
            return result.toString().trim();
        } else {
            return translateService.translateFromEnglish(str);
        }
    }
}
