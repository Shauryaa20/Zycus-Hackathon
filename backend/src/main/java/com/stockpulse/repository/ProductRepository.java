package com.stockpulse.repository;

import com.stockpulse.model.Product;
import com.stockpulse.model.ProductStatus;
import com.stockpulse.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByStatus(ProductStatus status);
    List<Product> findByCategory(Category category);
    List<Product> findByStatusAndCategory(ProductStatus status, Category category);
}
