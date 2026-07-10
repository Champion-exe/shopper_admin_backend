package com.shopper.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shopper.config.JsonListConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class Product {

    @Id
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(length = 50)
    private String brand;

    private Long categoryId;

    private Long sellerId;

    private String mainImage;

    @Convert(converter = JsonListConverter.class)
    @Column(length = 2000)
    private List<String> images;

    @Convert(converter = JsonListConverter.class)
    @Column(length = 500)
    private List<String> tags;

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer sales;

    @Column(columnDefinition = "DOUBLE DEFAULT 0")
    private Double rating;

    private Boolean isHot;

    private Boolean isNew;

    private Boolean isSeckill;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Transient
    private Double minPrice;

    @Transient
    private Integer totalStock;
}
