package com.shopsphere.shopsphere_web.controller;

import com.shopsphere.shopsphere_web.dto.ProductDTO;
import com.shopsphere.shopsphere_web.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 새로운 상품을 생성합니다.
     *
     * @param session 현재 HTTP 세션
     * @param request 생성할 상품 정보 (ProductDTO.CreateRequest)
     * @return 생성된 상품 정보 (ProductDTO.Response)와 201 Created 상태 코드
     */
    @PostMapping()
    public ResponseEntity<?> createProduct(
            HttpSession session,
            @RequestBody ProductDTO.CreateRequest request) {
        try {
            // 세션에서 사용자 ID 가져오기
            String userId = (String) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
            }

            // 서비스 레이어에 사용자 ID와 함께 상품 생성 요청
            ProductDTO.Response response = productService.createProduct(userId, request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace(); // 서버 로그에 스택 트레이스 출력
            String errorMessage = "상품 등록 중 오류가 발생했습니다: " + e.getMessage();
            if (e.getCause() != null) {
                errorMessage += " (원인: " + e.getCause().getMessage() + ")";
            }
            return ResponseEntity.status(500).body(Map.of(
                "message", errorMessage,
                "error", e.getClass().getName(),
                "details", e.getMessage()
            ));
        }
    }

    /**
     * 특정 상품의 상세 정보를 조회합니다.
     *
     * @param productId 조회할 상품 ID
     * @return 조회된 상품 정보 (ProductDTO.Response)와 200 OK 상태 코드,
     *         상품이 없는 경우 404 Not Found
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ProductDTO.Response> getProduct(@PathVariable Integer productId) {
        ProductDTO.Response response = productService.getProduct(productId);
        if (response != null) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 특정 카테고리에 속한 모든 상품 목록을 조회합니다.
     *
     * @param categoryId 조회할 카테고리 ID
     * @return 해당 카테고리의 상품 목록 (List<ProductDTO.Response>)과 200 OK 상태 코드
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductDTO.Response>> getProductsByCategory(@PathVariable Integer categoryId) {
        List<ProductDTO.Response> products = productService.getProductsByCategory(categoryId);
        return ResponseEntity.ok(products);
    }

    /**
     * 현재 로그인한 판매자가 등록한 모든 상품 목록을 조회합니다.
     *
     * @param session 현재 HTTP 세션
     * @return 판매자의 상품 목록 (List<ProductDTO.Response>)과 200 OK 상태 코드
     */
    @GetMapping("/seller/me")
    public ResponseEntity<List<ProductDTO.Response>> getMyProducts(HttpSession session) {
        List<ProductDTO.Response> products = productService.getProductsBySeller(session.getAttribute("userId").toString());
        return ResponseEntity.ok(products);
    }

    /**
     * 기존 상품 정보를 수정합니다.
     *
     * @param productId 수정할 상품 ID
     * @param request   수정할 상품 정보 (ProductDTO.UpdateRequest)
     * @return 수정된 상품 정보 (ProductDTO.Response)와 200 OK 상태 코드,
     *         상품이 없는 경우 404 Not Found
     */
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

    /**
     * 특정 상품을 삭제합니다.
     *
     * @param productId 삭제할 상품 ID
     * @return 204 No Content 상태 코드 (성공 시)
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Integer productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }
}
