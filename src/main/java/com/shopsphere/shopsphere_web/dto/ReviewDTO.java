package com.shopsphere.shopsphere_web.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewDTO {
    private Integer id;
    private String userId;
    private Integer productId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    @Data
    public static class CreateRequest {
        private Integer productId;
        private Integer rating;
        private String comment;
    }

    @Data
    public static class UpdateRequest {
        private Integer rating;
        private String comment;
    }

    @Data
    public static class Response {
        private Integer id;
        private UserDTO.Response user;
        private ProductDTO.Response product;
        private Integer rating;
        private String comment;
        private LocalDateTime createdAt;
        // --- 새로 추가된 필드 ---
        private String reviewImageUrl;
    }
}
