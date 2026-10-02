package com.example.eCommerce.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "PRODUCT_IMAGE")
@Getter
@Setter
@NoArgsConstructor
public class ProductImage {

    @Id
    @Column(name = "PRODUCT_ID")
    private Integer productId;

    // Plain byte[] maps to PostgreSQL BYTEA: stored in the row and deleted with it.
    // (@Lob mapped it to an OID large object, whose bytes stay behind in pg_largeobject when the row is deleted.)
    @Column(name = "IMAGE_DATA", nullable = false)
    private byte[] imageData;

    @Column(name = "CONTENT_TYPE", length = 100, nullable = false)
    private String contentType;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "PRODUCT_ID")
    private Product product;
}