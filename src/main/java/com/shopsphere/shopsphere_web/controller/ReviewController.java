package com.shopsphere.shopsphere_web.controller;

import com.shopsphere.shopsphere_web.dto.ReviewDTO;
import com.shopsphere.shopsphere_web.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType; // 추가

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
     * 리뷰 작성 (이미지 파일 포함)
     * consumes = MediaType.MULTIPART_FORM_DATA_VALUE 로 설정하여 multipart 요청을 받습니다.
     */
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Map<String, Object>> createReview(
            @RequestPart("request") ReviewDTO.CreateRequest requestDto, // JSON 데이터 부분
            @RequestPart(value = "reviewImageFile", required = false) MultipartFile reviewImageFile, // 파일 부분
            HttpSession session) { // HttpSession은 필드 주입 대신 파라미터로 받도록 수정 권장
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            // 401 Unauthorized 반환이 더 적절할 수 있습니다.
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        try {
            ReviewDTO.Response review = reviewService.createReview(requestDto, userId, reviewImageFile);
            return ResponseEntity.ok(Map.of(
                "message", "리뷰가 성공적으로 작성되었습니다.",
                "review", review
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
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
