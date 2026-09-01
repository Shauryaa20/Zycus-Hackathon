package com.stockpulse.ai;

import com.stockpulse.model.PricingSuggestion;
import com.stockpulse.model.Product;
import com.stockpulse.model.ProductStatus;
import com.stockpulse.model.ReorderSuggestion;
import com.stockpulse.model.SuggestionStatus;
import com.stockpulse.model.TriggerReason;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationOrchestrator {

    private final StrategyResolver strategyResolver;
    private final PricingSuggestionRepository pricingSuggestionRepository;
    private final ReorderSuggestionRepository reorderSuggestionRepository;
    private final ProductRepository productRepository;

    @Autowired
    public RecommendationOrchestrator(StrategyResolver strategyResolver,
                                      PricingSuggestionRepository pricingSuggestionRepository,
                                      ReorderSuggestionRepository reorderSuggestionRepository,
                                      ProductRepository productRepository) {
        this.strategyResolver = strategyResolver;
        this.pricingSuggestionRepository = pricingSuggestionRepository;
        this.reorderSuggestionRepository = reorderSuggestionRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public void generateAndPersistRecommendations(Product product, TriggerReason triggerReason) {
        
        // Idempotency check: don't create duplicate pending suggestions for same product + trigger reason
        boolean hasPendingPricing = !pricingSuggestionRepository
                .findByProductIdAndStatusAndTriggerReason(product.getId(), SuggestionStatus.PENDING, triggerReason).isEmpty();
        
        boolean hasPendingReorder = !reorderSuggestionRepository
                .findByProductIdAndStatusAndTriggerReason(product.getId(), SuggestionStatus.PENDING, triggerReason).isEmpty();

        if (hasPendingPricing && hasPendingReorder) {
            System.out.println("Pending suggestions already exist for " + product.getId() + " - skipping agentic loop.");
            return;
        }

        System.out.println("Running agentic loop for product: " + product.getId() + " due to: " + triggerReason);
        
        // Resolve active strategy dynamically
        CommerceAdvisor activeAdvisor = strategyResolver.resolveActiveAdvisor();
        
        // Generate Recommendations
        AdvisorResult result = activeAdvisor.generateRecommendations(product, triggerReason);

        // Update Product Status if it was Active
        if (product.getStatus() == ProductStatus.ACTIVE) {
            product.setStatus(ProductStatus.PRICE_REVIEW_PENDING);
            productRepository.save(product);
        }

        // Save Pricing Suggestion
        if (!hasPendingPricing) {
            PricingSuggestion pricingSuggestion = new PricingSuggestion();
            pricingSuggestion.setProduct(product);
            pricingSuggestion.setCurrentPrice(product.getCurrentPrice());
            pricingSuggestion.setRecommendedPrice(result.getRecommendedPrice());
            pricingSuggestion.setChangeDirection(result.getDirection());
            pricingSuggestion.setConfidence(result.getPricingConfidence());
            pricingSuggestion.setReasoning(result.getPricingReasoning());
            pricingSuggestion.setStatus(SuggestionStatus.PENDING);
            pricingSuggestion.setTriggerReason(triggerReason);
            pricingSuggestionRepository.save(pricingSuggestion);
        }

        // Save Reorder Suggestion
        if (!hasPendingReorder) {
            ReorderSuggestion reorderSuggestion = new ReorderSuggestion();
            reorderSuggestion.setProduct(product);
            reorderSuggestion.setCurrentStock(product.getStockLevel());
            reorderSuggestion.setRecommendedQuantity(result.getRecommendedQuantity());
            reorderSuggestion.setSuggestedLeadTimeDays(7); // Default
            reorderSuggestion.setConfidence(result.getReorderConfidence());
            reorderSuggestion.setReasoning(result.getReorderReasoning());
            reorderSuggestion.setStatus(SuggestionStatus.PENDING);
            reorderSuggestion.setTriggerReason(triggerReason);
            reorderSuggestionRepository.save(reorderSuggestion);
        }
    }
}
