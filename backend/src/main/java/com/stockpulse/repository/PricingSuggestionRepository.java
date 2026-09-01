package com.stockpulse.repository;

import com.stockpulse.model.PricingSuggestion;
import com.stockpulse.model.SuggestionStatus;
import com.stockpulse.model.TriggerReason;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PricingSuggestionRepository extends JpaRepository<PricingSuggestion, Long> {
    List<PricingSuggestion> findByProductIdAndStatusAndTriggerReason(String productId, SuggestionStatus status, TriggerReason triggerReason);
    List<PricingSuggestion> findByStatus(SuggestionStatus status);
    List<PricingSuggestion> findByProductIdAndStatus(String productId, SuggestionStatus status);
}
