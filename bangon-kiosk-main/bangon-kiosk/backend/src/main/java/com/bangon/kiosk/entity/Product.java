package com.bangon.kiosk.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** A menu item that customers can order. */
@Entity
@Table(name = "products", indexes = @Index(name = "idx_product_name", columnList = "name"))
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int caffeine;

    @Column(nullable = false)
    private int calories;

    @Column(length = 1024)
    private String image;

    /** Pastries and desserts cannot be customized. */
    private boolean pastry;

    private boolean active = true;

    public Product() {
    }

    public Product(Category category, String name, int price, int caffeine,
                   int calories, String image, boolean pastry) {
        this.category = category;
        this.name = name;
        this.price = BigDecimal.valueOf(price);
        this.caffeine = caffeine;
        this.calories = calories;
        this.image = image;
        this.pastry = pastry;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getCaffeine() {
        return caffeine;
    }

    public void setCaffeine(int caffeine) {
        this.caffeine = caffeine;
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public boolean isPastry() {
        return pastry;
    }

    public void setPastry(boolean pastry) {
        this.pastry = pastry;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
