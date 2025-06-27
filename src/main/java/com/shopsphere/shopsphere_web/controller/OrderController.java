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

    // 결제 요청 시작 (토스페이먼츠 결제창으로 리다이렉트 전)
    @PostMapping("/{orderId}/pay") // 또는 @PostMapping("/prepare-payment") 등으로 별도 엔드포인트 생성 가능
    public ResponseEntity<?> requestPayment(@PathVariable Integer orderId, HttpSession session) {
        try {
            String userId = (String) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
            }

            // 1. 주문 유효성 및 소유자 확인
            OrderDTO.Response order = orderService.getOrder(orderId);
            if (order == null) {
                return ResponseEntity.notFound().build();
            }
            if (!order.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("message", "접근 권한이 없습니다."));
            }
            // TODO: 주문 상태가 '결제 대기' 등 결제 가능한 상태인지 확인하는 로직 추가

            // 2. 토스페이먼츠 결제 요청 (Service Layer에서 처리)
            // 이 메서드는 토스페이먼츠에 결제 승인 요청을 보내고,
            // 결제창 URL과 paymentKey 등의 정보를 반환해야 합니다.
            Map<String, String> paymentInfo = orderService.requestTossPayment(orderId, userId); // 가상의 메서드
            return ResponseEntity.ok(paymentInfo); // 프론트엔드에 URL과 paymentKey를 전달

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "결제 요청 중 오류가 발생했습니다."));
        }
    }
    // 토스페이먼츠 결제 성공 콜백
    @GetMapping("/toss/success")
    public ResponseEntity<?> tossPaymentSuccess(
            @RequestParam String paymentKey,
            @RequestParam String orderId, // 이 orderId는 토스페이먼츠에서 받은 값이므로, 우리 시스템의 orderId와 매핑 필요
            @RequestParam Long amount) {
        try {
            // 1. 토스페이먼츠에 최종 결제 승인 요청 (Service Layer에서 처리)
            // 이때, amount 등도 함께 보내서 금액 위변조 여부 검증
            orderService.confirmTossPayment(paymentKey, orderId, amount); // 가상의 메서드

            // 2. 주문 상태 업데이트 및 결제 정보 저장
            orderService.updateOrderStatus(Integer.parseInt(orderId), "PAID"); // 주문 상태 '결제 완료'로 변경

            // 3. 클라이언트를 결제 성공 페이지로 리다이렉트 (프론트엔드 URL)
            // 실제 서비스에서는 리다이렉트 시킬 프론트엔드 URL을 반환하거나,
            // 아니면 프론트엔드에서 이 API를 호출하고 직접 성공 페이지로 이동
            return ResponseEntity.ok(Map.of("message", "결제가 성공적으로 완료되었습니다.", "orderId", orderId));

        } catch (IllegalArgumentException e) {
            // 결제 실패 처리 (예: 결제 정보 불일치, 위변조 의심 등)
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "결제 성공 처리 중 오류가 발생했습니다."));
        }
    }

    // 토스페이먼츠 결제 실패 콜백
    @GetMapping("/toss/fail")
    public ResponseEntity<?> tossPaymentFail(
            @RequestParam String code,
            @RequestParam String message,
            @RequestParam String orderId) {
        try {
            // 주문 상태를 '결제 실패'로 업데이트하거나, 사용자에게 실패 메시지 전달
            orderService.updateOrderStatus(Integer.parseInt(orderId), "PAYMENT_FAILED"); // 가상의 상태

            // 클라이언트를 결제 실패 페이지로 리다이렉트 (프론트엔드 URL)
            return ResponseEntity.badRequest().body(Map.of("message", "결제에 실패했습니다.", "code", code, "description", message, "orderId", orderId));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "결제 실패 처리 중 오류가 발생했습니다."));
        }
    }

}
