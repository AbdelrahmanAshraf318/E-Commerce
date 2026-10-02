package com.example.eCommerce.product.repo;

import com.example.eCommerce.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductCategoryRepo extends JpaRepository<Category, Integer>
{
}
