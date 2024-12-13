package com.saprone.recipe.controller;

import com.saprone.recipe.model.Recipe;
import com.saprone.recipe.service.RecipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/recipes")
@CrossOrigin(origins = "http://localhost:8085")
public class RecipeController {

    private final RecipeService recipeService;

    @Autowired
    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    public List<Recipe> getRecipes() {
        List<Object> basket = recipeService.getBasketFromMessageQueue();
        List<Long> ingredientBasketIds = recipeService.getIngredientBasketIds(basket);
        List<Recipe> recipes = recipeService.getRecipes(ingredientBasketIds);

        System.out.println("Recipes: "+recipes);

        return recipes;
    }
}
