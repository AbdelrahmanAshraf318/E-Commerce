package com.example.eCommerce.product.entity;

import com.example.eCommerce.product.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PRODUCT")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class Product
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PRODUCT_ID")
    @EqualsAndHashCode.Include
    private Integer productId;

    @NonNull
    @Column(name = "PRODUCT_NAME", nullable = false, length = 255)
    private String productName;

    @Column(name = "PRODUCT_DESCRIPTION", columnDefinition = "TEXT")
    private String productDescription;

    @Column(name = "PRICE", precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "CURRENCY", length = 3)
    private String currency; // e.g. "USD"

    @Column(name = "STOCK_QUANTITY", nullable = false)
    private Integer stockQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    private ProductStatus status; // ACTIVE, INACTIVE, OUT_OF_STOCK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CATEGORY_ID")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BRAND_ID")
    private Brand brand;

    @Column(name = "WEIGHT")
    private Double weight; // in kg

    @Column(name = "IMAGE_URL", length = 500)
    private String imageUrl;


    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
}
