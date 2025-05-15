package com.shopsphere.shopsphere_web.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProductCategoryDTO {
    private Integer id;
    private String name;
    private Integer parentId;
    private LocalDateTime createdAt;

    @Data
    public static class CreateRequest {
        private String name;
        private Integer parentId;
    }

    @Data
    public static class Response {
        private Integer id;
        private String name;
        private Response parent;
        private LocalDateTime createdAt;
    }
}
