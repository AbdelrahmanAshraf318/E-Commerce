package com.example.eCommerce.product.repo;

import com.example.eCommerce.product.entity.Product;
import com.example.eCommerce.product.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepo extends JpaRepository<Product, Integer>
{
    // Fetch brand + category in the same query - otherwise one extra query per product (N+1).
    @EntityGraph(attributePaths = {"category", "brand"})
    Page<Product> findByStatusNotAndProductNameContainingIgnoreCase(ProductStatus excludedStatus, String name, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "brand"})
    Optional<Product> findByProductIdAndStatusNot(Integer productId, ProductStatus excludedStatus);
}
