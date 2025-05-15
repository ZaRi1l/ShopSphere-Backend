package com.shopsphere.shopsphere_web.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CartItemDTO {
    private Integer id;
    private Integer cartId;
    private Integer productId;
    private Integer quantity;
    private LocalDateTime createdAt;

    @Data
    public static class AddRequest {
        private Integer productId;
        private Integer quantity;
        private Integer optionId;
    }

    @Data
    public static class UpdateRequest {
        private Integer quantity;
    }

    @Data
    public static class Response {
        private Integer id;
        private ProductDTO.Response product;
        private ProductOptionDTO.Response option;
        private Integer quantity;
        private Integer totalPrice;
        private LocalDateTime createdAt;
    }
}
