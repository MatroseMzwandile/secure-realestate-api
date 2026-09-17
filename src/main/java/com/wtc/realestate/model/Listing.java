package com.wtc.realestate.model;

import java.math.BigDecimal;

public class Listing {
    private int id;
    private String title;
    private String description;
    private BigDecimal price;
    private int realtorId;

    public Listing() {}
    public Listing(int id, String title, String description, BigDecimal price, int realtorId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.realtorId = realtorId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public int getRealtorId() { return realtorId; }
    public void setRealtorId(int realtorId) { this.realtorId = realtorId; }
}