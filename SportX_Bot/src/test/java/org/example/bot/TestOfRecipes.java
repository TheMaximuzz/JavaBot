package org.example.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class TestOfRecipes {

    private RecipesCommand recipesCommand;
    private SpoonacularAPI mockSpoonacularAPI;
    private DatabaseManager mockDatabaseManager;

    @BeforeEach
    public void setUp() {
        mockSpoonacularAPI = mock(SpoonacularAPI.class);
        mockDatabaseManager = mock(DatabaseManager.class);
        recipesCommand = new RecipesCommand(mockSpoonacularAPI, mockDatabaseManager);
    }

    @Test
    public void testGetContent() throws Exception {
        // это просто строка в формате JSON, иммитируем ответ от SpoonacularAPI
        String mockResponse = "{\"results\":[{\"title\":\"Tomato Soup\",\"id\":123}, {\"title\":\"Cheese Sandwich\",\"id\":124}]}";
        when(mockSpoonacularAPI.searchRecipes(anyString(), anyString(), anyString())).thenReturn(mockResponse);

        SendMessage message = recipesCommand.getContent(1656675887L, "tomatoes, cheese, chicken");

        assert(message.getText().contains("Рецепты, которые могут вам подойти"));
        assert(message.getReplyMarkup() != null);  // кнопки должны быть

        // был ли метод вызван с правильными параметрами
        verify(mockSpoonacularAPI).searchRecipes("tomatoes, cheese, chicken", "", "");
    }

    @Test
    public void testGetContent_EmptyInput() throws Exception {
        // что будет при пустом вводе
        SendMessage message = recipesCommand.getContent(1656675887L, "");
        assert(message.getText().contains("Не удалось обработать ваш запрос"));
    }

    @Test
    public void testGetRecipeDetails() throws Exception {
        String mockRecipeResponse = "{\"title\":\"Tomato Soup\",\"instructions\":\"Step 1: Boil tomatoes. Step 2: Blend.\"}";

        when(mockSpoonacularAPI.getRecipeInformation(123)).thenReturn(mockRecipeResponse);
        String recipeDetails = recipesCommand.getRecipeDetails(123);

        // есть ли то, что нам нужно будет вывести
        assert(recipeDetails.contains("Tomato Soup"));
        assert(recipeDetails.contains("Step 1: Boil tomatoes."));
    }


    // ещё проверка SpooonacularAPI, чтобы убедиться в корректности обработки API-запроса
    @Test
    public void testSearchRecipes() throws Exception {
        // как бы HTTP-ответ от SpoonacularAPI
        String mockResponse = "{\"results\":[{\"title\":\"Tomato Soup\",\"id\":123}, {\"title\":\"Cheese Sandwich\",\"id\":124}]}";

        // мокируем запрос
        when(mockSpoonacularAPI.searchRecipes("tomatoes, cheese, chicken", "", "")).thenReturn(mockResponse);

        // вызываем метод
        String response = mockSpoonacularAPI.searchRecipes("tomatoes, cheese, chicken", "", "");

        // правильность вызванных данных
        assert(response.contains("Tomato Soup"));
        assert(response.contains("Cheese Sandwich"));

        // правильность параметров
        verify(mockSpoonacularAPI).searchRecipes("tomatoes, cheese, chicken", "", "");
    }

}
