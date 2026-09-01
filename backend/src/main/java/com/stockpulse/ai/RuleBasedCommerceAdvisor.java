package com.stockpulse.ai;

import com.stockpulse.model.ChangeDirection;
import com.stockpulse.model.Product;
import com.stockpulse.model.TriggerReason;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("ruleBasedCommerceAdvisor")
public class RuleBasedCommerceAdvisor implements CommerceAdvisor {

    @Override
    public AdvisorResult generateRecommendations(Product product, TriggerReason triggerReason) {
        AdvisorResult result = new AdvisorResult();
        
        // Rule-based pricing
        if (product.getStockLevel() < product.getReorderThreshold()) {
            // Low stock -> 10% increase
            BigDecimal increase = product.getCurrentPrice().multiply(new BigDecimal("1.10"));
            result.setRecommendedPrice(increase.setScale(2, RoundingMode.HALF_UP));
            result.setDirection(ChangeDirection.INCREASE);
            result.setPricingReasoning("Rule: Low stock triggered a 10% price increase to protect inventory.");
        } else if (product.getDemandVelocity() > getCategoryAverageVelocity(product)) {
            // Demand spike -> 5% increase
            BigDecimal increase = product.getCurrentPrice().multiply(new BigDecimal("1.05"));
            result.setRecommendedPrice(increase.setScale(2, RoundingMode.HALF_UP));
            result.setDirection(ChangeDirection.INCREASE);
            result.setPricingReasoning("Rule: High demand velocity triggered a 5% price increase.");
        } else {
            result.setRecommendedPrice(product.getCurrentPrice());
            result.setDirection(ChangeDirection.HOLD);
            result.setPricingReasoning("Rule: Standard conditions apply, holding price.");
        }
        result.setPricingConfidence(1.0);

        // Rule-based reorder
        int suggestedQuantity = Math.max(1, (product.getReorderThreshold() * 3) - product.getStockLevel());
        result.setRecommendedQuantity(suggestedQuantity);
        result.setReorderReasoning("Rule: (Reorder Threshold * 3) - Current Stock");
        result.setReorderConfidence(1.0);

        return result;
    }

    private int getCategoryAverageVelocity(Product product) {
        // Simplified mockup for category average
        switch (product.getCategory()) {
            case ELECTRONICS: return 4;
            case APPAREL: return 6;
            case HOME: return 3;
            default: return 5;
        }
    }
}
