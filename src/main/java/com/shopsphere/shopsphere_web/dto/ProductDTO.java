package com.shopsphere.shopsphere_web.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import com.shopsphere.shopsphere_web.dto.ProductOptionDTO;
import com.shopsphere.shopsphere_web.dto.ProductImageDTO;
import com.shopsphere.shopsphere_web.dto.ProductCategoryDTO;
import com.shopsphere.shopsphere_web.dto.UserDTO;

@Data
public class ProductDTO {
    private Integer id;
    private Integer categoryId;
    private String name;
    private String description;
    private Integer price;
    private Integer stockQuantity;
    private String imageUrl;
    private LocalDateTime createdAt;
    private String userId;
    private Integer salesVolume;
    private List<ProductOptionDTO> options;
    private List<ProductImageDTO> images;

    @Data
    public static class CreateRequest {
        private Integer categoryId;
        private String name;
        private String description;
        private Integer price;
        private Integer stockQuantity;
        private String imageUrl;
        private List<ProductOptionDTO.CreateRequest> options;
    }

    @Data
    public static class UpdateRequest {
        private Integer categoryId;
        private String name;
        private String description;
        private Integer price;
        private Integer stockQuantity;
        private String imageUrl;
        private List<ProductOptionDTO.UpdateRequest> options;
    }

    @Data
    public static class Response {
        private Integer id;
        private ProductCategoryDTO.Response category;
        private String name;
        private String description;
        private Integer price;
        private Integer stockQuantity;
        private String imageUrl;
        private LocalDateTime createdAt;
        private UserDTO.Response seller;
        private Integer salesVolume;
        private List<ProductOptionDTO.Response> options;
        private List<ProductImageDTO.Response> images;
        private Double averageRating;
        private Long reviewCount;     // 리뷰 개수
        private Long interestCount;   // 관심(찜) 개수 - 이 부분은 백엔드 구현에 따라 달라질 수 있습니다.
    }
}
