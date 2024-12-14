package com.saprone.recipe.repository;

import com.saprone.recipe.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Integer> {

    @Query("SELECT r FROM Recipe r JOIN r.recipeIngredientDuplicates rid " +
            "WHERE rid.ingredientDuplicate.id IN :ingredientIds " +
            "GROUP BY r.id, r.name, r.image " +
            "HAVING COUNT(rid.ingredientDuplicate.id) = :count")
    List<Recipe> findRecipesByIngredientIds(@Param("ingredientIds") List<Long> ingredientIds, @Param("count") long count);
}
