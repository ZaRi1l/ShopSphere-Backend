package com.shopsphere.shopsphere_web.dto;

import lombok.Data;
import java.time.LocalDateTime;
// ProductDTO import가 필요할 수 있습니다 (만약 Response에 ProductDTO.Response가 있다면)
import com.shopsphere.shopsphere_web.dto.ProductDTO; 
import com.shopsphere.shopsphere_web.dto.ProductOptionDTO;

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
        private boolean hasReviewed; // 현재 사용자가 이 주문 항목에 대해 리뷰를 작성했는지 여부
    }
}
