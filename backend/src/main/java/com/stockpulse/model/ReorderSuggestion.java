package com.stockpulse.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reorder_suggestions")
public class ReorderSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private Integer currentStock;
    private Integer recommendedQuantity;
    private Integer suggestedLeadTimeDays;

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

    public ReorderSuggestion() {}

    public Long getId() { return id; }
    
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    
    public Integer getCurrentStock() { return currentStock; }
    public void setCurrentStock(Integer currentStock) { this.currentStock = currentStock; }
    
    public Integer getRecommendedQuantity() { return recommendedQuantity; }
    public void setRecommendedQuantity(Integer recommendedQuantity) { this.recommendedQuantity = recommendedQuantity; }
    
    public Integer getSuggestedLeadTimeDays() { return suggestedLeadTimeDays; }
    public void setSuggestedLeadTimeDays(Integer suggestedLeadTimeDays) { this.suggestedLeadTimeDays = suggestedLeadTimeDays; }
    
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
