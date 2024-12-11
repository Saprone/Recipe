package com.saprone.recipe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.saprone.recipe.model.IngredientDuplicate;
import com.saprone.recipe.model.Recipe;
import com.saprone.recipe.repository.IngredientDuplicateRepository;
import com.saprone.recipe.repository.RecipeRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.stream.IntStream;

@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final IngredientDuplicateRepository ingredientDuplicateRepository;
    private final WebClient webClient;
    private static final Logger logger = LoggerFactory.getLogger(RecipeService.class);
    private static final String RECIPES_FIRST_LETTER_MEAL_DB = "https://www.themealdb.com/api/json/v1/1/search.php?f=";
    private static final String URL_INGREDIENTS_MEAL_DB = "https://www.themealdb.com/api/json/v1/1/list.php?i=list";

    @Autowired
    public RecipeService(RecipeRepository recipeRepository, WebClient.Builder webClientBuilder, IngredientDuplicateRepository ingredientDuplicateRepository) {
        this.recipeRepository = recipeRepository;
        this.webClient = webClientBuilder.build();
        this.ingredientDuplicateRepository = ingredientDuplicateRepository;
    }

    @PostConstruct
    public void fetchAndSaveIngredientDuplicates() {
        try {
            if (ingredientDuplicateRepository.count() == 0) {
                JsonNode response = webClient.get()
                        .uri(URL_INGREDIENTS_MEAL_DB)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block();

                if (response != null && response.has("meals")) {
                    for (JsonNode meal : response.get("meals")) {
                        String ingredientDuplicateName = meal.get("strIngredient").asText();
                        IngredientDuplicate ingredientDuplicate = new IngredientDuplicate();
                        ingredientDuplicate.setName(ingredientDuplicateName);
                        ingredientDuplicateRepository.save(ingredientDuplicate);
                    }

                    logger.info("IngredientDuplicates fetched and saved successfully.");
                } else {
                    logger.warn("No ingredientDuplicates found in the API response.");
                }
            } else {
                logger.info("Database is not empty. Skipping fetching and saving ingredientDuplicates.");
            }
        } catch (Exception e) {
            logger.error("Error fetching and saving ingredientDuplicates: {}", e.getMessage(), e);
        }
    }

    @PostConstruct
    public void saveRecipeIngredientDuplicates() {
        try {
            IntStream.range(0, 26).mapToObj(i -> (char) ('a' + i)).forEach(letter -> {
                String url = RECIPES_FIRST_LETTER_MEAL_DB + letter;

                webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .subscribe(response -> {
                            JsonNode meals = response.path("meals");

                            if (meals.isArray() && !meals.isEmpty()) {
                                meals.forEach(meal -> {
                                    String recipeID = meal.path("idMeal").asText(null);

                                    for (int i = 1; i <= 20; i++) {
                                        String ingredientName = meal.path("strIngredient" + i).asText(null);

                                        if (ingredientName != null && !ingredientName.isEmpty()) {
                                            ingredientName = convertIngredientName(ingredientName);

                                            IngredientDuplicate ingredientDuplicate = ingredientDuplicateRepository.findByName(ingredientName);

                                            if (ingredientDuplicate != null) {
                                                Long ingredientID = ingredientDuplicate.getId();
                                                System.out.println(recipeID + " | " + ingredientID);
                                            } else {
                                                System.out.println(recipeID + " | " + ingredientName + " | Not found");
                                            }
                                        }
                                    }
                                });
                            } else {
                                logger.warn("No recipes found for letter: {}", letter);
                            }
                        }, error -> {
                            logger.error("Error fetching recipes for the letter {}: {}", letter, error.getMessage());
                        });
            });
        } catch (Exception e) {
            logger.error("Error saving recipeIngredientDuplicates: {}", e.getMessage(), e);
        }
    }

    private String convertIngredientName(String ingredientName) {
        return switch (ingredientName) {
            case "All spice" -> "Allspice";
            case "Blackberrys" -> "Blackberries";
            case "butter, softened" -> "Butternut Squash";
            case "carrot" -> "Carrots";
            case "Carrot" -> "Carrots";
            case "Chicken thigh" -> "Chicken Thighs";
            case "clove" -> "Cloves";
            case "Clove" -> "Cloves";
            case "Green Chili" -> "Green Chilli";
            case "Gruyere cheese" -> "Gruyère";
            case "Harissa" -> "Harissa Spice";
            case "potato" -> "Potatoes";
            case "red chili" -> "Red Chilli";
            case "Red Chili" -> "Red Chilli";
            case "Red Onion" -> "Red Onions";
            case "self raising flour" -> "Self-raising Flour";
            case "spring onion" -> "Spring Onions";
            case "Tarragon" -> "Tarragon Leaves";
            case "Tomato Purée" -> "Tomato Puree";
            case "Vermicelli" -> "Vermicelli Pasta";
            default -> ingredientName;
        };
    }
}
