package com.example.eCommerce.product.controller;

import com.example.eCommerce.common.dto.PageResponse;
import com.example.eCommerce.product.entity.ProductImage;
import com.example.eCommerce.product.restApiData.ProductResponseRecord;
import com.example.eCommerce.product.service.ProductService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.WebRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Public product catalogue")
public class ProductController
{
    private final ProductService productService;

    @GetMapping
    public PageResponse<ProductResponseRecord> getProducts(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 12, sort = "productName", direction = Sort.Direction.ASC) Pageable pageable)
    {
        return productService.getProducts(search, pageable);
    }

    @GetMapping("/{productId}")
    public ProductResponseRecord getProduct(@PathVariable Integer productId)
    {
        return productService.getProduct(productId);
    }

    /**
     * The ETag is derived from the bytes, so replacing an image changes it. Browsers reuse their copy for 5 minutes,
     * then revalidate with If-None-Match and get a body-less 304 while the image is unchanged.
     * (A long max-age without an ETag kept showing replaced images for up to an hour.)
     */
    @GetMapping("/{productId}/image")
    public ResponseEntity<byte[]> getProductImage(@PathVariable Integer productId, WebRequest webRequest)
    {
        ProductImage image = productService.getProductImage(productId);
        String etag = "\"" + DigestUtils.md5DigestAsHex(image.getImageData()) + "\"";
        CacheControl cacheControl = CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic();

        if (webRequest.checkNotModified(etag))
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).cacheControl(cacheControl).build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .eTag(etag)
                .cacheControl(cacheControl)
                .body(image.getImageData());
    }
}
