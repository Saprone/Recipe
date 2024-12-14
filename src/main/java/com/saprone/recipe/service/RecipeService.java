package com.saprone.recipe.service;

import com.saprone.recipe.model.Recipe;
import com.saprone.recipe.repository.RecipeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import java.util.ArrayList;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import com.azure.core.util.IterableStream;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;

@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private static final Logger logger = LoggerFactory.getLogger(RecipeService.class);
    @Value("${spring.cloud.azure.servicebus.connection-string}")
    private String serviceBusConnectionString;
    @Value("${spring.cloud.azure.servicebus.entity-name}")
    private String serviceBusEntityName;

    @Autowired
    public RecipeService(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    public List<Object> getBasketFromMessageQueue() {
        ServiceBusReceiverClient receiverClient = new ServiceBusClientBuilder()
            .connectionString(serviceBusConnectionString)
            .receiver()
            .queueName(serviceBusEntityName)
            .buildClient();

        List<Object> baskets = new ArrayList<>();

        try {
            IterableStream<ServiceBusReceivedMessage> messagesStream = receiverClient.receiveMessages(10);
            List<ServiceBusReceivedMessage> messages = new ArrayList<>();
            messagesStream.forEach(messages::add);

            if (!messages.isEmpty()) {
                ServiceBusReceivedMessage lastMessage = messages.getLast();
                Object basket = lastMessage.getBody();
                baskets.add(basket);
                receiverClient.complete(lastMessage);
            }
        } catch (Exception e) {
            logger.error("Error receiving basket from message queue: {}", e.getMessage(), e);
        } finally {
            receiverClient.close();
        }

        return baskets;
    }

    public List<Long> getIngredientBasketIds(List<Object> basket) {
        List<Long> ingredientBasketIds = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        if (basket != null && !basket.isEmpty()) {
            for (Object item : basket) {
                try {
                    String jsonString = item.toString();
                    List<Map<String, Object>> items = objectMapper.readValue(jsonString, new TypeReference<List<Map<String, Object>>>() {});

                    for (Map<String, Object> map : items) {
                        Long id = ((Number) map.get("id")).longValue();
                        ingredientBasketIds.add(id);
                    }
                } catch (Exception e) {
                    logger.error("Error parsing basket item: {}", e.getMessage(), e);
                }
            }
        }

        return ingredientBasketIds;
    }

    public List<Recipe> findRecipesOnIngredientsInBasket(List<Long> ingredientIds) {
        return recipeRepository.findRecipesByIngredientIds(ingredientIds, ingredientIds.size());
    }

    /// Code below was used to create the databases and populate them
    /// However the code needs to be refactored, because reactive web app is changed to web app
    /// I keep it here as reference and low priority fix, because the databases should not change often

    /*private final IngredientDuplicateRepository ingredientDuplicateRepository;
    private final RecipeIngredientDuplicateRepository recipeIngredientDuplicateRepository;
    private final WebClient webClient;
    private static final String RECIPES_FIRST_LETTER_MEAL_DB = "https://www.themealdb.com/api/json/v1/1/search.php?f=";
    private static final String URL_INGREDIENTS_MEAL_DB = "https://www.themealdb.com/api/json/v1/1/list.php?i=list";

    @Autowired
    public RecipeService(RecipeRepository recipeRepository, WebClient.Builder webClientBuilder, IngredientDuplicateRepository ingredientDuplicateRepository, RecipeIngredientDuplicateRepository recipeIngredientDuplicateRepository) {
        this.recipeRepository = recipeRepository;
        this.webClient = webClientBuilder.build();
        this.ingredientDuplicateRepository = ingredientDuplicateRepository;
        this.recipeIngredientDuplicateRepository = recipeIngredientDuplicateRepository;
    }

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
            logger.error("Error checking recipe database: {}", e.getMessage());
        }
    }

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

    public void saveRecipeIngredientDuplicates() {
        try {
            if (recipeIngredientDuplicateRepository.count() == 0) {
                //looping through alphabet does save all the recipe ingredient id pairs in the joined table
                //however the application shuts down at the end: "failed to start bean 'webServerStartStop'"
                var letter = "a";
                String url = RECIPES_FIRST_LETTER_MEAL_DB + letter;

                webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .subscribe(response -> {
                        JsonNode meals = response.path("meals");

                        if (meals.isArray()) {
                            meals.forEach(meal -> {
                                Recipe recipe = new Recipe();
                                recipe.setId(Integer.parseInt(meal.path("idMeal").asText()));

                                for (int i = 1; i <= 20; i++) {
                                    String ingredientName = meal.path("strIngredient" + i).asText(null);

                                    if (ingredientName != null && !ingredientName.isEmpty()) {
                                        ingredientName = convertIngredientName(ingredientName);
                                        IngredientDuplicate ingredientDuplicate = ingredientDuplicateRepository.findByName(ingredientName);

                                        if (ingredientDuplicate != null) {
                                            RecipeIngredientDuplicate recipeIngredientDuplicate = new RecipeIngredientDuplicate();
                                            recipeIngredientDuplicate.setRecipe(recipe);
                                            recipeIngredientDuplicate.setIngredientDuplicate(ingredientDuplicate);

                                            logger.info("Saved RecipeIngredientDuplicate for Recipe ID '{}' and Ingredient ID '{}'.", recipe.getId(), ingredientDuplicate.getId());
                                            recipeIngredientDuplicateRepository.save(recipeIngredientDuplicate);
                                        } else {
                                            logger.warn("Ingredient '{}' not found in the database.", ingredientName);
                                        }
                                    }
                                }
                            });
                        }
                    }, error -> {
                        logger.error("Error fetching recipes for the letter {}: {}", letter, error.getMessage());
                    });
            }
        } catch (Exception e) {
            logger.error("Error checking recipe ingredient duplicates database: {}", e.getMessage());
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
    }*/
}
