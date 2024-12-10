package com.saprone.recipe.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.stream.IntStream;

@Service
public class RecipeService {

    private final WebClient webClient;
    private static final Logger logger = LoggerFactory.getLogger(RecipeService.class);
    private static final String RECIPES_FIRST_LETTER_MEAL_DB = "https://www.themealdb.com/api/json/v1/1/search.php?f=";

    public RecipeService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @PostConstruct
    public void fetchRecipes() {
        IntStream.range(0, 26).mapToObj(i -> (char) ('a' + i)).forEach(letter -> {
            String url = RECIPES_FIRST_LETTER_MEAL_DB + letter;

            webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .subscribe(response -> {
                    JsonNode meals = response.path("meals");
                    if (meals.isArray()) {
                        meals.forEach(meal -> {
                            String recipeId = meal.path("idMeal").asText();
                            String recipeName = meal.path("strMeal").asText();
                            String recipeIngredient1 = meal.path("strIngredient1").asText();
                            String recipeIngredient2 = meal.path("strIngredient2").asText();
                            String recipeIngredient3 = meal.path("strIngredient3").asText();

                            System.out.println(recipeId+" | "+recipeName+" | "+recipeIngredient1+", "+recipeIngredient2+", "+recipeIngredient3+", etc..");
                        });
                    }
                }, error -> {
                    logger.error("Error fetching recipes for letter {}: {}", letter, error.getMessage());
                });
        });
    }
}
