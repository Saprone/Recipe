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
import reactor.core.publisher.Flux;
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
    public void fetchAndSaveRecipes() {
        try {
            if (recipeRepository.count() == 0) {
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
                                    String recipeImage = meal.path("strMealThumb").asText();

                                    if (!recipeRepository.existsById(Integer.parseInt(recipeId))) {
                                        Recipe recipe = new Recipe();
                                        recipe.setId(Integer.parseInt(recipeId));
                                        recipe.setName(recipeName);
                                        recipe.setImage(recipeImage);
                                        recipeRepository.save(recipe);
                                        logger.info("Recipe '{}' saved successfully.", recipeName);
                                    } else {
                                        logger.info("Recipe '{}' already exists in the database.", recipeName);
                                    }
                                });
                            }
                        }, error -> {
                            logger.error("Error fetching recipes for letter {}: {}", letter, error.getMessage());
                        });
                });
            } else {
                logger.info("Database is not empty. Skipping fetching and saving recipes.");
            }
        } catch (Exception e) {
            logger.error("Error checking recipe database: {}", e.getMessage(), e);
        }
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

    public Flux<Recipe> getAllRecipes() {
        return Flux.fromIterable(recipeRepository.findAll());
    }

    public Flux<IngredientDuplicate> getAllIngredientDuplicates() {
        return Flux.fromIterable(ingredientDuplicateRepository.findAll());
    }
}
