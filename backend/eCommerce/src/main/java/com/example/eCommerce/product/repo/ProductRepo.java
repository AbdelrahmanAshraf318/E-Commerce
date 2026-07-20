package com.example.eCommerce.product.repo;

import com.example.eCommerce.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin(value = "http:localhost:4200")
@Repository
public interface ProductRepo extends JpaRepository<Product, Integer>
{
}
