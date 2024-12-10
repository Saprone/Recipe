package com.saprone.recipe.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Service
public class RecipeService {

    private final WebClient webClient;
    private static final Logger logger = LoggerFactory.getLogger(RecipeService.class);
    private static final String URL_CATEGORIES_MEAL_DB = "https://www.themealdb.com/api/json/v1/1/categories.php";
    private static final String URL_CATEGORY_RECIPES_MEAL_DB = "https://www.themealdb.com/api/json/v1/1/filter.php?c=";
    //private static final String URL_RECIPE_DETAILS_MEAL_DB = "https://www.themealdb.com/api/json/v1/1/lookup.php?i=";

    public RecipeService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @PostConstruct
    public void fetchRecipes() {
        try {
            JsonNode response = webClient.get()
                .uri(URL_CATEGORIES_MEAL_DB)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

            if (response != null && response.has("categories")) {
                Flux.fromIterable(response.get("categories"))
                    .flatMap(category -> fetchRecipesByCategory(category.get("strCategory").asText()))
                    .flatMap(recipes -> Flux.fromIterable(recipes.findValue("meals")))
                    .map(meal -> meal.get("idMeal").asText()) // Get the idMeal
                    .subscribe(idMeal -> {
                        System.out.println("Meal ID: " + idMeal);
                    }, error -> {
                        logger.error("Error fetching recipes: {}", error.getMessage(), error);
                    });
            }
        } catch (Exception e) {
            logger.error("Error fetching categories: {}", e.getMessage());
        }
    }

    private Flux<JsonNode> fetchRecipesByCategory(String categoryName) {
        return webClient.get()
            .uri(URL_CATEGORY_RECIPES_MEAL_DB + categoryName)
            .retrieve()
            .bodyToFlux(JsonNode.class);
    }
}
