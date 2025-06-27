package com.shopsphere.shopsphere_web.controller;

import com.shopsphere.shopsphere_web.dto.OrderDTO;
import com.shopsphere.shopsphere_web.service.OrderService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 주문 생성
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderDTO.CreateRequest request, HttpSession session) {
        try {
            String userId = (String) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
            }
            OrderDTO.Response response = orderService.createOrder(userId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "주문 처리 중 오류가 발생했습니다."));
        }
    }

    // 주문 상세 조회
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable Integer orderId, HttpSession session) {
        try {
            String userId = (String) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
            }
            
            OrderDTO.Response order = orderService.getOrder(orderId);
            if (order == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Check if the order belongs to the current user
            if (!order.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("message", "접근 권한이 없습니다."));
            }
            
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "주문 조회 중 오류가 발생했습니다."));
        }
    }

    // 사용자 주문 목록 조회
    @GetMapping
    public ResponseEntity<?> getUserOrders(HttpSession session) {
        try {
            String userId = (String) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
            }
            
            List<OrderDTO.Response> orders = orderService.getUserOrders(userId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "주문 목록 조회 중 오류가 발생했습니다."));
        }
    }

    // 주문 주소 변경
    @PutMapping("/{orderId}/address")
    public ResponseEntity<?> updateOrderAddress(
            @PathVariable Integer orderId,
            @RequestBody OrderDTO.UpdateAddressRequest request,
            HttpSession session) {
        try {
            String userId = (String) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
            }
            
            // 주문 소유자 확인
            OrderDTO.Response order = orderService.getOrder(orderId);
            if (order == null) {
                return ResponseEntity.notFound().build();
            }
            if (!order.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("message", "접근 권한이 없습니다."));
            }
            
            // 주소 업데이트
            OrderDTO.Response updatedOrder = orderService.updateOrderAddress(orderId, request.getShippingAddress());
            return ResponseEntity.ok(updatedOrder);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "주소 변경 중 오류가 발생했습니다."));
        }
    }
    

    
    // 주문 취소
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Integer orderId, HttpSession session) {
        try {
            String userId = (String) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
            }
            
            OrderDTO.Response order = orderService.getOrder(orderId);
            if (order == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Check if the order belongs to the current user
            if (!order.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("message", "접근 권한이 없습니다."));
            }
            
            OrderDTO.Response updatedOrder = orderService.updateOrderStatus(orderId, "CANCELLED");
            if (updatedOrder == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "주문 취소 중 오류가 발생했습니다."));
        }
    }


}
