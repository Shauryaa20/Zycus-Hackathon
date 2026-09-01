package com.stockpulse.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpulse.model.ChangeDirection;
import com.stockpulse.model.Product;
import com.stockpulse.model.TriggerReason;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component("aiCommerceAdvisor")
public class AiCommerceAdvisor implements CommerceAdvisor {

    private final LLMGateway llmGateway;
    private final RuleBasedCommerceAdvisor fallbackAdvisor;
    private final ObjectMapper objectMapper;

    @Autowired
    public AiCommerceAdvisor(LLMGateway llmGateway, 
                             RuleBasedCommerceAdvisor fallbackAdvisor,
                             ObjectMapper objectMapper) {
        this.llmGateway = llmGateway;
        this.fallbackAdvisor = fallbackAdvisor;
        this.objectMapper = objectMapper;
    }

    @Override
    public AdvisorResult generateRecommendations(Product product, TriggerReason triggerReason) {
        try {
            String prompt;
            if (triggerReason == TriggerReason.INVENTORY_LOW) {
                prompt = buildInventoryLowPrompt(product);
            } else if (triggerReason == TriggerReason.DEMAND_SPIKE) {
                prompt = buildDemandSpikePrompt(product);
            } else {
                prompt = buildGenericPrompt(product, triggerReason);
            }
            
            String jsonResponse = llmGateway.callLLM(prompt);
            
            // Expected JSON:
            // { "recommendedPrice": 29.99, "direction": "INCREASE", "pricingConfidence": 0.82, "pricingReasoning": "...", 
            //   "recommendedQuantity": 150, "reorderConfidence": 0.78, "reorderReasoning": "..." }
            
            Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, Map.class);
            AdvisorResult result = parseResponse(responseMap);
            
            // Explicit Validation Boundary
            validateResult(result, product);

            return result;
            
        } catch (Exception e) {
            // Fallback to rule-based on any failure (timeout, parsing, bounds validation)
            System.err.println("[AI Advisor Error] Falling back to rules. Reason: " + e.getMessage());
            return fallbackAdvisor.generateRecommendations(product, triggerReason);
        }
    }

    private AdvisorResult parseResponse(Map<String, Object> responseMap) {
        AdvisorResult result = new AdvisorResult();
        
        // Parse Pricing
        if (responseMap.containsKey("recommendedPrice")) {
            result.setRecommendedPrice(new BigDecimal(responseMap.get("recommendedPrice").toString()));
            result.setDirection(ChangeDirection.valueOf((String) responseMap.get("direction")));
            result.setPricingConfidence(((Number) responseMap.get("pricingConfidence")).doubleValue());
            result.setPricingReasoning((String) responseMap.get("pricingReasoning"));
        } else {
            throw new RuntimeException("Missing pricing fields in LLM response");
        }
        
        // Parse Reorder
        if (responseMap.containsKey("recommendedQuantity")) {
            result.setRecommendedQuantity(((Number) responseMap.get("recommendedQuantity")).intValue());
            result.setReorderConfidence(((Number) responseMap.get("reorderConfidence")).doubleValue());
            result.setReorderReasoning((String) responseMap.get("reorderReasoning"));
        } else {
             throw new RuntimeException("Missing reorder fields in LLM response");
        }
        
        return result;
    }

    private void validateResult(AdvisorResult result, Product product) {
        if (result.getRecommendedPrice() == null || result.getRecommendedPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid recommended price (must be > 0)");
        }
        
        // Sane bounds checking: max 2x current price, min 0.2x current price
        BigDecimal currentPrice = product.getCurrentPrice();
        BigDecimal maxPrice = currentPrice.multiply(new BigDecimal("2.0"));
        BigDecimal minPrice = currentPrice.multiply(new BigDecimal("0.2"));
        
        if (result.getRecommendedPrice().compareTo(maxPrice) > 0 || result.getRecommendedPrice().compareTo(minPrice) < 0) {
            throw new RuntimeException("Recommended price violates sane bounds (0.2x - 2.0x of current price).");
        }
        
        if (result.getRecommendedQuantity() == null || result.getRecommendedQuantity() < 1) {
             throw new RuntimeException("Invalid recommended quantity (must be >= 1)");
        }
        
        if (result.getPricingConfidence() < 0.0 || result.getPricingConfidence() > 1.0) {
             throw new RuntimeException("Invalid pricing confidence (must be between 0.0 and 1.0)");
        }

        if (result.getReorderConfidence() < 0.0 || result.getReorderConfidence() > 1.0) {
             throw new RuntimeException("Invalid reorder confidence (must be between 0.0 and 1.0)");
        }
    }

    private String buildInventoryLowPrompt(Product product) {
        return String.format(
            "You are an expert retail merchandiser. You must output raw JSON only.\n" +
            "The following product has hit dangerously LOW INVENTORY.\n\n" +
            "Product: %s (SKU: %s)\n" +
            "Category: %s\n" +
            "Current Price: $%.2f\n" +
            "Stock Level: %d (Below Reorder Threshold: %d)\n" +
            "Demand Velocity (last 24h): %d\n\n" +
            "SITUATION & TRADEOFFS:\n" +
            "- The stock is below the reorder threshold.\n" +
            "- If demand velocity is high, you should strongly consider RAISING the price to protect remaining inventory and maximize margin before stockout.\n" +
            "- If demand velocity is extremely low, this might be a slow-moving item, and a CLEARANCE price (decrease) could be appropriate to liquidate remaining units.\n" +
            "- You MUST suggest a solid replenishment quantity to bring stock safely above the threshold (%d).\n\n" +
            "%s",
            product.getName(), product.getSku(), product.getCategory(), 
            product.getCurrentPrice(), product.getStockLevel(), product.getReorderThreshold(),
            product.getDemandVelocity(), product.getReorderThreshold(), getJsonFormatInstruction()
        );
    }
    
    private String buildDemandSpikePrompt(Product product) {
        int categoryAvgVelocity = getCategoryAverageVelocity(product);
        return String.format(
            "You are an expert retail merchandiser. You must output raw JSON only.\n" +
            "The following product is experiencing a massive DEMAND SPIKE.\n\n" +
            "Product: %s (SKU: %s)\n" +
            "Category: %s\n" +
            "Current Price: $%.2f\n" +
            "Stock Level: %d (Reorder Threshold: %d)\n" +
            "Current Demand Velocity (last 24h): %d\n" +
            "Category Average Velocity: %d\n\n" +
            "SITUATION & TRADEOFFS:\n" +
            "- Demand velocity has suddenly increased well beyond the category average.\n" +
            "- The business should capitalize on this increased demand with a modest or aggressive price increase depending on the magnitude of the spike.\n" +
            "- Stock availability must be considered. If stock is also low, the price increase should be steeper.\n" +
            "- Reorder decisions must account for the new higher velocity, likely requiring a significantly larger order quantity than normal.\n\n" +
            "%s",
            product.getName(), product.getSku(), product.getCategory(), 
            product.getCurrentPrice(), product.getStockLevel(), product.getReorderThreshold(),
            product.getDemandVelocity(), categoryAvgVelocity, getJsonFormatInstruction()
        );
    }
    
    private String buildGenericPrompt(Product product, TriggerReason triggerReason) {
        return String.format(
            "You are an expert retail merchandiser. You must output raw JSON only.\n" +
            "Given the following product context, provide pricing and reorder recommendations for event: %s\n\n" +
            "Product: %s (SKU: %s)\n" +
            "Category: %s\n" +
            "Current Price: $%.2f\n" +
            "Stock Level: %d (Reorder Threshold: %d)\n" +
            "Demand Velocity (last 24h): %d\n\n" +
            "%s",
            triggerReason.name(), product.getName(), product.getSku(), product.getCategory(), 
            product.getCurrentPrice(), product.getStockLevel(), product.getReorderThreshold(),
            product.getDemandVelocity(), getJsonFormatInstruction()
        );
    }
    
    private String getJsonFormatInstruction() {
        return "Output ONLY JSON matching this exact format:\n" +
               "{\n" +
               "  \"recommendedPrice\": 29.99,\n" +
               "  \"direction\": \"INCREASE\", // INCREASE, DECREASE, or HOLD\n" +
               "  \"pricingConfidence\": 0.85,\n" +
               "  \"pricingReasoning\": \"Explain merchandising tradeoff...\",\n" +
               "  \"recommendedQuantity\": 150,\n" +
               "  \"reorderConfidence\": 0.85,\n" +
               "  \"reorderReasoning\": \"Explain reorder logic...\"\n" +
               "}";
    }
    
    private int getCategoryAverageVelocity(Product product) {
        switch (product.getCategory()) {
            case ELECTRONICS: return 4;
            case APPAREL: return 6;
            case HOME: return 3;
            default: return 5;
        }
    }
}
