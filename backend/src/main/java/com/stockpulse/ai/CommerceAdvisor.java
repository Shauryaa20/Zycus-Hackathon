package com.stockpulse.ai;

import com.stockpulse.model.Product;
import com.stockpulse.model.TriggerReason;

public interface CommerceAdvisor {
    AdvisorResult generateRecommendations(Product product, TriggerReason triggerReason);
}
