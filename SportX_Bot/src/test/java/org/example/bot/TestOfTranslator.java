package org.example.bot;

import okhttp3.OkHttpClient;
import org.example.recipes.RecipeParser;
import org.example.recipes.RecipesCommand;
import org.example.recipes.SpoonacularAPI;
import org.example.recipes.TranslateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TestOfTranslator {

    private TranslateService translateService;
    private RecipesCommand recipesCommand;
    private RecipeParser recipeParser;
    private SpoonacularAPI mockSpoonacularAPI;
    private DatabaseManager mockDatabaseManager;

    @BeforeEach
    public void setUp() {
        OkHttpClient client = new OkHttpClient();
        translateService = new TranslateService(client);
        recipeParser = new RecipeParser();
        mockSpoonacularAPI = mock(SpoonacularAPI.class);
        mockDatabaseManager = mock(DatabaseManager.class);
        recipesCommand = new RecipesCommand(mockSpoonacularAPI, mockDatabaseManager);
    }

    @Test
    public void testTranslateToEnglish() {
        String russianText = "помидоры, сыр, курица";
        String translatedText = translateService.translateToEnglish(russianText);
        assertEquals("tomatoes, cheese, chicken", translatedText, "Перевод на английский некорректен");
    }

    @Test
    public void testTranslateToRussian() {
        String englishText = "tomatoes, cheese, chicken";
        String translatedText = translateService.translateFromEnglish(englishText);
        assertEquals("помидоры, сыр, курица", translatedText, "Перевод на русский некорректен");
    }


    @Test
    public void testParseRecipeDetailsWithTranslation() {
        // как бы ответ от Spoonacular
        String mockResponse = """
        {
            "title": "Tomato Soup",
            "instructions": "Step 1: Boil tomatoes. Step 2: Blend them."
        }
    """;

        String translatedTitle = "Томатный суп";
        String translatedInstructions = "Шаг 1: Отварите помидоры. Шаг 2: Смешайте их.";
        String details = recipeParser.getRecipeDetails(mockResponse);

        assertTrue(details.contains(translatedTitle));
        assertTrue(details.contains(translatedInstructions));

    }


    @Test
    public void testFindRecipesWithTranslationIntegration() throws Exception {
        String mockSpoonacularResponse = """
        {
            "results": [
                {"title": "Tomato Soup", "id": 1}
            ]
        }
    """;

        when(mockSpoonacularAPI.searchRecipes(anyString(), anyString(), anyString()))
                .thenReturn(mockSpoonacularResponse);

        String userInput = "помидоры, сыр";
        SendMessage message = recipesCommand.getContent(12345L, userInput);

        String expectedTitle = "Томатный суп";
        assertTrue(message.getText().contains(expectedTitle));
    }
}
