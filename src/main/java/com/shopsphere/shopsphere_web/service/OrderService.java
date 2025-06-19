package com.shopsphere.shopsphere_web.service;

import com.shopsphere.shopsphere_web.dto.OrderDTO;
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
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final ProductService productService;

    @Transactional
    public OrderDTO.Response createOrder(String userId, OrderDTO.CreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));

        // 주문 생성 전 모든 상품의 재고 확인
        for (OrderItemDTO.CreateRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + itemRequest.getProductId()));

            // 상품 재고 확인
            if (product.getStockQuantity() < itemRequest.getQuantity()) {
                throw new IllegalArgumentException(String.format(
                    "[%s] 상품의 재고가 부족합니다. 요청 수량: %d, 현재 재고: %d", 
                    product.getName(), itemRequest.getQuantity(), product.getStockQuantity()
                ));
            }

            // 옵션 재고 확인
            if (itemRequest.getOptionId() != null) {
                ProductOption option = productOptionRepository.findById(itemRequest.getOptionId())
                        .orElseThrow(() -> new IllegalArgumentException("상품 옵션을 찾을 수 없습니다. ID: " + itemRequest.getOptionId()));
                        
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
                .orderStatus("PENDING")
                .shippingAddress(request.getShippingAddress())
                .paymentMethod(request.getPaymentMethod())
                .createdAt(LocalDateTime.now())
                .build();

        int totalAmount = 0;

        // 주문 상품 생성 및 재고 업데이트
        for (OrderItemDTO.CreateRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + itemRequest.getProductId()));

            // 상품 재고 업데이트
            product.setStockQuantity(product.getStockQuantity() - itemRequest.getQuantity());
            product.setSalesVolume(product.getSalesVolume() + itemRequest.getQuantity());

            // 가격 계산 (옵션 가격 포함)
            int itemPrice = product.getPrice();
            ProductOption option = null;
            
            if (itemRequest.getOptionId() != null) {
                option = productOptionRepository.findById(itemRequest.getOptionId())
                        .orElseThrow(() -> new IllegalArgumentException("상품 옵션을 찾을 수 없습니다. ID: " + itemRequest.getOptionId()));
                
                // 옵션 재고 업데이트
                option.setStockQuantity(option.getStockQuantity() - itemRequest.getQuantity());
                itemPrice += option.getAdditionalPrice();
            }

            // 주문 상품 생성
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productOption(option)
                    .quantity(itemRequest.getQuantity())
                    .price(itemPrice)
                    .createdAt(LocalDateTime.now())
                    .build();

            order.getOrderItems().add(orderItem);
            totalAmount += itemPrice * itemRequest.getQuantity();
        }

        order.setTotalAmount(totalAmount);
        order = orderRepository.save(order);

        // Clear the user's cart after successful order
        cartService.clearCart(userId);

        return convertToResponse(order);
    }

    public OrderDTO.Response getOrder(Integer orderId) {
        return orderRepository.findById(orderId)
                .map(this::convertToResponse)
                .orElse(null);
    }

    public List<OrderDTO.Response> getUserOrders(String userId) {
        return orderRepository.findByUser_Id(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderDTO.Response updateOrderStatus(Integer orderId, String status) {
        return orderRepository.findById(orderId)
                .map(order -> {
                    order.setOrderStatus(status);
                    return convertToResponse(order);
                })
                .orElse(null);
    }
    
    /**
     * 주문의 배송 주소를 업데이트합니다.
     * @param orderId 주문 ID
     * @param newAddress 새로운 배송 주소
     * @return 업데이트된 주문 정보
     */
    @Transactional
    public OrderDTO.Response updateOrderAddress(Integer orderId, String newAddress) {
        if (newAddress == null || newAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("배송 주소를 입력해주세요.");
        }
        
        return orderRepository.findById(orderId)
                .map(order -> {
                    order.setShippingAddress(newAddress);
                    return convertToResponse(order);
                })
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
    }

    private OrderDTO.Response convertToResponse(Order order) {
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

        List<OrderItemDTO.Response> itemResponses = orderItemRepository.findByOrder_Id(order.getId()).stream()
                .map(this::convertToOrderItemResponse)
                .collect(Collectors.toList());
        response.setItems(itemResponses);

        return response;
    }

    private OrderItemDTO.Response convertToOrderItemResponse(OrderItem item) {
        OrderItemDTO.Response response = new OrderItemDTO.Response();
        response.setId(item.getId());
        response.setProduct(productService.getProduct(item.getProduct().getId()));
        if (item.getProductOption() != null) {
            response.setOption(convertToOptionResponse(item.getProductOption()));
        }
        response.setQuantity(item.getQuantity());
        response.setPrice(item.getPrice());
        response.setTotalPrice(item.getPrice() * item.getQuantity());
        response.setCreatedAt(item.getCreatedAt());
        return response;
    }

    private ProductOptionDTO.Response convertToOptionResponse(ProductOption option) {
        ProductOptionDTO.Response response = new ProductOptionDTO.Response();
        response.setId(option.getId());
        response.setSize(option.getSize());
        response.setStockQuantity(option.getStockQuantity());
        response.setAdditionalPrice(option.getAdditionalPrice());
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
