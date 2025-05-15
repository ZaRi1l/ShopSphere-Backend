package com.shopsphere.shopsphere_web.service;

import com.shopsphere.shopsphere_web.dto.CartDTO;
import com.shopsphere.shopsphere_web.dto.CartItemDTO;
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
        Cart cart = getOrCreateCartEntity(userId);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // Validate product option if provided
        ProductOption option = null;
        if (request.getOptionId() != null) {
            option = productOptionRepository.findById(request.getOptionId())
                    .orElseThrow(() -> new IllegalArgumentException("Product option not found"));
            if (!option.getProduct().getId().equals(product.getId())) {
                throw new IllegalArgumentException("Option does not belong to the product");
            }
        }

        // Check if item already exists in cart
        CartItem existingItem = cartItemRepository.findByCart_Id(cart.getId()).stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            cartItemRepository.save(existingItem);
        } else {
            CartItem cartItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .createdAt(LocalDateTime.now())
                    .build();
            cartItemRepository.save(cartItem);
        }

        return convertToResponse(cart);
    }

    @Transactional
    public CartDTO.Response updateItemQuantity(String userId, Integer cartItemId, CartItemDTO.UpdateRequest request) {
        Cart cart = getOrCreateCartEntity(userId);
        
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Cart item does not belong to user's cart");
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
        
        // Calculate total price including option price if present
        int totalPrice = item.getProduct().getPrice() * item.getQuantity();
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
