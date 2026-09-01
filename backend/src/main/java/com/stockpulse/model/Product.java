package com.stockpulse.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {

    @Id
    private String id;
    private String sku;
    private String name;

    @Enumerated(EnumType.STRING)
    private Category category;

    private BigDecimal currentPrice;
    private Integer stockLevel;
    private Integer reorderThreshold;
    private Integer demandVelocity;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    // Sprint 2 placeholders
    private BigDecimal costPrice;
    private String supplierId;

    public Product() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    
    public Integer getStockLevel() { return stockLevel; }
    public void setStockLevel(Integer stockLevel) { this.stockLevel = stockLevel; }
    
    public Integer getReorderThreshold() { return reorderThreshold; }
    public void setReorderThreshold(Integer reorderThreshold) { this.reorderThreshold = reorderThreshold; }
    
    public Integer getDemandVelocity() { return demandVelocity; }
    public void setDemandVelocity(Integer demandVelocity) { this.demandVelocity = demandVelocity; }
    
    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status; }

    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
    
    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }
}
