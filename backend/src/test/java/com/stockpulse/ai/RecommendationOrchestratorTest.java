package com.stockpulse.ai;

import com.stockpulse.model.ChangeDirection;
import com.stockpulse.model.PricingSuggestion;
import com.stockpulse.model.Product;
import com.stockpulse.model.ProductStatus;
import com.stockpulse.model.ReorderSuggestion;
import com.stockpulse.model.SuggestionStatus;
import com.stockpulse.model.TriggerReason;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
public class RecommendationOrchestratorTest {

    @Autowired
    private RecommendationOrchestrator orchestrator;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PricingSuggestionRepository pricingSuggestionRepository;

    @Autowired
    private ReorderSuggestionRepository reorderSuggestionRepository;

    private Product testProduct;

    @BeforeEach
    public void setup() {
        testProduct = new Product();
        testProduct.setId("TEST-001");
        testProduct.setCurrentPrice(new BigDecimal("100.00"));
        testProduct.setStockLevel(10);
        testProduct.setReorderThreshold(20);
        testProduct.setCategory(com.stockpulse.model.Category.ELECTRONICS);
        testProduct.setStatus(ProductStatus.ACTIVE);
        productRepository.save(testProduct);
    }

    @Test
    public void testIndependentIdempotency() {
        // 1. Manually add a pending PRICING suggestion for INVENTORY_LOW
        PricingSuggestion pricing = new PricingSuggestion();
        pricing.setProduct(testProduct);
        pricing.setStatus(SuggestionStatus.PENDING);
        pricing.setTriggerReason(TriggerReason.INVENTORY_LOW);
        pricing.setRecommendedPrice(new BigDecimal("110.00"));
        pricing.setChangeDirection(ChangeDirection.INCREASE);
        pricingSuggestionRepository.save(pricing);

        // 2. Trigger the orchestrator for INVENTORY_LOW
        // It should NOT create a new pricing suggestion, but MUST create a reorder suggestion
        orchestrator.generateAndPersistRecommendations(testProduct, TriggerReason.INVENTORY_LOW);

        List<PricingSuggestion> pricings = pricingSuggestionRepository.findByProductIdAndStatusAndTriggerReason(
                testProduct.getId(), SuggestionStatus.PENDING, TriggerReason.INVENTORY_LOW);
        List<ReorderSuggestion> reorders = reorderSuggestionRepository.findByProductIdAndStatusAndTriggerReason(
                testProduct.getId(), SuggestionStatus.PENDING, TriggerReason.INVENTORY_LOW);

        // Pricing remains 1 (no duplicate), Reorder is 1 (newly created)
        assertEquals(1, pricings.size(), "Should only have the 1 pricing suggestion we manually inserted");
        assertEquals(1, reorders.size(), "Should have successfully created 1 reorder suggestion");
    }

    @Test
    public void testDifferentTriggersDoNotSuppressEachOther() {
        // 1. Manually add pending PRICING & REORDER for INVENTORY_LOW
        PricingSuggestion pricing = new PricingSuggestion();
        pricing.setProduct(testProduct);
        pricing.setStatus(SuggestionStatus.PENDING);
        pricing.setTriggerReason(TriggerReason.INVENTORY_LOW);
        pricing.setRecommendedPrice(new BigDecimal("110.00"));
        pricing.setChangeDirection(ChangeDirection.INCREASE);
        pricingSuggestionRepository.save(pricing);

        ReorderSuggestion reorder = new ReorderSuggestion();
        reorder.setProduct(testProduct);
        reorder.setStatus(SuggestionStatus.PENDING);
        reorder.setTriggerReason(TriggerReason.INVENTORY_LOW);
        reorder.setRecommendedQuantity(50);
        reorderSuggestionRepository.save(reorder);

        // 2. Trigger orchestrator for DEMAND_SPIKE
        orchestrator.generateAndPersistRecommendations(testProduct, TriggerReason.DEMAND_SPIKE);

        // 3. Verify that DEMAND_SPIKE suggestions were created successfully
        List<PricingSuggestion> spikePricings = pricingSuggestionRepository.findByProductIdAndStatusAndTriggerReason(
                testProduct.getId(), SuggestionStatus.PENDING, TriggerReason.DEMAND_SPIKE);
        List<ReorderSuggestion> spikeReorders = reorderSuggestionRepository.findByProductIdAndStatusAndTriggerReason(
                testProduct.getId(), SuggestionStatus.PENDING, TriggerReason.DEMAND_SPIKE);

        assertEquals(1, spikePricings.size(), "Should create pricing for DEMAND_SPIKE");
        assertEquals(1, spikeReorders.size(), "Should create reorder for DEMAND_SPIKE");
    }
}
