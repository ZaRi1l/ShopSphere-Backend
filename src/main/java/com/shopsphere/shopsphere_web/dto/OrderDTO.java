package com.shopsphere.shopsphere_web.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import com.shopsphere.shopsphere_web.dto.OrderItemDTO;
import com.shopsphere.shopsphere_web.dto.UserDTO;

@Data
public class OrderDTO {
    private Integer id;
    private String userId;
    private LocalDateTime orderDate;
    private String orderStatus;
    private String shippingAddress;
    private Integer totalAmount;
    private String paymentMethod;
    private String transactionId;
    private LocalDateTime createdAt;
    private List<OrderItemDTO> items;

    @Data
    public static class CreateRequest {
        private String shippingAddress;
        private String paymentMethod;
        private List<OrderItemDTO.CreateRequest> items;
    }

    @Data
    public static class Response {
        private Integer id;
        private UserDTO.Response user;
        private LocalDateTime orderDate;
        private String orderStatus;
        private String shippingAddress;
        private Integer totalAmount;
        private String paymentMethod;
        private String transactionId;
        private List<OrderItemDTO.Response> items;
        private LocalDateTime createdAt;
    }
    
    @Data
    public static class UpdateAddressRequest {
        private String shippingAddress;
    }
}
