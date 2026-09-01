package com.stockpulse.controller;

import com.stockpulse.ai.StrategyResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final StrategyResolver strategyResolver;

    @Autowired
    public AdminController(StrategyResolver strategyResolver) {
        this.strategyResolver = strategyResolver;
    }

    @GetMapping("/strategy")
    public ResponseEntity<Map<String, String>> getStrategy() {
        return ResponseEntity.ok(Map.of("activeStrategy", strategyResolver.getActiveStrategy()));
    }

    @PostMapping("/strategy")
    public ResponseEntity<Map<String, String>> setStrategy(@RequestBody Map<String, String> payload) {
        String strategy = payload.get("strategy");
        if (strategy != null) {
            try {
                strategyResolver.setActiveStrategy(strategy);
                return ResponseEntity.ok(Map.of("activeStrategy", strategyResolver.getActiveStrategy()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
            }
        }
        return ResponseEntity.badRequest().build();
    }
}
