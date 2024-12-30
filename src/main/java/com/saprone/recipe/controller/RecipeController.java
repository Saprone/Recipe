package com.saprone.recipe.controller;

import com.saprone.recipe.model.Recipe;
import com.saprone.recipe.service.RecipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/recipes")
@CrossOrigin(origins = "*")
public class RecipeController {

    private final RecipeService recipeService;

    @Autowired
    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    public ResponseEntity<List<Recipe>> getRecipes() {
        List<Object> basket = recipeService.getBasketFromMessageQueue();
        List<Long> ingredientBasketIds = recipeService.getIngredientBasketIds(basket);
        List<Recipe> recipes = recipeService.findRecipesOnIngredientsInBasket(ingredientBasketIds);

        return ResponseEntity.ok(recipes);
    }

    @GetMapping("/status")
    public ResponseEntity<Boolean> getRecipeStatus() {
        return ResponseEntity.ok(true);
    }
}
