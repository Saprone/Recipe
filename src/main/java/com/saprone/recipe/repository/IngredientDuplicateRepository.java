package com.saprone.recipe.repository;

import com.saprone.recipe.model.IngredientDuplicate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientDuplicateRepository extends JpaRepository<IngredientDuplicate, Long> {
    IngredientDuplicate findByName(String name);
}
