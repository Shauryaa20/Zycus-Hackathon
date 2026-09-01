package com.stockpulse.events;

import com.stockpulse.ai.RecommendationOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class AgenticLoopListener {

    private final RecommendationOrchestrator orchestrator;

    @Autowired
    public AgenticLoopListener(RecommendationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Async
    @EventListener
    public void handleProductEvent(ProductEvent event) {
        // Delegate to orchestrator
        orchestrator.generateAndPersistRecommendations(event.getProduct(), event.getTriggerReason());
    }
}
