package com.saprone.recipe.controller;

import com.saprone.recipe.model.Recipe;
import com.saprone.recipe.service.RecipeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RecipeControllerTest {

    @Mock
    private RecipeService recipeService;

    @InjectMocks
    private RecipeController recipeController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getRecipesShouldReturnRecipes() {
        // Arrange
        Recipe recipe = new Recipe();
        recipe.setId(1);
        recipe.setName("Beef Wellington");

        when(recipeService.getBasketFromMessageQueue()).thenReturn(List.of());
        when(recipeService.getIngredientBasketIds(anyList())).thenReturn(List.of());
        when(recipeService.findRecipesOnIngredientsInBasket(anyList())).thenReturn(List.of(recipe));

        // Act
        ResponseEntity<List<Recipe>> response = recipeController.getRecipes();

        // Assert
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()).containsExactly(recipe);
        verify(recipeService, times(1)).getBasketFromMessageQueue();
        verify(recipeService, times(1)).getIngredientBasketIds(anyList());
        verify(recipeService, times(1)).findRecipesOnIngredientsInBasket(anyList());
    }

    @Test
    void getRecipeStatusShouldReturnTrue() {
        // Act
        ResponseEntity<Boolean> response = recipeController.getRecipeStatus();

        // Assert
        assertThat(response.getBody()).isTrue();
        verify(recipeService, times(0)).getBasketFromMessageQueue();
    }
}
