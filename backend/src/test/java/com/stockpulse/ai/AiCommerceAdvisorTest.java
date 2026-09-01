package com.stockpulse.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpulse.model.Product;
import com.stockpulse.model.TriggerReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class AiCommerceAdvisorTest {

    private LLMGateway llmGateway;
    private RuleBasedCommerceAdvisor fallbackAdvisor;
    private AiCommerceAdvisor aiAdvisor;
    private Product product;

    @BeforeEach
    public void setup() {
        llmGateway = mock(LLMGateway.class);
        fallbackAdvisor = mock(RuleBasedCommerceAdvisor.class);
        aiAdvisor = new AiCommerceAdvisor(llmGateway, fallbackAdvisor, new ObjectMapper());

        product = new Product();
        product.setId("TEST-001");
        product.setCurrentPrice(new BigDecimal("100.00"));
        product.setStockLevel(10);
        product.setReorderThreshold(20);
        product.setCategory(com.stockpulse.model.Category.ELECTRONICS);
    }

    @Test
    public void testFallbackOnMalformedJson() {
        when(llmGateway.callLLM(anyString())).thenReturn("{ invalid json ");
        
        when(fallbackAdvisor.generateRecommendations(product, TriggerReason.INVENTORY_LOW))
                .thenReturn(new AdvisorResult());

        AdvisorResult result = aiAdvisor.generateRecommendations(product, TriggerReason.INVENTORY_LOW);
        
        assertNotNull(result);
        verify(fallbackAdvisor, times(1)).generateRecommendations(product, TriggerReason.INVENTORY_LOW);
    }

    @Test
    public void testFallbackOnNegativePrice() {
        String badJson = "{\"recommendedPrice\": -10.00, \"direction\": \"DECREASE\", \"pricingConfidence\": 0.9, \"recommendedQuantity\": 50, \"reorderConfidence\": 0.9}";
        when(llmGateway.callLLM(anyString())).thenReturn(badJson);
        
        when(fallbackAdvisor.generateRecommendations(product, TriggerReason.INVENTORY_LOW))
                .thenReturn(new AdvisorResult());

        AdvisorResult result = aiAdvisor.generateRecommendations(product, TriggerReason.INVENTORY_LOW);
        
        assertNotNull(result);
        verify(fallbackAdvisor, times(1)).generateRecommendations(product, TriggerReason.INVENTORY_LOW);
    }
    
    @Test
    public void testFallbackOnAbsurdPrice() {
        // 10x current price
        String badJson = "{\"recommendedPrice\": 1000.00, \"direction\": \"INCREASE\", \"pricingConfidence\": 0.9, \"recommendedQuantity\": 50, \"reorderConfidence\": 0.9}";
        when(llmGateway.callLLM(anyString())).thenReturn(badJson);
        
        when(fallbackAdvisor.generateRecommendations(product, TriggerReason.INVENTORY_LOW))
                .thenReturn(new AdvisorResult());

        AdvisorResult result = aiAdvisor.generateRecommendations(product, TriggerReason.INVENTORY_LOW);
        
        assertNotNull(result);
        verify(fallbackAdvisor, times(1)).generateRecommendations(product, TriggerReason.INVENTORY_LOW);
    }
    
    @Test
    public void testFallbackOnInvalidConfidence() {
        String badJson = "{\"recommendedPrice\": 110.00, \"direction\": \"INCREASE\", \"pricingConfidence\": 1.5, \"recommendedQuantity\": 50, \"reorderConfidence\": 0.9}";
        when(llmGateway.callLLM(anyString())).thenReturn(badJson);
        
        when(fallbackAdvisor.generateRecommendations(product, TriggerReason.INVENTORY_LOW))
                .thenReturn(new AdvisorResult());

        AdvisorResult result = aiAdvisor.generateRecommendations(product, TriggerReason.INVENTORY_LOW);
        
        assertNotNull(result);
        verify(fallbackAdvisor, times(1)).generateRecommendations(product, TriggerReason.INVENTORY_LOW);
    }
}
