package com.stockpulse.ai;

import com.stockpulse.model.ChangeDirection;
import java.math.BigDecimal;

public class AdvisorResult {
    // Pricing
    private BigDecimal recommendedPrice;
    private ChangeDirection direction;
    private Double pricingConfidence;
    private String pricingReasoning;

    // Reorder
    private Integer recommendedQuantity;
    private Double reorderConfidence;
    private String reorderReasoning;
    
    // Getters and Setters
    public BigDecimal getRecommendedPrice() { return recommendedPrice; }
    public void setRecommendedPrice(BigDecimal recommendedPrice) { this.recommendedPrice = recommendedPrice; }
    
    public ChangeDirection getDirection() { return direction; }
    public void setDirection(ChangeDirection direction) { this.direction = direction; }
    
    public Double getPricingConfidence() { return pricingConfidence; }
    public void setPricingConfidence(Double pricingConfidence) { this.pricingConfidence = pricingConfidence; }
    
    public String getPricingReasoning() { return pricingReasoning; }
    public void setPricingReasoning(String pricingReasoning) { this.pricingReasoning = pricingReasoning; }
    
    public Integer getRecommendedQuantity() { return recommendedQuantity; }
    public void setRecommendedQuantity(Integer recommendedQuantity) { this.recommendedQuantity = recommendedQuantity; }
    
    public Double getReorderConfidence() { return reorderConfidence; }
    public void setReorderConfidence(Double reorderConfidence) { this.reorderConfidence = reorderConfidence; }
    
    public String getReorderReasoning() { return reorderReasoning; }
    public void setReorderReasoning(String reorderReasoning) { this.reorderReasoning = reorderReasoning; }
}
