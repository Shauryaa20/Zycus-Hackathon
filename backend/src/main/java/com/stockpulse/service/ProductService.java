package com.stockpulse.service;

import com.stockpulse.events.ProductEvent;
import com.stockpulse.model.Category;
import com.stockpulse.model.Product;
import com.stockpulse.model.ProductStatus;
import com.stockpulse.model.TriggerReason;
import com.stockpulse.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public ProductService(ProductRepository productRepository, ApplicationEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    public List<Product> getProducts(ProductStatus status, Category category) {
        if (status != null && category != null) {
            return productRepository.findByStatusAndCategory(status, category);
        } else if (status != null) {
            return productRepository.findByStatus(status);
        } else if (category != null) {
            return productRepository.findByCategory(category);
        }
        return productRepository.findAll();
    }

    @Transactional
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    public Product updateStock(String id, int newStock) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        
        product.setStockLevel(newStock);
        
        if (newStock == 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        } else if (product.getStatus() == ProductStatus.OUT_OF_STOCK && newStock > 0) {
            product.setStatus(ProductStatus.ACTIVE);
        }

        Product saved = productRepository.save(product);
        
        // Trigger agentic loop if below threshold
        if (saved.getStockLevel() < saved.getReorderThreshold()) {
            eventPublisher.publishEvent(new ProductEvent(this, saved, TriggerReason.INVENTORY_LOW));
        }

        return saved;
    }

    @Transactional
    public Product simulateSale(String id, int quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        
        product.setStockLevel(Math.max(0, product.getStockLevel() - quantity));
        product.setDemandVelocity(product.getDemandVelocity() + quantity);

        if (product.getStockLevel() == 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        }

        Product saved = productRepository.save(product);

        // Check Triggers
        boolean inventoryLow = saved.getStockLevel() < saved.getReorderThreshold();
        
        // Define category average dynamically or hardcoded for hackathon
        int categoryAverage = getCategoryAverageVelocity(saved.getCategory());
        boolean demandSpike = saved.getDemandVelocity() > (categoryAverage * 3);

        if (demandSpike) {
            eventPublisher.publishEvent(new ProductEvent(this, saved, TriggerReason.DEMAND_SPIKE));
        } else if (inventoryLow) {
            eventPublisher.publishEvent(new ProductEvent(this, saved, TriggerReason.INVENTORY_LOW));
        }

        return saved;
    }

    public void triggerManualPricing(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        eventPublisher.publishEvent(new ProductEvent(this, product, TriggerReason.MANUAL));
    }

    private int getCategoryAverageVelocity(Category category) {
        switch (category) {
            case ELECTRONICS: return 4;
            case APPAREL: return 6;
            case HOME: return 3;
            default: return 5;
        }
    }
}
