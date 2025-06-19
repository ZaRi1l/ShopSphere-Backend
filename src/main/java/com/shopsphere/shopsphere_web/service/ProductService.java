// src/main/java/com/shopsphere/shopsphere_web/service/ProductService.java (수정)
package com.shopsphere.shopsphere_web.service;

import com.shopsphere.shopsphere_web.dto.ProductCategoryDTO;
import com.shopsphere.shopsphere_web.dto.ProductDTO;
import com.shopsphere.shopsphere_web.dto.ProductOptionDTO;
import com.shopsphere.shopsphere_web.dto.ProductImageDTO;
import com.shopsphere.shopsphere_web.entity.*;
import com.shopsphere.shopsphere_web.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductImageRepository imageRepository;
    private final ProductOptionRepository optionRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository; // ReviewRepository 주입

    // ... createProduct, getProduct, getProductsByCategory, getProductsBySeller, updateProduct, deleteProduct 메서드들은 기존과 동일 ...

    // ProductService 내의 convertToResponse 메서드 수정
    private ProductDTO.Response convertToResponse(Product product) {
        ProductDTO.Response response = new ProductDTO.Response();
        response.setId(product.getId());
        response.setCategory(convertToCategoryResponse(product.getCategory()));
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setStockQuantity(product.getStockQuantity());
        response.setImageUrl(product.getImageUrl());
        response.setCreatedAt(product.getCreatedAt());
        response.setSalesVolume(product.getSalesVolume());
        // response.setSeller(convertToUserResponse(product.getUser())); // 판매자 정보가 필요하면 이 주석을 해제하고 구현


        // 옵션 변환
        List<ProductOptionDTO.Response> optionResponses = optionRepository.findByProduct_Id(product.getId())
                .stream()
                .map(this::convertToOptionResponse)
                .collect(Collectors.toList());
        response.setOptions(optionResponses);

        // 이미지 변환
        List<ProductImageDTO.Response> imageResponses = imageRepository.findByProduct_Id(product.getId())
                .stream()
                .map(this::convertToImageResponse)
                .collect(Collectors.toList());
        response.setImages(imageResponses);

        // 🌟 리뷰 개수 및 평균 평점 설정 (추가)
        response.setReviewCount(reviewRepository.countByProductId(product.getId()));
        response.setAverageRating(reviewRepository.findAverageRatingByProductId(product.getId()).orElse(0.0)); // 평균 없으면 0.0

        // 🌟 관심 수 설정 (찜하기 기능이 없으므로 임의의 값 설정)
        // 실제 구현에서는 '찜하기' 엔티티/리포토리를 통해 가져와야 합니다.
        response.setInterestCount(999L); // 예시: 임의의 값 999

        return response;
    }

    private ProductOptionDTO.Response convertToOptionResponse(ProductOption option) {
        ProductOptionDTO.Response response = new ProductOptionDTO.Response();
        response.setId(option.getId());
        response.setSize(option.getSize());
        response.setStockQuantity(option.getStockQuantity());
        response.setAdditionalPrice(option.getAdditionalPrice());
        return response;
    }

    private ProductImageDTO.Response convertToImageResponse(ProductImage image) {
        ProductImageDTO.Response response = new ProductImageDTO.Response();
        response.setId(image.getId());
        response.setImageUrl(image.getImageUrl());
        response.setCreatedAt(image.getCreatedAt());
        return response;
    }

    private ProductCategoryDTO.Response convertToCategoryResponse(ProductCategory category) {
        ProductCategoryDTO.Response response = new ProductCategoryDTO.Response();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setCreatedAt(category.getCreatedAt());
        if (category.getParent() != null) {
            response.setParent(convertToCategoryResponse(category.getParent()));
        }
        return response;
    }
    public ProductDTO.Response getProduct(Integer productId) { // <-- 여기 있습니다!
        return productRepository.findById(productId)
                .map(this::convertToResponse)
                .orElse(null);
    }
}
