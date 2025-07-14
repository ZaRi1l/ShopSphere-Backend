package com.shopsphere.shopsphere_web.service;

import com.shopsphere.shopsphere_web.dto.CartDTO;
import com.shopsphere.shopsphere_web.dto.CartItemDTO;
import com.shopsphere.shopsphere_web.dto.OrderItemDTO;
import com.shopsphere.shopsphere_web.dto.ProductOptionDTO;
import com.shopsphere.shopsphere_web.dto.UserDTO;
import com.shopsphere.shopsphere_web.entity.*;
import com.shopsphere.shopsphere_web.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final UserRepository userRepository;
    private final ProductService productService;

    @Transactional
    public CartDTO.Response getOrCreateCart(String userId) {
        return convertToResponse(getOrCreateCartEntity(userId));
    }

    @Transactional
    protected Cart getOrCreateCartEntity(String userId) {
        return cartRepository.findByUser_Id(userId)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(userRepository.findById(userId)
                                    .orElseThrow(() -> new IllegalArgumentException("User not found")))
                            .createdAt(LocalDateTime.now())
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    @Transactional
    public CartDTO.Response addItem(String userId, CartItemDTO.AddRequest request) {
        if (request.getQuantity() <= 0) {
            throw new IllegalArgumentException("수량은 1개 이상이어야 합니다.");
        }

        Cart cart = getOrCreateCartEntity(userId);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        // 상품 옵션 유효성 검사
        final ProductOption option;
        if (request.getOptionId() != null) {
            option = productOptionRepository.findById(request.getOptionId())
                    .orElseThrow(() -> new IllegalArgumentException("상품 옵션을 찾을 수 없습니다."));
            if (!option.getProduct().getId().equals(product.getId())) {
                throw new IllegalArgumentException("해당 상품의 옵션이 아닙니다.");
            }
            // 옵션 재고 확인
            if (request.getQuantity() > option.getStockQuantity()) {
                throw new IllegalArgumentException("선택하신 옵션의 재고가 부족합니다. 최대 수량: " + option.getStockQuantity());
            }
        } else {
            option = null;
            // 일반 상품 재고 확인
            if (request.getQuantity() > product.getStockQuantity()) {
                throw new IllegalArgumentException("상품 재고가 부족합니다. 최대 수량: " + product.getStockQuantity());
            }
        }

        // 기존 장바구니 아이템 조회
        CartItem existingItem = cartItemRepository.findByCart_Id(cart.getId()).stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()) && 
                    ((item.getProductOption() == null && option == null) || 
                     (item.getProductOption() != null && option != null && 
                      item.getProductOption().getId().equals(option.getId()))))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            // 기존 아이템이 있는 경우 수량 증가 전 추가 재고 확인
            int newQuantity = existingItem.getQuantity() + request.getQuantity();
            if (option != null) {
                if (newQuantity > option.getStockQuantity()) {
                    throw new IllegalArgumentException("선택하신 옵션의 재고가 부족합니다. 추가 가능 수량: " + 
                            (option.getStockQuantity() - existingItem.getQuantity()));
                }
            } else {
                if (newQuantity > product.getStockQuantity()) {
                    throw new IllegalArgumentException("상품 재고가 부족합니다. 추가 가능 수량: " + 
                            (product.getStockQuantity() - existingItem.getQuantity()));
                }
            }
            
            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
        } else {
            // 새 아이템 추가
            CartItem cartItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .productOption(option)
                    .quantity(request.getQuantity())
                    .createdAt(LocalDateTime.now())
                    .build();
            cartItemRepository.save(cartItem);
        }

        return convertToResponse(cart);
    }

    @Transactional
    public CartDTO.Response updateItemQuantity(String userId, Integer cartItemId, CartItemDTO.UpdateRequest request) {
        if (request.getQuantity() <= 0) {
            throw new IllegalArgumentException("수량은 1개 이상이어야 합니다.");
        }

        Cart cart = getOrCreateCartEntity(userId);
        
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니 상품을 찾을 수 없습니다."));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("해당 장바구니 상품에 대한 권한이 없습니다.");
        }

        // 상품 옵션이 있는 경우 옵션 재고 확인
        if (cartItem.getProductOption() != null) {
            if (request.getQuantity() > cartItem.getProductOption().getStockQuantity()) {
                throw new IllegalArgumentException("선택하신 옵션의 재고가 부족합니다. 최대 수량: " + cartItem.getProductOption().getStockQuantity());
            }
        } else {
            // 일반 상품 재고 확인
            if (request.getQuantity() > cartItem.getProduct().getStockQuantity()) {
                throw new IllegalArgumentException("상품 재고가 부족합니다. 최대 수량: " + cartItem.getProduct().getStockQuantity());
            }
        }

        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);

        return convertToResponse(cart);
    }

    @Transactional
    public CartDTO.Response removeItem(String userId, Integer cartItemId) {
        Cart cart = getOrCreateCartEntity(userId);
        
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Cart item does not belong to user's cart");
        }

        cartItemRepository.delete(cartItem);
        return convertToResponse(cart);
    }

    @Transactional
    public void clearCart(String userId) {
        Cart cart = getOrCreateCartEntity(userId);
        cartItemRepository.deleteAll(cartItemRepository.findByCart_Id(cart.getId()));
    }
    
    @Transactional
    public void removeItems(String userId, List<OrderItemDTO.CreateRequest> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        
        Cart cart = getOrCreateCartEntity(userId);
        List<CartItem> cartItems = cartItemRepository.findByCart_Id(cart.getId());
        
        for (OrderItemDTO.CreateRequest item : items) {
            cartItems.stream()
                .filter(cartItem -> {
                    boolean productMatches = cartItem.getProduct().getId().equals(item.getProductId());
                    boolean optionMatches = (item.getOptionId() == null && cartItem.getProductOption() == null) || 
                                          (item.getOptionId() != null && cartItem.getProductOption() != null && 
                                           cartItem.getProductOption().getId().equals(item.getOptionId()));
                    return productMatches && optionMatches;
                })
                .findFirst()
                .ifPresent(cartItem -> cartItemRepository.delete(cartItem));
        }
    }

    private CartDTO.Response convertToResponse(Cart cart) {
        CartDTO.Response response = new CartDTO.Response();
        response.setId(cart.getId());
        response.setUser(convertToUserResponse(cart.getUser()));
        
        List<CartItemDTO.Response> itemResponses = cartItemRepository.findByCart_Id(cart.getId()).stream()
                .map(this::convertToCartItemResponse)
                .collect(Collectors.toList());
        
        response.setItems(itemResponses);
        response.setCreatedAt(cart.getCreatedAt());
        
        // Calculate total amount
        int totalAmount = itemResponses.stream()
                .mapToInt(CartItemDTO.Response::getTotalPrice)
                .sum();
        response.setTotalAmount(totalAmount);
        
        return response;
    }

    private CartItemDTO.Response convertToCartItemResponse(CartItem item) {
        CartItemDTO.Response response = new CartItemDTO.Response();
        response.setId(item.getId());
        response.setProduct(productService.getProduct(item.getProduct().getId()));
        response.setQuantity(item.getQuantity());
        
        // Set product option if exists
        if (item.getProductOption() != null) {
            ProductOptionDTO.Response optionResponse = new ProductOptionDTO.Response();
            optionResponse.setId(item.getProductOption().getId());
            optionResponse.setSize(item.getProductOption().getSize());
            optionResponse.setStockQuantity(item.getProductOption().getStockQuantity());
            optionResponse.setAdditionalPrice(item.getProductOption().getAdditionalPrice());
            response.setOption(optionResponse);
        }
        
        // Calculate total price including option price if present
        int basePrice = item.getProduct().getPrice();
        int optionPrice = (item.getProductOption() != null) ? item.getProductOption().getAdditionalPrice() : 0;
        int totalPrice = (basePrice + optionPrice) * item.getQuantity();
        response.setTotalPrice(totalPrice);
        
        response.setCreatedAt(item.getCreatedAt());
        return response;
    }

    private UserDTO.Response convertToUserResponse(User user) {
        UserDTO.Response response = new UserDTO.Response();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setName(user.getName());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setAddress(user.getAddress());
        response.setRole(user.getRole());
        return response;
    }
}
