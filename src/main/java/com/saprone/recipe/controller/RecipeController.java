package com.saprone.recipe.controller;

import com.saprone.recipe.model.Recipe;
import com.saprone.recipe.service.RecipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
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
    //public ResponseEntity<List<Recipe>> getRecipes() {
    public void getRecipes() {
        //List<Object> basket = recipeService.getBasketFromMessageQueue();
        //List<Long> ingredientBasketIds = recipeService.getIngredientBasketIds(basket);

        List<Long> ingredientBasketIds = new ArrayList<>();
        ingredientBasketIds.add(11L);
        ingredientBasketIds.add(112L);
        ingredientBasketIds.add(209L);
        ingredientBasketIds.add(300L);
        ingredientBasketIds.add(378L);
        ingredientBasketIds.add(379L);
        ingredientBasketIds.add(382L);

        System.out.println(ingredientBasketIds);

        List<Recipe> recipes = recipeService.getRecipes(ingredientBasketIds);

        System.out.println("Recipes: "+recipes);

        //return ResponseEntity.ok(recipes);
    }
}
