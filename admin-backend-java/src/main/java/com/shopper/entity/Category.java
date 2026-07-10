package com.shopper.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "categories")
public class Category {

    @Id
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 50)
    private String icon;

    private Long parentId;

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer sortOrder;

    @Transient
    private List<Category> children = new ArrayList<>();
}
