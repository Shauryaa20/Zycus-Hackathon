package com.stockpulse.repository;

import com.stockpulse.model.ReorderSuggestion;
import com.stockpulse.model.SuggestionStatus;
import com.stockpulse.model.TriggerReason;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReorderSuggestionRepository extends JpaRepository<ReorderSuggestion, Long> {
    List<ReorderSuggestion> findByProductIdAndStatusAndTriggerReason(String productId, SuggestionStatus status, TriggerReason triggerReason);
    List<ReorderSuggestion> findByStatus(SuggestionStatus status);
    List<ReorderSuggestion> findByProductIdAndStatus(String productId, SuggestionStatus status);
}
