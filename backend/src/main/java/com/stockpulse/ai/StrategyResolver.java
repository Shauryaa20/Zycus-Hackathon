package com.stockpulse.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StrategyResolver {

    private final Map<String, CommerceAdvisor> advisors = new ConcurrentHashMap<>();
    
    private String activeStrategy;

    @Autowired
    public StrategyResolver(
            @Value("${commerce.strategy:RULE}") String initialStrategy,
            RuleBasedCommerceAdvisor ruleBasedAdvisor,
            AiCommerceAdvisor aiAdvisor) {
        
        this.activeStrategy = initialStrategy.toUpperCase();
        
        // Register strategies
        advisors.put("RULE", ruleBasedAdvisor);
        advisors.put("AI", aiAdvisor);
    }

    /**
     * Resolves the active strategy dynamically at runtime.
     */
    public CommerceAdvisor resolveActiveAdvisor() {
        CommerceAdvisor advisor = advisors.get(activeStrategy.toUpperCase());
        if (advisor == null) {
            // Default fallback if configuration is messed up
            return advisors.get("RULE");
        }
        return advisor;
    }

    /**
     * Update the active strategy at runtime.
     */
    public void setActiveStrategy(String strategy) {
        if (!advisors.containsKey(strategy.toUpperCase())) {
            throw new IllegalArgumentException("Unknown strategy: " + strategy);
        }
        this.activeStrategy = strategy.toUpperCase();
    }
    
    public String getActiveStrategy() {
        return activeStrategy;
    }
}
