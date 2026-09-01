package com.stockpulse.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pricing_suggestions")
public class PricingSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private BigDecimal currentPrice;
    private BigDecimal recommendedPrice;

    @Enumerated(EnumType.STRING)
    private ChangeDirection changeDirection;

    private Double confidence;

    @Column(length = 2000)
    private String reasoning;

    @Enumerated(EnumType.STRING)
    private SuggestionStatus status;

    @Enumerated(EnumType.STRING)
    private TriggerReason triggerReason;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public PricingSuggestion() {}

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    
    public BigDecimal getRecommendedPrice() { return recommendedPrice; }
    public void setRecommendedPrice(BigDecimal recommendedPrice) { this.recommendedPrice = recommendedPrice; }
    
    public ChangeDirection getChangeDirection() { return changeDirection; }
    public void setChangeDirection(ChangeDirection changeDirection) { this.changeDirection = changeDirection; }
    
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    
    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }
    
    public SuggestionStatus getStatus() { return status; }
    public void setStatus(SuggestionStatus status) { this.status = status; }
    
    public TriggerReason getTriggerReason() { return triggerReason; }
    public void setTriggerReason(TriggerReason triggerReason) { this.triggerReason = triggerReason; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
}
