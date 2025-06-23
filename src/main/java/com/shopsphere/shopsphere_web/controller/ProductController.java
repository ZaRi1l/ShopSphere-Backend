package com.shopsphere.shopsphere_web.controller;

import com.shopsphere.shopsphere_web.dto.ProductDTO;
import com.shopsphere.shopsphere_web.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 특정 상품의 상세 정보를 조회합니다.
     *
     * @param productId 조회할 상품 ID
     * @return 조회된 상품 정보 (ProductDTO.Response)와 200 OK 상태 코드,
     *         상품이 없는 경우 404 Not Found
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ProductDTO.Response> getProductById(@PathVariable Integer productId) {
        ProductDTO.Response product = productService.getProduct(productId);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(product);
    }

    /**
     * 상품 목록을 조회합니다. 카테고리, 가격 범위, 정렬 옵션으로 필터링 가능합니다.
     *
     * @param categoryId 카테고리 ID (선택 사항)
     * @param minPrice   최소 가격 (선택 사항, 예: 10000)
     * @param maxPrice   최대 가격 (선택 사항, 예: 50000)
     * @param sortOption 정렬 옵션 (선택 사항, 기본값 "musinsa_recommend",
     *                   다른 값: "sales_volume_desc", "created_at_desc", "price_asc", "price_desc")
     * @return 조회된 상품 목록과 200 OK 상태 코드
     */
    @GetMapping
    public ResponseEntity<List<ProductDTO.Response>> getProducts(
            @RequestParam(name = "categoryId", required = false) Integer categoryId,
            @RequestParam(name = "minPrice", required = false) Integer minPrice,
            @RequestParam(name = "maxPrice", required = false) Integer maxPrice,
            @RequestParam(name = "sort", defaultValue = "musinsa_recommend") String sortOption) {
        
        List<ProductDTO.Response> products = productService.getProducts(categoryId, minPrice, maxPrice, sortOption);
        return ResponseEntity.ok(products);
    }

    /**
     * 새로운 상품을 생성합니다. (로그인 필요)
     *
     * @param session 현재 HTTP 세션
     * @param request 생성할 상품 정보 (ProductDTO.CreateRequest)
     * @return 생성된 상품 정보 (ProductDTO.Response)와 201 Created 상태 코드,
     *         로그인 필요 시 401, 오류 발생 시 500
     */
    @PostMapping
    public ResponseEntity<?> createProduct(
            HttpSession session,
            @RequestBody ProductDTO.CreateRequest request) {
        try {
            String userId = (String) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
            }
            ProductDTO.Response response = productService.createProduct(userId, request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            // 운영 환경에서는 상세 오류 메시지 노출 주의
            e.printStackTrace(); 
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("message", "상품 등록 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 현재 로그인한 판매자가 등록한 모든 상품 목록을 조회합니다. (로그인 필요)
     *
     * @param session 현재 HTTP 세션
     * @return 판매자의 상품 목록 (List<ProductDTO.Response>)과 200 OK 상태 코드,
     *         로그인 필요 시 401
     */
    @GetMapping("/seller/me")
    public ResponseEntity<?> getMyProducts(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
        }
        List<ProductDTO.Response> products = productService.getProductsBySeller(userId);
        return ResponseEntity.ok(products);
    }

    /**
     * 기존 상품 정보를 수정합니다. (로그인 및 상품 소유자 확인 필요)
     *
     * @param productId 수정할 상품 ID
     * @param request   수정할 상품 정보 (ProductDTO.UpdateRequest)
     * @param session   현재 HTTP 세션
     * @return 수정된 상품 정보 (ProductDTO.Response)와 200 OK 상태 코드,
     *         로그인 필요 시 401, 권한 없을 시 403, 상품 없을 시 404, 오류 발생 시 500
     */
    @PutMapping("/{productId}")
    public ResponseEntity<?> updateProduct(
            @PathVariable Integer productId,
            @RequestBody ProductDTO.UpdateRequest request,
            HttpSession session) {
        try {
            String userId = (String) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
            }
            ProductDTO.Response response = productService.updateProduct(userId, productId, request);
            if (response == null) { // Service에서 상품 못 찾으면 null 반환하거나 예외 발생하도록 통일 필요
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) { // 상품 또는 카테고리 못 찾은 경우
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) { // 권한 없는 경우
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("message", "상품 수정 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 특정 상품을 삭제합니다. (로그인 및 상품 소유자 확인 필요)
     *
     * @param productId 삭제할 상품 ID
     * @param session   현재 HTTP 세션
     * @return 성공 메시지와 200 OK 상태 코드,
     *         로그인 필요 시 401, 권한 없을 시 403, 상품 없을 시 404, 오류 발생 시 500
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<?> deleteProduct(
            @PathVariable Integer productId,
            HttpSession session) {
        try {
            String userId = (String) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
            }
            productService.deleteProduct(userId, productId);
            return ResponseEntity.ok(Map.of("message", "상품이 성공적으로 삭제되었습니다."));
        } catch (IllegalArgumentException e) { // 상품 못 찾은 경우
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) { // 권한 없는 경우
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("message", "상품 삭제 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 상품명 또는 카테고리명으로 상품을 검색합니다.
     *
     * @param keyword    검색어
     * @param sortOption 정렬 옵션 (선택 사항, 기본값 "musinsa_recommend")
     * @return 검색된 상품 목록 (List<ProductDTO.Response>)
     */
    @GetMapping("/search")
    public ResponseEntity<List<ProductDTO.Response>> searchProducts(
            @RequestParam(name = "keyword") String keyword, // 키워드는 필수로 가정
            @RequestParam(name = "sort", defaultValue = "musinsa_recommend") String sortOption) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList()); // 빈 키워드는 빈 목록 반환
        }
        List<ProductDTO.Response> products = productService.searchProductsByKeyword(keyword.trim(), sortOption);
        return ResponseEntity.ok(products);
    }
}