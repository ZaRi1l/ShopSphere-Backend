package com.shopsphere.shopsphere_web.controller;

import com.shopsphere.shopsphere_web.dto.CartDTO;
import com.shopsphere.shopsphere_web.dto.CartItemDTO;
import com.shopsphere.shopsphere_web.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // 장바구니 조회
    @GetMapping
    public ResponseEntity<?> getCart(HttpSession session) {
        try {
            String userId = (String) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
            }
            
            CartDTO.Response cart = cartService.getOrCreateCart(userId);
            return ResponseEntity.ok(cart);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "장바구니 조회 중 오류가 발생했습니다."));
        }
    }

    // 상품 장바구니 추가
    @PostMapping("/items")
    public ResponseEntity<?> addItem(@RequestBody CartItemDTO.AddRequest request, HttpSession session) {
        try {
            String userId = (String) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
            }
            
            CartDTO.Response cart = cartService.addItem(userId, request);
            return ResponseEntity.ok(cart);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "상품 추가 중 오류가 발생했습니다."));
        }
    }

    // 상품 수량 수정
    @PutMapping("/items/{itemId}")
    public ResponseEntity<?> updateItemQuantity(
            @PathVariable Integer itemId,
            @RequestBody CartItemDTO.UpdateRequest request,
            HttpSession session) {
        try {
            String userId = (String) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
            }
            
            CartDTO.Response cart = cartService.updateItemQuantity(userId, itemId, request);
            return ResponseEntity.ok(cart);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "상품 수량 수정 중 오류가 발생했습니다."));
        }
    }

    // 상품 삭제
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> removeItem(@PathVariable Integer itemId, HttpSession session) {
        try {
            String userId = (String) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
            }
            
            CartDTO.Response cart = cartService.removeItem(userId, itemId);
            return ResponseEntity.ok(cart);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "상품 삭제 중 오류가 발생했습니다."));
        }
    }

    // 장바구니 비우기
    @DeleteMapping
    public ResponseEntity<?> clearCart(HttpSession session) {
        try {
            String userId = (String) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
            }
            
            cartService.clearCart(userId);
            return ResponseEntity.ok(Map.of("message", "장바구니가 비워졌습니다."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "장바구니 비우기 중 오류가 발생했습니다."));
        }
    }
}
