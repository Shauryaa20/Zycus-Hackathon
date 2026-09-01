package com.stockpulse.events;

import com.stockpulse.model.Product;
import com.stockpulse.model.TriggerReason;
import org.springframework.context.ApplicationEvent;

public class ProductEvent extends ApplicationEvent {

    private final Product product;
    private final TriggerReason triggerReason;

    public ProductEvent(Object source, Product product, TriggerReason triggerReason) {
        super(source);
        this.product = product;
        this.triggerReason = triggerReason;
    }

    public Product getProduct() { return product; }
    public TriggerReason getTriggerReason() { return triggerReason; }
}
