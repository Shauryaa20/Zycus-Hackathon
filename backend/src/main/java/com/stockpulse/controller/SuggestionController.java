package com.stockpulse.controller;

import com.stockpulse.model.PricingSuggestion;
import com.stockpulse.model.Product;
import com.stockpulse.model.ProductStatus;
import com.stockpulse.model.ReorderSuggestion;
import com.stockpulse.model.SuggestionStatus;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SuggestionController {

    private final PricingSuggestionRepository pricingRepository;
    private final ReorderSuggestionRepository reorderRepository;
    private final ProductRepository productRepository;

    @Autowired
    public SuggestionController(PricingSuggestionRepository pricingRepository,
                                ReorderSuggestionRepository reorderRepository,
                                ProductRepository productRepository) {
        this.pricingRepository = pricingRepository;
        this.reorderRepository = reorderRepository;
        this.productRepository = productRepository;
    }

    @GetMapping("/pricing-suggestions/pending")
    public List<PricingSuggestion> getPendingPricing() {
        return pricingRepository.findByStatus(SuggestionStatus.PENDING);
    }

    @GetMapping("/reorder-suggestions/pending")
    public List<ReorderSuggestion> getPendingReorder() {
        return reorderRepository.findByStatus(SuggestionStatus.PENDING);
    }

    @PatchMapping("/pricing-suggestions/{id}")
    @Transactional
    public ResponseEntity<PricingSuggestion> updatePricingSuggestion(
            @PathVariable Long id, @RequestBody Map<String, String> payload) {
        
        PricingSuggestion suggestion = pricingRepository.findById(id).orElseThrow();
        
        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            return ResponseEntity.badRequest().build();
        }

        SuggestionStatus newStatus = SuggestionStatus.valueOf(payload.get("status").toUpperCase());
        suggestion.setStatus(newStatus);
        
        if (newStatus == SuggestionStatus.ACCEPTED) {
            Product product = suggestion.getProduct();
            product.setCurrentPrice(suggestion.getRecommendedPrice());
            checkAndResetProductStatus(product);
            productRepository.save(product);
        } else if (newStatus == SuggestionStatus.REJECTED) {
            Product product = suggestion.getProduct();
            checkAndResetProductStatus(product);
            productRepository.save(product);
        }
        
        return ResponseEntity.ok(pricingRepository.save(suggestion));
    }

    @PatchMapping("/reorder-suggestions/{id}")
    @Transactional
    public ResponseEntity<ReorderSuggestion> updateReorderSuggestion(
            @PathVariable Long id, @RequestBody Map<String, String> payload) {
        
        ReorderSuggestion suggestion = reorderRepository.findById(id).orElseThrow();
        
        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            return ResponseEntity.badRequest().build();
        }

        SuggestionStatus newStatus = SuggestionStatus.valueOf(payload.get("status").toUpperCase());
        suggestion.setStatus(newStatus);
        
        if (newStatus == SuggestionStatus.ACCEPTED) {
            Product product = suggestion.getProduct();
            product.setStockLevel(product.getStockLevel() + suggestion.getRecommendedQuantity());
            if (product.getStatus() == ProductStatus.OUT_OF_STOCK) {
                checkAndResetProductStatus(product);
            }
            productRepository.save(product);
        } else if (newStatus == SuggestionStatus.REJECTED) {
            Product product = suggestion.getProduct();
            checkAndResetProductStatus(product);
            productRepository.save(product);
        }
        
        return ResponseEntity.ok(reorderRepository.save(suggestion));
    }
    
    private void checkAndResetProductStatus(Product product) {
        // If there are no other pending suggestions, revert status to ACTIVE
        boolean hasPendingPricing = !pricingRepository.findByProductIdAndStatus(product.getId(), SuggestionStatus.PENDING).isEmpty();
        boolean hasPendingReorder = !reorderRepository.findByProductIdAndStatus(product.getId(), SuggestionStatus.PENDING).isEmpty();
        
        if (!hasPendingPricing && !hasPendingReorder && product.getStockLevel() > 0) {
            product.setStatus(ProductStatus.ACTIVE);
        }
    }
}
