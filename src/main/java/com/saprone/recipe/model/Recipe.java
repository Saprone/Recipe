package com.saprone.recipe.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Getter
@Setter
public class Recipe {
    @Id
    private Integer id;
    private String name;
    private String image;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<RecipeIngredientDuplicate> recipeIngredientDuplicates;
}
