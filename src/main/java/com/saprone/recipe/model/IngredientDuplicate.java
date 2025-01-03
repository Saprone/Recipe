package com.saprone.recipe.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "ingredient_duplicate", schema = "dbo")
public class IngredientDuplicate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @OneToMany(mappedBy = "ingredientDuplicate", cascade = CascadeType.ALL)
    private List<RecipeIngredientDuplicate> recipeIngredientDuplicates;
}
