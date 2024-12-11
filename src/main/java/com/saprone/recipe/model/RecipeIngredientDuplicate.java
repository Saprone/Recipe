package com.saprone.recipe.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class RecipeIngredientDuplicate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ingredient_duplicate_id")
    private IngredientDuplicate ingredientDuplicate;

    @ManyToOne
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;
}
