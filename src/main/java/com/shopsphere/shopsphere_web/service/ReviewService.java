package com.shopsphere.shopsphere_web.service;

import com.shopsphere.shopsphere_web.dto.ReviewDTO;
import com.shopsphere.shopsphere_web.dto.UserDTO;
import com.shopsphere.shopsphere_web.dto.ProductDTO;
import com.shopsphere.shopsphere_web.entity.Review;
import com.shopsphere.shopsphere_web.entity.User;
import com.shopsphere.shopsphere_web.entity.Product;
import com.shopsphere.shopsphere_web.repository.ReviewRepository;
import com.shopsphere.shopsphere_web.repository.UserRepository;
import com.shopsphere.shopsphere_web.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다."));
        return convertToDTO(review);
    }

    /**
     * 리뷰 작성
     */
    @Transactional
    public ReviewDTO.Response createReview(ReviewDTO.CreateRequest request, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));

        // 이미 리뷰를 작성했는지 확인
        boolean hasReviewed = reviewRepository.existsByUser_IdAndProduct_Id(userId, request.getProductId());
        if (hasReviewed) {
            throw new RuntimeException("이미 해당 상품에 리뷰를 작성하셨습니다.");
        }

        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(request.getRating())
                .comment(request.getComment())
                .createdAt(LocalDateTime.now())
                .build();

        Review savedReview = reviewRepository.save(review);
        return convertToDTO(savedReview);
    }

    /**
     * 리뷰 수정
     */
    @Transactional
    public ReviewDTO.Response updateReview(Integer reviewId, ReviewDTO.UpdateRequest request, String userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다."));

        // 리뷰 작성자와 수정 요청자가 동일한지 확인
        if (!review.getUser().getId().equals(userId)) {
            throw new RuntimeException("리뷰를 수정할 권한이 없습니다.");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        return convertToDTO(review);
    }

    /**
     * 리뷰 삭제
     */
    @Transactional
    public void deleteReview(Integer reviewId, String userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다."));

        // 리뷰 작성자와 삭제 요청자가 동일한지 확인 (또는 관리자인지 확인)
        if (!review.getUser().getId().equals(userId)) {
            throw new RuntimeException("리뷰를 삭제할 권한이 없습니다.");
        }

        reviewRepository.delete(review);
    }

    /**
     * 상품의 평균 평점 조회
     */
    public Double getAverageRatingByProductId(Integer productId) {
        return reviewRepository.findAverageRatingByProductId(productId)
                .orElse(0.0);
    }

    /**
     * 상품의 리뷰 개수 조회
     */
    public Long getReviewCountByProductId(Integer productId) {
        return reviewRepository.countByProductId(productId);
    }

    /**
     * Entity를 DTO로 변환
     */
    private ReviewDTO.Response convertToDTO(Review review) {
        ReviewDTO.Response response = new ReviewDTO.Response();
        response.setId(review.getId());
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setCreatedAt(review.getCreatedAt());
        
        // User 정보 설정 (기본 정보만)
        if (review.getUser() != null) {
            UserDTO.Response userResponse = new UserDTO.Response();
            userResponse.setId(review.getUser().getId());
            userResponse.setName(review.getUser().getName());
            response.setUser(userResponse);
        }
        
        // Product 정보 설정 (기본 정보만)
        if (review.getProduct() != null) {
            ProductDTO.Response productResponse = new ProductDTO.Response();
            productResponse.setId(review.getProduct().getId());
            productResponse.setName(review.getProduct().getName());
            response.setProduct(productResponse);
        }
        
        return response;
    }
}
