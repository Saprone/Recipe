package com.saprone.recipe.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class Recipe {
    @Id
    private Integer id;
    private String name;
    private String image;
}
