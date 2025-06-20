package com.shopsphere.shopsphere_web.controller;

import com.shopsphere.shopsphere_web.dto.ReviewDTO;
import com.shopsphere.shopsphere_web.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final HttpSession session;

    /**
     * 상품별 리뷰 목록 조회
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReviewDTO.Response>> getReviewsByProductId(
            @PathVariable Integer productId) {
        List<ReviewDTO.Response> reviews = reviewService.getReviewsByProductId(productId);
        return ResponseEntity.ok(reviews);
    }

    /**
     * 내가 작성한 리뷰 목록 조회
     */
    @GetMapping("/my-reviews")
    public ResponseEntity<List<ReviewDTO.Response>> getMyReviews() {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }
        List<ReviewDTO.Response> reviews = reviewService.getReviewsByUserId(userId);
        return ResponseEntity.ok(reviews);
    }

    /**
     * 리뷰 상세 조회
     */
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewDTO.Response> getReviewById(
            @PathVariable Integer reviewId) {
        ReviewDTO.Response review = reviewService.getReviewById(reviewId);
        return ResponseEntity.ok(review);
    }

    /**
     * 리뷰 작성
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createReview(
            @RequestBody ReviewDTO.CreateRequest request) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }
        ReviewDTO.Response review = reviewService.createReview(request, userId);
        return ResponseEntity.ok(Map.of(
            "message", "리뷰가 성공적으로 작성되었습니다.",
            "review", review
        ));
    }

    /**
     * 리뷰 수정
     */
    @PutMapping("/{reviewId}")
    public ResponseEntity<Map<String, Object>> updateReview(
            @PathVariable Integer reviewId,
            @RequestBody ReviewDTO.UpdateRequest request) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }
        ReviewDTO.Response updatedReview = reviewService.updateReview(reviewId, request, userId);
        return ResponseEntity.ok(Map.of(
            "message", "리뷰가 성공적으로 수정되었습니다.",
            "review", updatedReview
        ));
    }

    /**
     * 리뷰 삭제
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Map<String, String>> deleteReview(@PathVariable Integer reviewId) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.ok(Map.of("message", "리뷰가 성공적으로 삭제되었습니다."));
    }

    /**
     * 상품의 평균 평점 조회
     */
    @GetMapping("/product/{productId}/average-rating")
    public ResponseEntity<Double> getAverageRating(@PathVariable Integer productId) {
        Double averageRating = reviewService.getAverageRatingByProductId(productId);
        return ResponseEntity.ok(averageRating);
    }

    /**
     * 상품의 리뷰 개수 조회
     */
    @GetMapping("/product/{productId}/count")
    public ResponseEntity<Long> getReviewCount(@PathVariable Integer productId) {
        Long count = reviewService.getReviewCountByProductId(productId);
        return ResponseEntity.ok(count);
    }
}
