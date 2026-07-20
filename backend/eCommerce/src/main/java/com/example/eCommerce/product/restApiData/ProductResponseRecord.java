package com.example.eCommerce.product.restApiData;

import com.example.eCommerce.product.entity.Product;
import com.example.eCommerce.product.enums.ProductStatus;

import java.math.BigDecimal;
import java.util.Objects;

public record ProductResponseRecord(Integer productId,
                                    String productName,
                                    String productDescription,
                                    BigDecimal price,
                                    String currency,
                                    Integer stockQuantity,
                                    ProductStatus status,
                                    String categoryName,
                                    String brandName,
                                    String imageUrl)
{
    public static ProductResponseRecord fromEntity(Product product)
    {
        return new ProductResponseRecord(
                product.getProductId(),
                product.getProductName(),
                product.getProductDescription(),
                product.getPrice(),
                product.getCurrency(),
                product.getStockQuantity(),
                product.getStatus(),
                Objects.nonNull(product.getCategory()) ? product.getCategory().getCategoryName() : null,
                Objects.nonNull(product.getBrand()) ? product.getBrand().getBrandName() : null,
                product.getImageUrl()
        );
    }
}
