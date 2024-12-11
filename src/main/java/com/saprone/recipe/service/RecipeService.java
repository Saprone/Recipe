package com.saprone.recipe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.saprone.recipe.model.Recipe;
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
    private final WebClient webClient;
    private static final Logger logger = LoggerFactory.getLogger(RecipeService.class);
    private static final String RECIPES_FIRST_LETTER_MEAL_DB = "https://www.themealdb.com/api/json/v1/1/search.php?f=";

    @Autowired
    public RecipeService(RecipeRepository recipeRepository, WebClient.Builder webClientBuilder) {
        this.recipeRepository = recipeRepository;
        this.webClient = webClientBuilder.build();
    }

    @PostConstruct
    public void fetchAndSaveRecipes() {
        try {
            if (recipeRepository.count() == 0) {
                fetchAndSaveRecipesFromApi();
            } else {
                logger.info("Database is not empty. Skipping fetching and saving recipes.");
            }
        } catch (Exception e) {
            logger.error("Error checking recipe database: {}", e.getMessage(), e);
        }
    }

    private void fetchAndSaveRecipesFromApi() {
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
                            //String recipeInstructions = meal.path("strInstructions").asText();
                            String recipeImage = meal.path("strMealThumb").asText();

                            if (!recipeRepository.existsById(Integer.parseInt(recipeId))) {
                                Recipe recipe = new Recipe();
                                recipe.setId(Integer.parseInt(recipeId));
                                recipe.setName(recipeName);
                                //recipe.setInstructions(recipeInstructions);
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
    }

    public Flux<Recipe> getAllRecipes() {
        return Flux.fromIterable(recipeRepository.findAll());
    }
}
