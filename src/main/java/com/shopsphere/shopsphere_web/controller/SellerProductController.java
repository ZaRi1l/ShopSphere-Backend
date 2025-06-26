package com.shopsphere.shopsphere_web.controller;

import com.shopsphere.shopsphere_web.dto.ProductDTO;
import com.shopsphere.shopsphere_web.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
public class SellerProductController {

    private final ProductService productService;

    /**
     * 현재 로그인한 판매자가 등록한 모든 상품 목록을 조회합니다. (페이징 미적용, 필요시 getProducts로 통합 고려)
     *
     * @param session 현재 HTTP 세션
     * @return 판매자의 상품 목록 (List<ProductDTO.Response>)과 200 OK 상태 코드
     */
    @GetMapping("/products")
    public ResponseEntity<List<ProductDTO.Response>> getMyProducts(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(List.of());
        }
        List<ProductDTO.Response> products = productService.getProductsBySeller(userId);
        return ResponseEntity.ok(products);
    }

    /**
     * 현재 로그인한 판매자가 등록한 특정 카테고리의 상품 목록을 조회합니다.
     *
     * @param categoryId 조회할 카테고리 ID
     * @param session 현재 HTTP 세션
     * @return 해당 카테고리의 판매자 상품 목록 (List<ProductDTO.Response>)과 200 OK 상태 코드
     */
    @GetMapping("/products/category/{categoryId}")
    public ResponseEntity<List<ProductDTO.Response>> getMyProductsByCategory(
            @PathVariable Integer categoryId, 
            HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(List.of());
        }
        
        List<ProductDTO.Response> sellerProducts = productService.getProductsBySeller(userId);
        List<ProductDTO.Response> filteredProducts = sellerProducts.stream()
                .filter(product -> product.getCategory() != null && categoryId.equals(product.getCategory().getId()))
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(filteredProducts);
    }
}
