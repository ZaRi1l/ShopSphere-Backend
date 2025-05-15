package com.shopsphere.shopsphere_web.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OrderItemDTO {
    private Integer id;
    private Integer orderId;
    private Integer productId;
    private Integer optionId;
    private Integer quantity;
    private Integer price;
    private LocalDateTime createdAt;

    @Data
    public static class CreateRequest {
        private Integer productId;
        private Integer optionId;
        private Integer quantity;
    }

    @Data
    public static class Response {
        private Integer id;
        private ProductDTO.Response product;
        private ProductOptionDTO.Response option;
        private Integer quantity;
        private Integer price;
        private Integer totalPrice;
        private LocalDateTime createdAt;
    }
}
