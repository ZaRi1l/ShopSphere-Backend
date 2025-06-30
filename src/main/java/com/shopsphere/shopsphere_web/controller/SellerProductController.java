package com.shopsphere.shopsphere_web.controller;

import com.shopsphere.shopsphere_web.dto.ProductDTO;
import com.shopsphere.shopsphere_web.dto.SalesStatisticsDTO;
import com.shopsphere.shopsphere_web.service.ProductService;
import com.shopsphere.shopsphere_web.service.SalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
public class SellerProductController {

    private final ProductService productService;
    private final SalesService salesService;

    /**
     * 현재 로그인한 판매자가 등록한 모든 상품 목록을 조회합니다. (페이징 미적용, 필요시 getProducts로 통합 고려)
     *
     * @param session 현재 HTTP 세션
     * @return 판매자의 상품 목록 (List<ProductDTO.Response>)과 200 OK 상태 코드
     */
    @GetMapping("/sales/statistics")
    public ResponseEntity<?> getSalesStatistics(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String product,
            @RequestParam(required = false, defaultValue = "daily") String timeRange,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpSession session) {

        System.out.println("Received request - category: " + category + ", product: " + product +
                ", timeRange: " + timeRange + ", startDate: " + startDate + ", endDate: " + endDate);

        String userId = (String) session.getAttribute("userId");
        System.out.println("Session userId: " + userId);

        if (userId == null) {
            System.out.println("Unauthorized access attempt");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        try {
            SalesStatisticsDTO.Response response = salesService.getSalesStatistics(
                    userId, category, product, timeRange, startDate, endDate);
            System.out.println("Successfully retrieved sales statistics");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error in getSalesStatistics: ");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("판매 통계를 가져오는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

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
     * @param session    현재 HTTP 세션
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
