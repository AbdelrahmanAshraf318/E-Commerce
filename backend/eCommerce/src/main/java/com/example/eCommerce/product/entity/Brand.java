package com.example.eCommerce.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Builder
@Setter
@Getter
@AllArgsConstructor
@Entity
@Table(name = "BRAND")
public class Brand
{
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "BRAND_ID")
    private Integer brandId;

    @Column(name = "BRAND_NAME", nullable = false, unique = true)
    private String brandName;

    @Column(name = "LOGO_URL", length = 500)
    private String logoUrl;

    @OneToMany(mappedBy = "brand")
    @Builder.Default
    private List<Product> products = new ArrayList<>();
}
