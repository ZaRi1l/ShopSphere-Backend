package com.shopsphere.shopsphere_web.controller;

import com.shopsphere.shopsphere_web.dto.ProductDTO;
import com.shopsphere.shopsphere_web.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.List; // getAllProducts 등에서 List를 반환할 경우 필요

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 상품 목록을 필터링, 정렬, 페이징하여 조회합니다.
     * 프론트엔드 Middle.js에서 이 API를 호출하도록 합니다.
     *
     * @param categoryId 카테고리 ID (선택 사항)
     * @param minPrice 최소 가격 (선택 사항)
     * @param maxPrice 최대 가격 (선택 사항)
     * @param sortOption 정렬 옵션 (기본값: "musinsa_recommend")
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지 당 상품 수 (기본값: 5 또는 프론트엔드가 필요한 개수)
     * @return 페이징 처리된 상품 목록 (Page<ProductDTO.Response>)
     */
    @GetMapping
    public ResponseEntity<Page<ProductDTO.Response>> getProducts(
            @RequestParam(name = "categoryId", required = false) Integer categoryId,
            @RequestParam(name = "minPrice", required = false) Integer minPrice,
            @RequestParam(name = "maxPrice", required = false) Integer maxPrice,
            @RequestParam(name = "sort", defaultValue = "musinsa_recommend") String sortOption,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDTO.Response> productsPage = productService.findProductsWithFiltersAndSort(
                categoryId, minPrice, maxPrice, sortOption, pageable
        );
        return ResponseEntity.ok(productsPage);
    }

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
            String userId = (String) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
            }
            ProductDTO.Response response = productService.createProduct(userId, request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "message", "상품 등록 중 오류가 발생했습니다: " + e.getMessage(),
                "error", e.getClass().getName()
            ));
        }
    }

    /**
     * 특정 카테고리에 속한 모든 상품 목록을 조회합니다. (페이징 미적용, 필요시 getProducts로 통합 고려)
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
     * 현재 로그인한 판매자가 등록한 모든 상품 목록을 조회합니다. (페이징 미적용, 필요시 getProducts로 통합 고려)
     *
     * @param session 현재 HTTP 세션
     * @return 판매자의 상품 목록 (List<ProductDTO.Response>)과 200 OK 상태 코드
     */
    @GetMapping("/seller/me")
    public ResponseEntity<List<ProductDTO.Response>> getMyProducts(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(List.of());
        }
        List<ProductDTO.Response> products = productService.getProductsBySeller(userId);
        return ResponseEntity.ok(products);
    }

    /**
     * 기존 상품 정보를 수정합니다.
     *
     * @param productId 수정할 상품 ID
     * @param request   수정할 상품 정보 (ProductDTO.UpdateRequest)
     * @param session   현재 HTTP 세션
     * @return 수정된 상품 정보 (ProductDTO.Response)와 200 OK 상태 코드
     */
    @PutMapping("/{productId}")
    public ResponseEntity<?> updateProduct(
            @PathVariable Integer productId,
            @RequestBody ProductDTO.UpdateRequest request,
            HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
        }
        try {
            ProductDTO.Response response = productService.updateProduct(userId, productId, request);
            if (response != null) {
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.notFound().build(); // 상품이 없는 경우
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 특정 상품을 삭제합니다.
     *
     * @param productId 삭제할 상품 ID
     * @param session   현재 HTTP 세션
     * @return 성공 메시지와 200 OK 상태 코드
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<?> deleteProduct(
            @PathVariable Integer productId,
            HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
        }
        try {
            productService.deleteProduct(userId, productId);
            return ResponseEntity.ok().body(Map.of("message", "상품이 성공적으로 삭제되었습니다."));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) { // ProductService에서 상품 못찾을 시 발생 가능
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 상품명 또는 카테고리명으로 상품을 검색합니다. (페이징 적용 안됨, 필요시 getProducts로 통합)
     * @param keyword 검색어
     * @param sortOption 정렬 옵션
     * @return 검색된 상품 목록
     */
    @GetMapping("/search")
    public ResponseEntity<List<ProductDTO.Response>> searchProducts(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "sort", defaultValue = "musinsa_recommend") String sortOption) {
        if (keyword == null || keyword.trim().isEmpty()) {
            // 키워드가 없으면 빈 리스트 대신 getProducts를 호출하도록 유도하거나,
            // 별도의 "추천 상품" 로직을 태울 수 있음. 여기서는 빈 리스트 반환.
            return ResponseEntity.ok(List.of());
        }
        List<ProductDTO.Response> products = productService.searchProductsByKeyword(keyword.trim(), sortOption);
        return ResponseEntity.ok(products);
    }
}