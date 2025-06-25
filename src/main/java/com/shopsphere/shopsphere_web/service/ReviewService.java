package com.shopsphere.shopsphere_web.service;

import com.shopsphere.shopsphere_web.dto.*; // 모든 필요한 DTO import 가정
import com.shopsphere.shopsphere_web.entity.*; // 모든 필요한 Entity import 가정
import com.shopsphere.shopsphere_web.repository.ProductRepository;
import com.shopsphere.shopsphere_web.repository.ReviewRepository;
import com.shopsphere.shopsphere_web.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections; // Collections.emptyList() 사용을 위해
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository; // Product 정보 접근 시 필요할 수 있음
    private final FileStorageService fileStorageService;
    private final HttpServletRequest httpServletRequest;

    /**
     * 상품 ID로 리뷰 목록 조회
     */
    public List<ReviewDTO.Response> getReviewsByProductId(Integer productId) {
        // Repository에서 Product.images 까지 Fetch Join 하도록 EntityGraph 수정 권장
        return reviewRepository.findByProduct_Id(productId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 사용자 ID로 리뷰 목록 조회
     */
    public List<ReviewDTO.Response> getReviewsByUserId(String userId) {
        // Repository에서 Product.images 까지 Fetch Join 하도록 EntityGraph 수정 권장
        return reviewRepository.findByUser_Id(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 리뷰 상세 조회
     */
    public ReviewDTO.Response getReviewById(Integer reviewId) {
        // Repository에서 Product.images 까지 Fetch Join 하도록 EntityGraph 수정 권장
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다. ID: " + reviewId));
        return convertToDTO(review);
    }

    /**
     * 리뷰 작성 (이미지 파일 포함)
     */
    @Transactional
    public ReviewDTO.Response createReview(ReviewDTO.CreateRequest requestDto, String userId, MultipartFile reviewImageFile) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. ID: " + userId));
        
        // Product 엔티티를 조회할 때, 연관된 images, category, seller 등을 함께 가져오도록 ProductRepository의 findById 수정 권장 (EntityGraph 등)
        Product product = productRepository.findById(requestDto.getProductId())
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다. ID: " + requestDto.getProductId()));

        boolean hasReviewed = reviewRepository.existsByUser_IdAndProduct_Id(userId, requestDto.getProductId());
        if (hasReviewed) {
            throw new RuntimeException("이미 해당 상품에 리뷰를 작성하셨습니다.");
        }

        String reviewImageUrl = null;
        if (reviewImageFile != null && !reviewImageFile.isEmpty()) {
            String storedFilePathSegment = fileStorageService.storeReviewImage(reviewImageFile, userId, product.getId());
            reviewImageUrl = "/" + fileStorageService.getBaseUploadUrlSegment() + "/" +
                             fileStorageService.getReviewImageSubDir() + "/" + storedFilePathSegment;
        }

        Review review = Review.builder()
                .user(user)
                .product(product) // Product 엔티티 자체를 연결
                .rating(requestDto.getRating())
                .comment(requestDto.getComment())
                .createdAt(LocalDateTime.now())
                .reviewImageUrl(reviewImageUrl)
                .build();

        Review savedReview = reviewRepository.save(review);
        // 저장 후 반환되는 DTO에는 Product의 상세 정보가 포함되어야 함
        return convertToDTO(savedReview);
    }

    /**
     * 리뷰 수정
     */
    @Transactional
    public ReviewDTO.Response updateReview(Integer reviewId, ReviewDTO.UpdateRequest requestDto, String userId) {
        Review review = reviewRepository.findById(reviewId) // EntityGraph로 product.images 등 가져오도록
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다. ID: " + reviewId));

        if (!review.getUser().getId().equals(userId)) {
            throw new RuntimeException("리뷰를 수정할 권한이 없습니다.");
        }

        review.setRating(requestDto.getRating());
        review.setComment(requestDto.getComment());
        // 이미지 수정 로직은 여기에 추가 (필요시)

        Review updatedReview = reviewRepository.save(review);
        return convertToDTO(updatedReview);
    }

    /**
     * 리뷰 삭제 (첨부 이미지도 함께 삭제)
     */
    @Transactional
    public void deleteReview(Integer reviewId, String userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다. ID: " + reviewId));

        if (!review.getUser().getId().equals(userId)) {
            throw new RuntimeException("리뷰를 삭제할 권한이 없습니다.");
        }

        String fullImageUrl = review.getReviewImageUrl();
        if (fullImageUrl != null && !fullImageUrl.isEmpty()) {
            String scheme = httpServletRequest.getScheme();
            String serverName = httpServletRequest.getServerName();
            int serverPort = httpServletRequest.getServerPort();
            String contextPath = httpServletRequest.getContextPath();
            String baseServerUrl = scheme + "://" + serverName + ":" + serverPort + contextPath;
            String filePathPrefix = baseServerUrl + "/" + fileStorageService.getBaseUploadUrlSegment() + "/" +
                                    fileStorageService.getReviewImageSubDir() + "/";

            if (fullImageUrl.startsWith(filePathPrefix)) {
                String filePathSegment = fullImageUrl.substring(filePathPrefix.length());
                fileStorageService.deleteReviewImage(filePathSegment);
            } else {
                System.err.println("리뷰 이미지 URL에서 파일 경로를 추출할 수 없습니다: " + fullImageUrl);
            }
        }
        reviewRepository.delete(review);
    }

    /**
     * 상품의 평균 평점 조회
     */
    public Double getAverageRatingByProductId(Integer productId) {
        return reviewRepository.findAverageRatingByProductId(productId).orElse(0.0);
    }

    /**
     * 상품의 리뷰 개수 조회
     */
    public Long getReviewCountByProductId(Integer productId) {
        return reviewRepository.countByProductId(productId);
    }

    // --- DTO 변환 헬퍼 메소드들 ---

    private UserDTO.Response convertToUserDTOResponse(User user) {
        if (user == null) return null;
        UserDTO.Response userResponse = new UserDTO.Response();
        userResponse.setId(user.getId());
        userResponse.setName(user.getName());
        userResponse.setProfileImageUrl(user.getProfileImageUrl()); // User 엔티티에 getProfileImageUrl()이 있다고 가정
        // UserDTO.Response에 정의된 다른 필드들도 채워줍니다.
        // userResponse.setEmail(user.getEmail());
        return userResponse;
    }

    private ProductImageDTO convertToProductImageDTO(ProductImage imageEntity) {
        if (imageEntity == null) return null;
        ProductImageDTO dto = new ProductImageDTO();
        dto.setId(imageEntity.getId());
        dto.setImageUrl(imageEntity.getImageUrl());
        dto.setDisplayOrder(imageEntity.getDisplayOrder());
        dto.setCreatedAt(imageEntity.getCreatedAt());
        // dto.setIsRepresentative(imageEntity.getIsRepresentative()); // ProductImage 엔티티에 isRepresentative 필드가 있다면
        return dto;
    }

    private ProductCategoryDTO.Response convertToProductCategoryDTOResponse(ProductCategory categoryEntity) {
        if (categoryEntity == null) return null;
        ProductCategoryDTO.Response dto = new ProductCategoryDTO.Response();
        dto.setId(categoryEntity.getId());
        dto.setName(categoryEntity.getName());
        // ProductCategoryDTO.Response에 정의된 다른 필드들도 채워줍니다.
        // dto.setParentCategoryId(categoryEntity.getParent() != null ? categoryEntity.getParent().getId() : null);
        dto.setCreatedAt(categoryEntity.getCreatedAt()); // ProductCategory 엔티티에 createdAt 필드가 있다고 가정
        return dto;
    }
    
    private ProductOptionDTO.Response convertToProductOptionDTOResponse(ProductOption optionEntity) {
        if (optionEntity == null) return null;
        ProductOptionDTO.Response dto = new ProductOptionDTO.Response();
        dto.setId(optionEntity.getId());
        dto.setSize(optionEntity.getSize()); // 또는 optionName
        dto.setStockQuantity(optionEntity.getStockQuantity());
        dto.setAdditionalPrice(optionEntity.getAdditionalPrice());
        // ProductOptionDTO.Response에 정의된 다른 필드들도 채워줍니다.
        return dto;
    }


    /**
     * Review Entity를 ReviewDTO.Response로 변환 (상품 이미지 포함)
     */
    private ReviewDTO.Response convertToDTO(Review review) {
        if (review == null) {
            return null;
        }
        ReviewDTO.Response response = new ReviewDTO.Response();
        response.setId(review.getId());
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setCreatedAt(review.getCreatedAt());
        response.setReviewImageUrl(review.getReviewImageUrl());

        // User 정보 설정
        if (review.getUser() != null) {
            response.setUser(convertToUserDTOResponse(review.getUser()));
        }
        
        // Product 정보 설정
        if (review.getProduct() != null) {
            Product productEntity = review.getProduct();
            ProductDTO.Response productResponse = new ProductDTO.Response();
            
            productResponse.setId(productEntity.getId());
            productResponse.setName(productEntity.getName());
            productResponse.setDescription(productEntity.getDescription()); // Product 엔티티에 getDescription() 있다고 가정
            productResponse.setPrice(productEntity.getPrice());             // Product 엔티티에 getPrice() 있다고 가정
            productResponse.setStockQuantity(productEntity.getStockQuantity()); // Product 엔티티에 getStockQuantity() 있다고 가정
            productResponse.setCreatedAt(productEntity.getCreatedAt());       // Product 엔티티에 getCreatedAt() 있다고 가정
            productResponse.setSalesVolume(productEntity.getSalesVolume());   // Product 엔티티에 getSalesVolume() 있다고 가정

            // Category 정보 설정
            if (productEntity.getCategory() != null) {
                productResponse.setCategory(convertToProductCategoryDTOResponse(productEntity.getCategory()));
            }

            // Seller 정보 설정 (Product 엔티티의 판매자 필드가 user라고 가정)
            if (productEntity.getUser() != null) {
                productResponse.setSeller(convertToUserDTOResponse(productEntity.getUser()));
            }
            
            // Product Images 정보 설정 (핵심 수정 부분)
            if (productEntity.getImages() != null && !productEntity.getImages().isEmpty()) {
                List<ProductImageDTO> imageDTOs = productEntity.getImages().stream()
                    .map(this::convertToProductImageDTO)
                    .collect(Collectors.toList());
                productResponse.setImages(imageDTOs);
            } else {
                productResponse.setImages(Collections.emptyList()); // 이미지가 없을 경우 빈 리스트 설정
            }

            // Product Options 정보 설정
            if (productEntity.getOptions() != null && !productEntity.getOptions().isEmpty()) {
                List<ProductOptionDTO.Response> optionDTOs = productEntity.getOptions().stream()
                    .map(this::convertToProductOptionDTOResponse)
                    .collect(Collectors.toList());
                productResponse.setOptions(optionDTOs);
            } else {
                productResponse.setOptions(Collections.emptyList());
            }

            // 리뷰 평균 및 개수 (Product 엔티티에 이 필드들이 이미 계산되어 있다면 사용, 없다면 여기서 계산하지 않도록 주의)
            // ProductDTO.Response에 averageRating, reviewCount 필드가 정의되어 있으므로, Product 엔티티에서 가져오거나 ProductService를 통해 가져온 값을 사용해야 함.
            // 여기서는 Product 엔티티에 이미 해당 값이 있다고 가정하고 매핑합니다. (N+1 방지를 위해)
            // 만약 Product 엔티티에 없다면, ProductDTO.Response에서 이 필드들을 제거하거나,
            // ReviewService에서 이 값을 채우지 않도록 하고, ProductService에서 채우도록 해야합니다.
            // 또는, DTO 프로젝션 시점에서 계산하도록 합니다.
            // 여기서는 Product 엔티티에 getter가 있다고 가정하고 채웁니다.
            // 실제로는 productEntity.getAverageRating(), productEntity.getReviewCount() 와 같이 접근해야 합니다.
            // 만약 Product 엔티티에 해당 필드/getter가 없다면, 다음 두 줄은 주석 처리하거나 ProductService에서 처리하도록 위임해야 합니다.
            // productResponse.setAverageRating(reviewRepository.findAverageRatingByProductId(productEntity.getId()).orElse(0.0)); // N+1 유발 가능성 매우 높음 (주석 처리 권장)
            // productResponse.setReviewCount(reviewRepository.countByProductId(productEntity.getId())); // N+1 유발 가능성 매우 높음 (주석 처리 권장)
            // ==> ProductDTO.Response에 이미 이 필드들이 있으므로 Product 엔티티에서 해당 값을 가져와야 합니다.
            // Product 엔티티에 averageRating과 reviewCount 필드가 있고, 적절히 업데이트 된다고 가정.
            
            response.setProduct(productResponse);
        }
        
        return response;
    }
}