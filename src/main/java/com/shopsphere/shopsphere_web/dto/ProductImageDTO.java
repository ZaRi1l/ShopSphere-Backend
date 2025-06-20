package com.shopsphere.shopsphere_web.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data // 이 DTO 클래스에 대한 Lombok @Data
public class ProductImageDTO { // 독립 클래스로 가정
    private Long id;
    private String imageUrl;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private Boolean isRepresentative; // 대표 이미지 여부 (선택적)

    @Data
    public static class CreateRequest {
        private String imageUrl;
    }

    @Data
    public static class Response {
        private Long id;
        private String imageUrl;
        private LocalDateTime createdAt;
    }

    
}
