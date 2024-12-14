package org.example.recipes;

import okhttp3.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

public class TranslateService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TranslateService.class);
    private static final String TRANSLATE_API_URL = "https://api.mymemory.translated.net/get?q=%s&langpair=%s|%s";

    private final OkHttpClient client;

    public TranslateService(OkHttpClient client) {
        this.client = client;
    }

    public String translateToEnglish(String text) {
        String sourceLang = "ru";

        String url = String.format(TRANSLATE_API_URL, text, sourceLang, "en");
        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            assert response.body() != null;
            return parseResponse(response.body().string());
        } catch (IOException e) {
            LOGGER.error("Error during translation", e);
            return "";
        }
    }

    public String translateFromEnglish(String text) {
        String targetLang = "ru";

        String url = String.format(TRANSLATE_API_URL, text, "en", targetLang);
        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            assert response.body() != null;
            return parseResponse(response.body().string());
        } catch (IOException e) {
            LOGGER.error("Error during translation", e);
            return "";
        }
    }

    private String parseResponse(String responseBody) {
        JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonObject responseData = jsonResponse.getAsJsonObject("responseData");
        if (responseData == null) {
            LOGGER.error("Invalid response format: missing 'responseData'");
            return null;
        }
        return responseData.get("translatedText").getAsString();
    }
}