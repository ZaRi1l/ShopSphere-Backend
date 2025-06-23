package com.shopsphere.shopsphere_web.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
// ProductOptionDTO, ProductImageDTO, ProductCategoryDTO, UserDTO 등 필요한 DTO import

@Data
public class ProductDTO {
    // 이 필드들은 ProductService.convertToResponse에서 Response 내부 클래스로 옮겨졌으므로,
    // 최상위 ProductDTO는 사실상 네임스페이스 역할만 하거나 제거될 수 있습니다.
    // 여기서는 CreateRequest, UpdateRequest, Response를 내부 클래스로 유지합니다.

    @Data
    public static class CreateRequest {
        private Integer categoryId;
        private String name;
        private String description;
        private Integer price;
        private Integer stockQuantity;
        private String imageUrl; // 대표 이미지 URL
        private List<String> additionalImageUrls; // 추가 이미지 URL 목록 (ProductService에서 처리 방식 변경 필요)
        private List<ProductOptionDTO.CreateRequest> options;
    }

    @Data
    public static class UpdateRequest {
        private Integer categoryId;
        private String name;
        private String description;
        private Integer price;
        private Integer stockQuantity;
        private String imageUrl; // 대표 이미지 URL
        // 이미지 수정/삭제/추가를 위한 필드 추가 필요 (예: List<Integer> deletedImageIds, List<String> newImageUrls)
        private List<ProductOptionDTO.UpdateRequest> options;
    }

    @Data
    public static class Response {
        private Integer id;
        private ProductCategoryDTO.Response category; // ProductCategoryDTO.Response 타입으로 변경
        private String name;
        private String description;
        private Integer price;
        private Integer stockQuantity;
        // private String imageUrl; // Product 엔티티에서 제거하고 images 리스트로 관리
        private LocalDateTime createdAt;
        private UserDTO.Response seller; // UserDTO.Response 타입으로 변경
        private Integer salesVolume;
        private List<ProductOptionDTO.Response> options; // ProductOptionDTO.Response 타입으로 변경
        private List<ProductImageDTO> images; // 모든 상품 이미지를 담는 리스트
        private Double averageRating;
        private Long reviewCount;
        private Long interestCount; // 예시 필드
    }
}