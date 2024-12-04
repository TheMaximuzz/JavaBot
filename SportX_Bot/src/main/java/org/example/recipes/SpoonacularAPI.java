package org.example.recipes;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

//  Основная логика взаимодействия с API Spoonacular. Запрашивает рецепты и информацию о них.
public class SpoonacularAPI {
    private final String apiToken;
    private static final String BASE_URL = "https://api.spoonacular.com";
    private static final Logger logger = Logger.getLogger(SpoonacularAPI.class.getName());

    public SpoonacularAPI(String apiToken) {
        this.apiToken = apiToken;
    }

    public String searchRecipes(String query, String diet, String intolerances) throws Exception {
        String endpoint = "/recipes/complexSearch";
        String url = BASE_URL + endpoint + "?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8) +
                "&diet=" + URLEncoder.encode(diet, StandardCharsets.UTF_8) +
                "&intolerances=" + URLEncoder.encode(intolerances, StandardCharsets.UTF_8) +
                "&apiKey=" + apiToken;

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return response.toString();
            } else {
                logger.severe("Error: API returned code " + responseCode);
                return "Ошибка: API вернул код " + responseCode;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error executing request", e);
            throw new Exception("Ошибка выполнения запроса: " + e.getMessage());
        }
    }

    public String getRecipeInformation(int recipeId) throws Exception {
        String endpoint = "/recipes/" + recipeId + "/information";
        String url = BASE_URL + endpoint + "?apiKey=" + apiToken;

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return response.toString();
            } else {
                logger.severe("Error: API returned code " + responseCode);
                return "Ошибка: API вернул код " + responseCode;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error executing request", e);
            throw new Exception("Ошибка выполнения запроса: " + e.getMessage());
        }
    }
}
