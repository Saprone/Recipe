package com.saprone.recipe.service;

import com.azure.core.util.IterableStream;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.saprone.recipe.model.Recipe;
import com.saprone.recipe.repository.RecipeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Value;

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

        System.out.println("Basket content: " + basket);

        if (basket != null) {
            for (Object item : basket) {
                String idString = item.toString().replaceAll(".*id=(\\d+),.*", "$1");
                ingredientBasketIds.add(Long.parseLong(idString));
            }
        }

        return ingredientBasketIds;
    }

    public List<Recipe> getRecipes(List<Long> ingredientBasketIds) {
        return recipeRepository.findRecipesByIngredientIds(ingredientBasketIds, ingredientBasketIds.size());
    }
}
