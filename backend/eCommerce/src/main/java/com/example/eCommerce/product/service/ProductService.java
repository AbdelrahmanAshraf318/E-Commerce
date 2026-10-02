package com.example.eCommerce.product.service;

import com.example.eCommerce.common.dto.PageResponse;
import com.example.eCommerce.common.enums.ErrorCode;
import com.example.eCommerce.exception.BusinessException;
import com.example.eCommerce.product.entity.Product;
import com.example.eCommerce.product.entity.ProductImage;
import com.example.eCommerce.product.enums.ProductStatus;
import com.example.eCommerce.product.repo.ProductImageRepo;
import com.example.eCommerce.product.repo.ProductRepo;
import com.example.eCommerce.product.restApiData.ProductResponseRecord;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public, read-only catalogue. IN_ACTIVE products are hidden from customers.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService
{
    private final ProductRepo productRepo;
    private final ProductImageRepo productImageRepo;

    public PageResponse<ProductResponseRecord> getProducts(String search, Pageable pageable)
    {
        return PageResponse.from(
                productRepo.findByStatusNotAndProductNameContainingIgnoreCase(
                        ProductStatus.IN_ACTIVE, StringUtils.trimToEmpty(search), pageable),
                ProductResponseRecord::fromEntity);
    }

    public ProductResponseRecord getProduct(Integer productId)
    {
        return productRepo.findByProductIdAndStatusNot(productId, ProductStatus.IN_ACTIVE)
                .map(ProductResponseRecord::fromEntity)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, productId));
    }

    /**
     * For other modules (cart, orders) that need the entity. IN_ACTIVE products are treated as non-existent,
     * exactly like on the public catalogue. Stock is NOT checked here - that rule belongs to the caller.
     */
    public Product getVisibleProduct(Integer productId)
    {
        return productRepo.findById(productId)
                .filter(product -> product.getStatus() != ProductStatus.IN_ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, productId));
    }

    public ProductImage getProductImage(Integer productId)
    {
        return productImageRepo.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_IMAGE_NOT_FOUND, productId));
    }
}
