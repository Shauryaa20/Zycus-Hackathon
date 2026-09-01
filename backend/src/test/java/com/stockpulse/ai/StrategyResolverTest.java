package com.stockpulse.ai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class StrategyResolverTest {

    @Autowired
    private StrategyResolver strategyResolver;

    @Autowired
    private RuleBasedCommerceAdvisor ruleAdvisor;

    @Autowired
    private AiCommerceAdvisor aiAdvisor;

    @Test
    public void testStrategySwitchingAtRuntime() {
        // Set to RULE
        strategyResolver.setActiveStrategy("RULE");
        CommerceAdvisor current = strategyResolver.resolveActiveAdvisor();
        assertTrue(current instanceof RuleBasedCommerceAdvisor, "Should resolve to RuleBasedCommerceAdvisor");

        // Switch to AI at runtime
        strategyResolver.setActiveStrategy("AI");
        current = strategyResolver.resolveActiveAdvisor();
        assertTrue(current instanceof AiCommerceAdvisor, "Should resolve to AiCommerceAdvisor");
        
        // Assert state changed
        assertEquals("AI", strategyResolver.getActiveStrategy());
    }
}
