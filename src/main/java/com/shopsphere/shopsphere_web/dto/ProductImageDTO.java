package com.shopsphere.shopsphere_web.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProductImageDTO {
    private Integer id;
    private Integer productId;
    private String imageUrl;
    private LocalDateTime createdAt;

    @Data
    public static class CreateRequest {
        private String imageUrl;
    }

    @Data
    public static class Response {
        private Integer id;
        private String imageUrl;
        private LocalDateTime createdAt;
    }
}
