package com.shopsphere.shopsphere_web.service;

import com.shopsphere.shopsphere_web.dto.ProductDTO; // ProductDTO import 확인
import com.shopsphere.shopsphere_web.dto.ReviewDTO;
import com.shopsphere.shopsphere_web.dto.UserDTO;
import com.shopsphere.shopsphere_web.entity.Product; // Product import 확인
import com.shopsphere.shopsphere_web.entity.Review;
import com.shopsphere.shopsphere_web.entity.User;
import com.shopsphere.shopsphere_web.repository.ProductRepository; // ProductRepository import 확인
import com.shopsphere.shopsphere_web.repository.ReviewRepository;
import com.shopsphere.shopsphere_web.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest; // HttpServletRequest import
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService; // FileStorageService 주입
    private final HttpServletRequest httpServletRequest; // HttpServletRequest 주입 (URL 생성용)

    /**
     * 상품 ID로 리뷰 목록 조회
     */
    public List<ReviewDTO.Response> getReviewsByProductId(Integer productId) {
        return reviewRepository.findByProduct_Id(productId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 사용자 ID로 리뷰 목록 조회
     */
    public List<ReviewDTO.Response> getReviewsByUserId(String userId) {
        return reviewRepository.findByUser_Id(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 리뷰 상세 조회
     */
    public ReviewDTO.Response getReviewById(Integer reviewId) {
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
        
        Product product = productRepository.findById(requestDto.getProductId())
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다. ID: " + requestDto.getProductId()));

        // 이미 리뷰를 작성했는지 확인 (동일 사용자가 동일 상품에 대해)
        boolean hasReviewed = reviewRepository.existsByUser_IdAndProduct_Id(userId, requestDto.getProductId());
        if (hasReviewed) {
            throw new RuntimeException("이미 해당 상품에 리뷰를 작성하셨습니다.");
        }

        String reviewImageUrl = null;
        String storedFilePathSegment = null; 

        if (reviewImageFile != null && !reviewImageFile.isEmpty()) {
            // 1. 파일 저장 (예: "productId/userId-productId-uuid.jpg" 형태로 반환됨)
            storedFilePathSegment = fileStorageService.storeReviewImage(reviewImageFile, userId, product.getId());
            
            // 2. 웹 접근 가능 전체 URL 생성
            // 예: "http://localhost:8080/uploads/review_images/1/user1-1-uuid.jpg"
            // String scheme = httpServletRequest.getScheme(); // http 또는 https
            // String serverName = httpServletRequest.getServerName(); // localhost
            // int serverPort = httpServletRequest.getServerPort(); // 8080
            // String contextPath = httpServletRequest.getContextPath(); // 일반적으로 "" (비어 있음)
            
            // String baseServerUrl = scheme + "://" + serverName + ":" + serverPort + contextPath;
            
            reviewImageUrl = "/" + fileStorageService.getBaseUploadUrlSegment() + "/" +
                 fileStorageService.getReviewImageSubDir() + "/" + storedFilePathSegment;

        }

        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(requestDto.getRating())
                .comment(requestDto.getComment())
                .createdAt(LocalDateTime.now())
                .reviewImageUrl(reviewImageUrl) // 저장된 이미지의 전체 웹 URL 설정
                .build();

        Review savedReview = reviewRepository.save(review);
        return convertToDTO(savedReview);
    }

    /**
     * 리뷰 수정
     * (이미지 수정 기능은 현재 미포함. 필요시 파라미터로 MultipartFile 받고 기존 이미지 삭제/새 이미지 저장 로직 추가)
     */
    @Transactional
    public ReviewDTO.Response updateReview(Integer reviewId, ReviewDTO.UpdateRequest requestDto, String userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다. ID: " + reviewId));

        // 리뷰 작성자와 수정 요청자가 동일한지 확인
        if (!review.getUser().getId().equals(userId)) {
            throw new RuntimeException("리뷰를 수정할 권한이 없습니다.");
        }

        // 이미지 수정 로직은 여기에 추가될 수 있습니다.
        // 예를 들어, 새로운 이미지가 제공되면 기존 이미지를 삭제하고 새 이미지를 저장합니다.
        // 만약 이미지를 제거하고 싶다면 review.setReviewImageUrl(null);

        review.setRating(requestDto.getRating());
        review.setComment(requestDto.getComment());
        // review.setUpdatedAt(LocalDateTime.now()); // 수정 시간 필드가 있다면 업데이트

        Review updatedReview = reviewRepository.save(review); // JPA 변경 감지에 의해 명시적 save는 필요 없을 수 있음
        return convertToDTO(updatedReview);
    }

    /**
     * 리뷰 삭제 (첨부 이미지도 함께 삭제)
     */
    @Transactional
    public void deleteReview(Integer reviewId, String userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다. ID: " + reviewId));

        // 리뷰 작성자와 삭제 요청자가 동일한지 확인 (또는 관리자 권한 확인 로직 추가 가능)
        if (!review.getUser().getId().equals(userId)) {
            throw new RuntimeException("리뷰를 삭제할 권한이 없습니다.");
        }

        String fullImageUrl = review.getReviewImageUrl();
        if (fullImageUrl != null && !fullImageUrl.isEmpty()) {
            // 전체 URL에서 FileStorageService가 관리하는 경로 부분만 추출
            // 예: http://localhost:8080/uploads/review_images/1/filename.jpg
            //    -> "1/filename.jpg" (FileStorageService.storeReviewImage의 반환값 형태)
            
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
                // URL 형식이 예상과 다를 경우 로깅 또는 다른 방식으로 처리
                System.err.println("리뷰 이미지 URL에서 파일 경로를 추출할 수 없습니다: " + fullImageUrl);
            }
        }
        reviewRepository.delete(review);
    }

    /**
     * 상품의 평균 평점 조회
     */
    public Double getAverageRatingByProductId(Integer productId) {
        return reviewRepository.findAverageRatingByProductId(productId)
                .orElse(0.0); // 평균 평점이 없으면 0.0 반환
    }

    /**
     * 상품의 리뷰 개수 조회
     */
    public Long getReviewCountByProductId(Integer productId) {
        return reviewRepository.countByProductId(productId);
    }

    /**
     * Review Entity를 ReviewDTO.Response로 변환
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
        response.setReviewImageUrl(review.getReviewImageUrl()); // 이미지 URL 매핑

        // User 정보 설정
        if (review.getUser() != null) {
            UserDTO.Response userResponse = new UserDTO.Response();
            userResponse.setId(review.getUser().getId());
            userResponse.setName(review.getUser().getName());
            // 필요시 다른 사용자 정보도 추가 (예: 프로필 이미지)
            userResponse.setProfileImageUrl(review.getUser().getProfileImageUrl());
            response.setUser(userResponse);
        }
        
        // Product 정보 설정
        if (review.getProduct() != null) {
            ProductDTO.Response productResponse = new ProductDTO.Response();
            productResponse.setId(review.getProduct().getId());
            productResponse.setName(review.getProduct().getName());
            // 필요시 다른 상품 정보도 추가 (예: 대표 이미지)
            // productResponse.setImageUrl(review.getProduct().getImageUrl()); // Product 엔티티에 대표 이미지 URL 필드가 있다면
            // 상품 이미지 리스트를 여기서 채우는 것은 N+1을 유발할 수 있으므로 주의.
            // ProductService를 통해 가져오거나, ReviewRepository에서 Fetch Join으로 가져와야 함.
            // 현재는 ReviewRepository에서 Product를 Fetch Join 하고 있으므로, product.getImages() 접근 가능
            if (review.getProduct().getImages() != null && !review.getProduct().getImages().isEmpty()) {
                // ProductDTO.Response 에 images 필드가 List<ProductImageDTO> 타입이라고 가정
                // ProductImage를 ProductImageDTO로 변환하는 로직 필요
                // productResponse.setImages(review.getProduct().getImages().stream().map(this::convertToProductImageDTO).collect(Collectors.toList()));
            }
            response.setProduct(productResponse);
        }
        
        return response;
    }

    // ProductImage -> ProductImageDTO 변환 메소드 (필요시 추가)
    /*
    private ProductImageDTO convertToProductImageDTO(ProductImage image) {
        if (image == null) return null;
        ProductImageDTO dto = new ProductImageDTO();
        dto.setId(image.getId());
        dto.setImageUrl(image.getImageUrl());
        dto.setDisplayOrder(image.getDisplayOrder());
        // dto.setIsRepresentative(image.getIsRepresentative());
        return dto;
    }
    */
}