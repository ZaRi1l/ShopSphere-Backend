// src/main/java/com/shopsphere/shopsphere_web/service/OrderService.java
package com.shopsphere.shopsphere_web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.shopsphere_web.dto.OrderDTO;
import com.shopsphere.shopsphere_web.dto.OrderItemDTO;
import com.shopsphere.shopsphere_web.dto.ProductDTO;
import com.shopsphere.shopsphere_web.dto.ProductOptionDTO;
import com.shopsphere.shopsphere_web.dto.UserDTO; // ProductDTO는 ProductService에서 변환하므로 여기선 UserDTO만 필요할 수 있음
import com.shopsphere.shopsphere_web.entity.*;
import com.shopsphere.shopsphere_web.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpHeaders;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 토스페이먼츠 API 통신을 위한 DTO (해당 경로에 있어야 합니다)
import com.shopsphere.shopsphere_web.dto.TossPaymentResponseDTO;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

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

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // application.properties 또는 application.yml에서 설정된 값 주입
    @Value("${toss.payments.secretKey}")
    private String tossPaymentsSecretKey;

    @Value("${toss.payments.baseUrl}")
    private String tossPaymentsBaseUrl;

    @Value("${toss.payments.successUrl}")
    private String tossPaymentsSuccessUrl;

    @Value("${toss.payments.failUrl}")
    private String tossPaymentsFailUrl;

    @Transactional
    public OrderDTO.Response createOrder(String userId, OrderDTO.CreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다. ID: " + userId));

        // 주문 생성 전 모든 상품의 재고 확인
        for (OrderItemDTO.CreateRequest itemRequest : request.getItems()) {
            Product product = productRepository.findByIdForUpdate(itemRequest.getProductId()) // 이 부분 변경
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
                ProductOption option = productOptionRepository.findByIdForUpdate(itemRequest.getOptionId()) // 이 부분 변경
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
// 1. 주문을 먼저 저장하여 영속화하고 ID를 부여받습니다.


        // 2. 토스페이먼츠용 고유 주문 ID를 생성하고 transactionId 필드에 저장합니다.
        String tossOrderId = "shopsphere_order_" + savedOrder.getId() + "_" + System.currentTimeMillis();
        savedOrder.setTransactionId(tossOrderId);

        // 3. transactionId가 업데이트된 주문을 다시 저장합니다.
        Order finalOrder = orderRepository.save(savedOrder);

        // 4. 장바구니 비우기 로직은 그대로 유지합니다.
        cartService.clearCart(userId);

        // 5. 최종 주문 정보를 DTO로 변환하여 반환합니다.
        // 기존에 사용하시던 convertToResponse 메소드를 호출합니다.
        return convertToResponse(finalOrder, userId);
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

    @Transactional // 결제 요청 시 order 테이블의 transactionId를 업데이트하므로 @Transactional 필요
    public Map<String, String> requestTossPayment(Integer orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 주문 소유자 확인 (OrderController에서 이미 했지만, 서비스 계층에서도 방어적으로 확인)
        // User 엔티티의 ID 필드를 사용하여 비교 (userId는 String 타입으로 가정)
        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("해당 주문에 대한 접근 권한이 없습니다.");
        }

        // TODO: 주문 상태가 'PENDING_PAYMENT' 또는 'PAYMENT_FAILED'와 같이 결제 가능한 상태인지 확인하는 로직 추가
        if (!"PENDING_PAYMENT".equals(order.getOrderStatus()) && !"PAYMENT_FAILED".equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("현재 주문 상태에서는 결제를 요청할 수 없습니다. 현재 상태: " + order.getOrderStatus());
        }

        // 토스페이먼츠 요청에 필요한 HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String encodedAuth = Base64.getEncoder().encodeToString((tossPaymentsSecretKey + ":").getBytes(StandardCharsets.UTF_8));
        headers.setBasicAuth(encodedAuth);

        // 토스페이먼츠용 고유 주문 ID 생성 (예: "order_우리시스템OrderId_타임스탬프")
        // 토스페이먼츠 orderId는 최대 64자 영문 대소문자, 숫자, -, _ 만 허용
        String tossOrderId = "order_" + order.getId() + "_" + System.currentTimeMillis();

        // 주문 정보에 토스 주문 ID 저장 (선택 사항이지만 추적에 유용)
        // Order 엔티티의 transactionId 필드를 토스페이먼츠의 orderId로 활용
        order.setTransactionId(tossOrderId);
        orderRepository.save(order);


        // 토스페이먼츠 요청 바디 생성
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("orderId", tossOrderId); // 토스페이먼츠용 고유 주문 ID
        requestBody.put("amount", order.getTotalAmount());
        // 실제 상품명 조합 로직 필요 (예: "상품A 외 2개" 또는 주문 상품 리스트에서 대표 상품명)
        requestBody.put("orderName", "상품 구매 (" + order.getId() + ")");
        requestBody.put("customerName", order.getUser().getName()); // 사용자 이름 사용
        requestBody.put("successUrl", tossPaymentsSuccessUrl);
        requestBody.put("failUrl", tossPaymentsFailUrl);
        // requestBody.put("flowMode", "DEFAULT"); // 리다이렉트 방식 (필요시 추가)
        // requestBody.put("easyPay", "NAVER_PAY"); // 특정 간편결제로 바로 연결 (필요시 추가)

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            // 토스페이먼츠 결제 승인 API 호출 (POST /v1/payments)
            ResponseEntity<TossPaymentResponseDTO> responseEntity = restTemplate.postForEntity(
                    tossPaymentsBaseUrl + "/v1/payments",
                    entity,
                    TossPaymentResponseDTO.class
            );

            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                TossPaymentResponseDTO tossResponse = responseEntity.getBody();

                // 임시 저장했던 tossOrderId (transactionId)와 실제로 발급받은 paymentKey를 함께 반환
                Map<String, String> paymentInfo = new HashMap<>();
                paymentInfo.put("paymentKey", tossResponse.getPaymentKey());
                paymentInfo.put("checkoutUrl", tossResponse.getCheckout().getUrl()); // 결제창 URL
                paymentInfo.put("tossOrderId", tossResponse.getOrderId()); // 토스에서 받은 orderId (우리가 보낸 값과 동일해야 함)

                return paymentInfo;

            } else {
                // 토스 응답은 2xx이나 body가 없거나 예상치 못한 경우
                throw new RuntimeException("토스페이먼츠 결제 요청 실패: 응답 코드 " + responseEntity.getStatusCode() + " - 응답 바디 없음 또는 오류.");
            }
        } catch (HttpClientErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            System.err.println("토스페이먼츠 API 에러 (requestTossPayment): " + errorBody);
            // 오류 응답 본문을 파싱하여 클라이언트에 더 상세한 정보 제공
            try {
                Map<String, String> errorMap = objectMapper.readValue(errorBody, Map.class);
                throw new IllegalArgumentException("토스페이먼츠 결제 요청 오류: " + errorMap.getOrDefault("message", "알 수 없는 오류"), e);
            } catch (Exception jsonEx) {
                // JSON 파싱 실패 시 원본 오류를 그대로 전달
                throw new RuntimeException("토스페이먼츠 결제 요청 중 오류 발생 (JSON 파싱 실패): " + errorBody, e);
            }
        } catch (Exception e) {
            // 그 외 예상치 못한 예외 처리
            throw new RuntimeException("토스페이먼츠 API 통신 중 예외 발생: " + e.getMessage(), e);
        }
    }

    @Transactional
    public OrderDTO.Response confirmTossPayment(String paymentKey, String tossOrderId, int amount, String userId) {
        Order order = orderRepository.findByTransactionId(tossOrderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 이미 처리된 주문 ID 입니다: " + tossOrderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new SecurityException("주문 정보에 접근할 권한이 없습니다.");
        }

        if (!order.getTotalAmount().equals(amount)) {
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
        }

        if ("PAID".equals(order.getOrderStatus())) {
            System.out.println("이미 결제 완료된 주문입니다. (멱등성 처리): " + tossOrderId);
            return toOrderResponseDTO(order);
        }

        HttpHeaders headers = new HttpHeaders();
        String encodedAuth = Base64.getEncoder().encodeToString((tossPaymentsSecretKey + ":").getBytes(StandardCharsets.UTF_8));
        headers.setBasicAuth(encodedAuth);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("paymentKey", paymentKey);
        requestBody.put("orderId", tossOrderId);
        requestBody.put("amount", amount);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<TossPaymentResponseDTO> responseEntity = restTemplate.postForEntity(
                    tossPaymentsBaseUrl + "/v1/payments/confirm",
                    entity,
                    TossPaymentResponseDTO.class
            );

            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                TossPaymentResponseDTO tossResponse = responseEntity.getBody();

                order.setOrderStatus("COMPLETED");
                order.setPaymentMethod(tossResponse.getMethod());
                order.setTransactionId(tossResponse.getPaymentKey());

                Order savedOrder = orderRepository.save(order);

                return toOrderResponseDTO(savedOrder);
            } else {
                throw new RuntimeException("토스페이먼츠 최종 결제 승인에 실패했습니다. 응답 코드: " + responseEntity.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            try {
                Map<String, String> errorMap = objectMapper.readValue(e.getResponseBodyAsString(), Map.class);
                String errorMessage = errorMap.getOrDefault("message", "알 수 없는 오류가 발생했습니다.");
                throw new IllegalArgumentException("결제 승인 실패: " + errorMessage);
            } catch (Exception jsonEx) {
                throw new RuntimeException("결제 승인 중 오류가 발생했습니다.", e);
            }
        } catch (Exception e) {
            throw new RuntimeException("결제 승인 중 예상치 못한 오류가 발생했습니다.", e);
        }
    }

    // --- Private DTO Conversion Methods ---

    private OrderDTO.Response toOrderResponseDTO(Order order) {
        OrderDTO.Response dto = new OrderDTO.Response();

        dto.setId(order.getId());
        dto.setOrderDate(order.getOrderDate());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setTransactionId(order.getTransactionId());
        dto.setCreatedAt(order.getCreatedAt());

        // User 정보 변환 (null 체크 포함)
        if (order.getUser() != null) {
            dto.setUser(toUserResponseDTO(order.getUser()));
        }

        // OrderItem 리스트 변환 (null 체크 포함)
        if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
            List<OrderItemDTO.Response> itemDtos = order.getOrderItems().stream()
                    .map(this::toOrderItemResponseDTO)
                    .collect(Collectors.toList());
            dto.setItems(itemDtos);
        }

        return dto;
    }

    private UserDTO.Response toUserResponseDTO(com.shopsphere.shopsphere_web.entity.User user) {
        return UserDTO.Response.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName()) // User 엔티티의 name 필드를 사용합니다. (getUsername() -> getName())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .role(user.getRole())
                .profileImageUrl(user.getProfileImageUrl()) // User 엔티티의 프로필 이미지 URL 필드를 사용합니다.
                .build();
    }

    private OrderItemDTO.Response toOrderItemResponseDTO(OrderItem item) {
        OrderItemDTO.Response itemDto = new OrderItemDTO.Response();

        itemDto.setId(item.getId());
        itemDto.setQuantity(item.getQuantity());
        itemDto.setPrice(item.getPrice());

        // Product 정보 변환 (null 체크 포함)
        if (item.getProduct() != null) {
            ProductDTO.Response productDto = new ProductDTO.Response();
            productDto.setId(item.getProduct().getId());
            productDto.setName(item.getProduct().getName());

            // ProductDTO.Response에는 imageUrl 필드가 없고 images 리스트가 있으므로,
            // images 리스트에서 대표 이미지를 찾아 설정하거나 첫 번째 이미지를 사용합니다.
            if (item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
                // 예를 들어, isRepresentative 플래그가 있는 첫 번째 이미지를 찾거나, 그냥 첫 번째 이미지를 사용합니다.
                // 여기서는 ProductImageDTO가 어떻게 구성되었는지 모르므로, DTO 자체를 넘깁니다.
                // ProductImageDTO에 imageUrl 필드가 있다면 아래와 같이 할 수 있습니다.
                // String representativeImageUrl = item.getProduct().getImages().get(0).getImageUrl();
                // productDto.set...(representativeImageUrl); // ProductDTO에 맞는 필드에 설정

                // ProductImageDTO 전체를 넘겨야 한다면 그대로 설정합니다.
                // ProductImageDTO 클래스가 필요합니다.
                // productDto.setImages(item.getProduct().getImages().stream()...);
            }

            // 상품 판매자 정보 설정
            if (item.getProduct().getUser() != null) {
                productDto.setSeller(toUserResponseDTO(item.getProduct().getUser()));
            }

            itemDto.setProduct(productDto);
        }

        // ProductOption 정보가 있다면 여기에 변환 로직 추가
        // if (item.getProductOption() != null) { ... }

        return itemDto;
    }
}