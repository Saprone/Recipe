package com.saprone.recipe.repository;

import com.saprone.recipe.model.RecipeIngredientDuplicate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeIngredientDuplicateRepository extends JpaRepository<RecipeIngredientDuplicate, Long> {
}
