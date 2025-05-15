package com.shopsphere.shopsphere_web.dto;

import lombok.Data;

@Data
public class ProductOptionDTO {
    private Integer id;
    private Integer productId;
    private String size;
    private Integer stockQuantity;
    private Integer additionalPrice;

    @Data
    public static class CreateRequest {
        private String size;
        private Integer stockQuantity;
        private Integer additionalPrice;
    }

    @Data
    public static class UpdateRequest {
        private Integer id;
        private String size;
        private Integer stockQuantity;
        private Integer additionalPrice;
    }

    @Data
    public static class Response {
        private Integer id;
        private String size;
        private Integer stockQuantity;
        private Integer additionalPrice;
    }
}
