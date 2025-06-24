// src/main/java/com/shopsphere/shopsphere_web/service/OrderService.java
package com.shopsphere.shopsphere_web.service;

import com.shopsphere.shopsphere_web.dto.OrderDTO;
import com.shopsphere.shopsphere_web.dto.OrderItemDTO;
import com.shopsphere.shopsphere_web.dto.ProductDTO;
import com.shopsphere.shopsphere_web.dto.ProductOptionDTO;
import com.shopsphere.shopsphere_web.dto.UserDTO; // ProductDTO는 ProductService에서 변환하므로 여기선 UserDTO만 필요할 수 있음
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
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository; // OrderItem 저장을 위해 필요
    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final UserRepository userRepository;
    private final CartService cartService; // 주문 후 장바구니 비우기 위해
    private final ProductService productService; // ProductDTO 변환을 위해
    private final ReviewRepository reviewRepository;

    @Transactional
    public OrderDTO.Response createOrder(String userId, OrderDTO.CreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다. ID: " + userId));

        // 주문 생성 전 모든 상품의 재고 확인
        for (OrderItemDTO.CreateRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + itemRequest.getProductId()));

            // 상품 재고 확인 (옵션 없는 경우)
            if (itemRequest.getOptionId() == null) {
                if (product.getStockQuantity() < itemRequest.getQuantity()) {
                    throw new IllegalArgumentException(String.format(
                        "[%s] 상품의 재고가 부족합니다. 요청 수량: %d, 현재 재고: %d",
                        product.getName(), itemRequest.getQuantity(), product.getStockQuantity()
                    ));
                }
            } else { // 옵션 재고 확인
                ProductOption option = productOptionRepository.findById(itemRequest.getOptionId())
                        .orElseThrow(() -> new IllegalArgumentException("상품 옵션을 찾을 수 없습니다. ID: " + itemRequest.getOptionId()));

                if (!option.getProduct().getId().equals(product.getId())) {
                     throw new IllegalArgumentException(String.format(
                        "상품 ID %d에 대한 옵션 ID %d가 유효하지 않습니다.", product.getId(), itemRequest.getOptionId()
                    ));
                }

                if (option.getStockQuantity() < itemRequest.getQuantity()) {
                    throw new IllegalArgumentException(String.format(
                        "[%s - %s] 옵션의 재고가 부족합니다. 요청 수량: %d, 현재 재고: %d",
                        product.getName(), option.getSize(), itemRequest.getQuantity(), option.getStockQuantity()
                    ));
                }
            }
        }

        // 모든 재고 확인 후 주문 생성
        Order order = Order.builder()
                .user(user)
                .orderDate(LocalDateTime.now())
                .orderStatus("PENDING_PAYMENT") // 초기 상태 (예: 결제 대기)
                .shippingAddress(request.getShippingAddress())
                .paymentMethod(request.getPaymentMethod())
                .createdAt(LocalDateTime.now())
                .build();
        // orderItems 리스트는 Order 엔티티의 @Builder.Default에 의해 new ArrayList<>()로 초기화됨

        int calculatedTotalAmount = 0;

        // 주문 상품 생성 및 재고 업데이트
        for (OrderItemDTO.CreateRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId()).get(); // 위에서 이미 검증됨

            int itemBasePrice = product.getPrice();
            ProductOption option = null;

            if (itemRequest.getOptionId() != null) {
                option = productOptionRepository.findById(itemRequest.getOptionId()).get(); // 위에서 이미 검증됨
                // 옵션 재고 차감
                option.setStockQuantity(option.getStockQuantity() - itemRequest.getQuantity());
                productOptionRepository.save(option); // 변경된 옵션 재고 저장
                itemBasePrice += option.getAdditionalPrice();
            } else {
                // 상품 재고 차감 (옵션 없는 경우)
                product.setStockQuantity(product.getStockQuantity() - itemRequest.getQuantity());
            }
            
            // 공통: 상품 판매량 증가
            product.setSalesVolume((product.getSalesVolume() != null ? product.getSalesVolume() : 0) + itemRequest.getQuantity());
            productRepository.save(product); // 변경된 상품 재고 및 판매량 저장


            OrderItem orderItem = OrderItem.builder()
                    .order(order) // 양방향 연관관계 설정 (Order 엔티티 저장 시 OrderItem도 함께 저장됨 - CascadeType.ALL)
                    .product(product)
                    .productOption(option)
                    .quantity(itemRequest.getQuantity())
                    .price(itemBasePrice) // 주문 시점의 (상품 단가 + 옵션 추가금액)
                    .createdAt(LocalDateTime.now())
                    .build();
            order.getOrderItems().add(orderItem); // Order 엔티티에 OrderItem 추가
            calculatedTotalAmount += itemBasePrice * itemRequest.getQuantity();
        }

        order.setTotalAmount(calculatedTotalAmount);
        Order savedOrder = orderRepository.save(order); // Order 저장 (Cascade로 OrderItem도 저장됨)

        // 주문 성공 후 장바구니 비우기
        cartService.clearCart(userId);

        return convertToResponse(savedOrder, userId);
    }


    public OrderDTO.Response getOrder(Integer orderId) {
        // Fetch Join을 사용하여 연관 엔티티를 함께 로드하는 것이 좋음
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다. ID: " + orderId));
        return convertToResponse(order, order.getUser().getId()); // 주문자 ID 전달
    }

    public List<OrderDTO.Response> getUserOrders(String userId) {
        List<Order> userOrders = orderRepository.findByUser_IdWithDetails(userId);
        return userOrders.stream()
                .map(order -> convertToResponse(order, userId))
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderDTO.Response updateOrderStatus(Integer orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다. ID: " + orderId));
        
        // TODO: 주문 상태 변경에 따른 추가 로직 (예: 재고 복원 등)
        if ("CANCELLED".equals(status) && !"PENDING_PAYMENT".equals(order.getOrderStatus()) && !"PROCESSING".equals(order.getOrderStatus())) {
             // 이미 배송 시작되었거나 완료된 주문은 취소 불가 등의 로직 추가 가능
            // throw new IllegalStateException("주문을 취소할 수 없는 상태입니다.");
        }
        // 주문 취소 시 재고 복원 로직 (예시)
        if ("CANCELLED".equals(status) && ("PENDING_PAYMENT".equals(order.getOrderStatus()) || "PROCESSING".equals(order.getOrderStatus()))) {
            for (OrderItem item : order.getOrderItems()) {
                Product product = item.getProduct();
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                product.setSalesVolume((product.getSalesVolume() != null ? product.getSalesVolume() : 0) - item.getQuantity());
                productRepository.save(product);

                if (item.getProductOption() != null) {
                    ProductOption option = item.getProductOption();
                    option.setStockQuantity(option.getStockQuantity() + item.getQuantity());
                    productOptionRepository.save(option);
                }
            }
        }

        order.setOrderStatus(status);
        Order updatedOrder = orderRepository.save(order);
        return convertToResponse(updatedOrder, updatedOrder.getUser().getId());
    }

    @Transactional
    public OrderDTO.Response updateOrderAddress(Integer orderId, String newAddress) {
        if (newAddress == null || newAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("배송 주소를 입력해주세요.");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다. ID: " + orderId));
        
        // TODO: 주문 상태에 따라 주소 변경 가능 여부 체크 (예: 배송 시작 전까지만)
        if (!"PENDING_PAYMENT".equals(order.getOrderStatus()) && !"PROCESSING".equals(order.getOrderStatus())) {
            // throw new IllegalStateException("배송 주소를 변경할 수 없는 주문 상태입니다.");
        }

        order.setShippingAddress(newAddress);
        Order updatedOrder = orderRepository.save(order);
        return convertToResponse(updatedOrder, updatedOrder.getUser().getId());
    }

    private OrderDTO.Response convertToResponse(Order order, String userId) {
        OrderDTO.Response response = new OrderDTO.Response();
        response.setId(order.getId());
        response.setUser(convertToUserResponse(order.getUser()));
        response.setOrderDate(order.getOrderDate());
        response.setOrderStatus(order.getOrderStatus());
        response.setShippingAddress(order.getShippingAddress());
        response.setTotalAmount(order.getTotalAmount());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setTransactionId(order.getTransactionId());
        response.setCreatedAt(order.getCreatedAt());

        List<OrderItemDTO.Response> itemResponses = order.getOrderItems().stream()
                .map(item -> convertToOrderItemResponse(item, userId))
                .collect(Collectors.toList());
        response.setItems(itemResponses);

        return response;
    }

    private OrderItemDTO.Response convertToOrderItemResponse(OrderItem item, String userId) {
        OrderItemDTO.Response response = new OrderItemDTO.Response();
        response.setId(item.getId());

        if (item.getProduct() != null) {
            ProductDTO.Response productDto = productService.getProduct(item.getProduct().getId());
            response.setProduct(productDto);
        }

        if (item.getProductOption() != null) {
            response.setOption(convertToOptionResponse(item.getProductOption()));
        }
        response.setQuantity(item.getQuantity());
        response.setPrice(item.getPrice());
        response.setTotalPrice(item.getPrice() * item.getQuantity());
        response.setCreatedAt(item.getCreatedAt());

        if (item.getProduct() != null && userId != null) {
            boolean hasReviewed = reviewRepository.existsByUser_IdAndProduct_Id(userId, item.getProduct().getId());
            response.setHasReviewed(hasReviewed);
        } else {
            response.setHasReviewed(false);
        }

        return response;
    }

    private ProductOptionDTO.Response convertToOptionResponse(ProductOption option) {
        if (option == null) return null;
        ProductOptionDTO.Response response = new ProductOptionDTO.Response();
        response.setId(option.getId());
        response.setSize(option.getSize());
        response.setStockQuantity(option.getStockQuantity());
        response.setAdditionalPrice(option.getAdditionalPrice());
        return response;
    }

    private UserDTO.Response convertToUserResponse(User user) {
        if (user == null) return null;
        UserDTO.Response response = new UserDTO.Response();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setName(user.getName());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setAddress(user.getAddress());
        response.setRole(user.getRole());
        response.setProfileImageUrl(user.getProfileImageUrl());
        return response;
    }
}