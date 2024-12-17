package com.saprone.recipe.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "recipe_ingredient_duplicate", schema = "dbo")
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
