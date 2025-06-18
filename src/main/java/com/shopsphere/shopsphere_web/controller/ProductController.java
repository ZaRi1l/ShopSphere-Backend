package com.shopsphere.shopsphere_web.controller;

import com.shopsphere.shopsphere_web.dto.ProductDTO;
import com.shopsphere.shopsphere_web.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductDTO.Response> createProduct(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ProductDTO.CreateRequest request) {
        ProductDTO.Response response = productService.createProduct(userDetails.getUsername(), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDTO.Response> getProduct(@PathVariable Integer productId) {
        ProductDTO.Response response = productService.getProduct(productId);
        if (response != null) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductDTO.Response>> getProductsByCategory(@PathVariable Integer categoryId) {
        List<ProductDTO.Response> products = productService.getProductsByCategory(categoryId);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/seller/me")
    public ResponseEntity<List<ProductDTO.Response>> getMyProducts(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ProductDTO.Response> products = productService.getProductsBySeller(userDetails.getUsername());
        return ResponseEntity.ok(products);
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ProductDTO.Response> updateProduct(
            @PathVariable Integer productId,
            @RequestBody ProductDTO.UpdateRequest request) {
        ProductDTO.Response response = productService.updateProduct(productId, request);
        if (response != null) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Integer productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }
}
