package com.saprone.recipe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.saprone.recipe.model.IngredientDuplicate;
import com.saprone.recipe.model.Recipe;
import com.saprone.recipe.model.RecipeIngredientDuplicate;
import com.saprone.recipe.repository.IngredientDuplicateRepository;
import com.saprone.recipe.repository.RecipeIngredientDuplicateRepository;
import com.saprone.recipe.repository.RecipeRepository;
import com.azure.core.util.IterableStream;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private static final Logger logger = LoggerFactory.getLogger(RecipeService.class);
    @Value("${spring.cloud.azure.servicebus.connection-string}")
    private String serviceBusConnectionString;
    @Value("${spring.cloud.azure.servicebus.entity-name}")
    private String serviceBusEntityName;
    private final IngredientDuplicateRepository ingredientDuplicateRepository;
    private final RecipeIngredientDuplicateRepository recipeIngredientDuplicateRepository;
    private final RestTemplate restTemplate;
    private static final String RECIPES_FIRST_LETTER_MEAL_DB = "https://www.themealdb.com/api/json/v1/1/search.php?f=";
    private static final String URL_INGREDIENTS_MEAL_DB = "https://www.themealdb.com/api/json/v1/1/list.php?i=list";

    @Autowired
    public RecipeService(RecipeRepository recipeRepository, IngredientDuplicateRepository ingredientDuplicateRepository, RecipeIngredientDuplicateRepository recipeIngredientDuplicateRepository, RestTemplate restTemplate) {
        this.recipeRepository = recipeRepository;
        this.ingredientDuplicateRepository = ingredientDuplicateRepository;
        this.recipeIngredientDuplicateRepository = recipeIngredientDuplicateRepository;
        this.restTemplate = restTemplate;
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

                for (ServiceBusReceivedMessage message : messages) {
                    receiverClient.complete(message);
                }
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

    //1. Create 'ingredient_duplicate' table + populate it (after fetching)
    public void fetchAndSaveIngredientDuplicates() {
        try {
            if (ingredientDuplicateRepository.count() == 0) {
                ResponseEntity<JsonNode> response = restTemplate.getForEntity(URL_INGREDIENTS_MEAL_DB, JsonNode.class);
                String pathName = "meals";

                if (response.getBody() != null && response.getBody().has(pathName) {
                    for (JsonNode meal : response.getBody().get(pathName)) {
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
            logger.error("Error fetching and saving ingredientDuplicates: {}", e.getMessage());
        }
    }

    //2. Create 'recipe' table + populate it (after fetching)
    public void fetchAndSaveRecipes() {
        try {
            if (recipeRepository.count() == 0) {
                IntStream.range(0, 26).mapToObj(i -> (char) ('a' + i)).forEach(letter -> {
                    String url = RECIPES_FIRST_LETTER_MEAL_DB + letter;
                    String pathName = "meals";

                    try {
                        ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
                        JsonNode meals = Objects.requireNonNull(response.getBody()).path(pathName);

                        if (meals.isArray()) {
                            meals.forEach(meal -> {
                                String recipeId = meal.path("idMeal").asText();
                                String recipeName = meal.path("strMeal").asText();

                                if (!recipeRepository.existsById(Integer.parseInt(recipeId))) {
                                    Recipe recipe = new Recipe();
                                    recipe.setId(Integer.parseInt(recipeId));
                                    recipe.setName(recipeName);
                                    recipeRepository.save(recipe);
                                    logger.info("Recipe '{}' saved successfully.", recipeName);
                                } else {
                                    logger.info("Recipe '{}' already exists in the database.", recipeName);
                                }
                            });
                        }
                    } catch (Exception error) {
                        logger.error("Error fetching recipes for letter {}: {}", letter, error.getMessage());
                    }
                });
            } else {
                logger.info("Database is not empty. Skipping fetching and saving recipes.");
            }
        } catch (Exception e) {
            logger.error("Error checking recipe database: {}", e.getMessage());
        }
    }

    //3. Create 'recipe_ingredient_duplicate' table + populate it (after fetching)
    public void saveRecipeIngredientDuplicates() {
        try {
            if (recipeIngredientDuplicateRepository.count() == 0) {
                IntStream.range(0, 26)
                    .mapToObj(i -> (char) ('a' + i))
                    .forEach(this::fetchAndSaveRecipesForLetter);
            }
        } catch (Exception e) {
            logger.error("Error checking recipe ingredient duplicates database: {}", e.getMessage());
        }
    }

    private void fetchAndSaveRecipesForLetter(char letter) {
        String url = RECIPES_FIRST_LETTER_MEAL_DB + letter;
        String pathName = "meals";

        try {
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
            JsonNode meals = Objects.requireNonNull(response.getBody()).path(pathName);

            if (meals.isArray()) {
                meals.forEach(this::processRecipe);
            }
        } catch (Exception error) {
            logger.error("Error fetching recipes for the letter {}: {}", letter, error.getMessage());
        }
    }

    private void processRecipe(JsonNode meal) {
        Recipe recipe = new Recipe();
        recipe.setId(Integer.parseInt(meal.path("idMeal").asText()));

        for (int i = 1; i <= 20; i++) {
            String ingredientName = meal.path("strIngredient" + i).asText(null);
            
            if (ingredientName != null && !ingredientName.isEmpty()) {
                processIngredient(recipe, ingredientName);
            }
        }
    }

    private void processIngredient(Recipe recipe, String ingredientName) {
        ingredientName = convertIngredientName(ingredientName);
        IngredientDuplicate ingredientDuplicate = ingredientDuplicateRepository.findByName(ingredientName);

        if (ingredientDuplicate != null) {
            saveRecipeIngredientDuplicate(recipe, ingredientDuplicate);
        } else {
            logger.warn("Ingredient '{}' not found in the database.", ingredientName);
        }
    }

    private void saveRecipeIngredientDuplicate(Recipe recipe, IngredientDuplicate ingredientDuplicate) {
        RecipeIngredientDuplicate recipeIngredientDuplicate = new RecipeIngredientDuplicate();
        recipeIngredientDuplicate.setRecipe(recipe);
        recipeIngredientDuplicate.setIngredientDuplicate(ingredientDuplicate);

        logger.info("Saved RecipeIngredientDuplicate for Recipe ID '{}' and Ingredient ID '{}'.", recipe.getId(), ingredientDuplicate.getId());
        recipeIngredientDuplicateRepository.save(recipeIngredientDuplicate);
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
