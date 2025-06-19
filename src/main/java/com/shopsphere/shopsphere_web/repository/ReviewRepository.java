package com.shopsphere.shopsphere_web.repository;

import com.shopsphere.shopsphere_web.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
    List<Review> findByProduct_Id(Integer productId);
    List<Review> findByUser_Id(String userId);
    // 특정 상품 ID에 대한 리뷰 개수를 세는 메서드
    Long countByProductId(Integer productId);

    // 특정 상품 ID에 대한 평균 평점을 찾는 메서드
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Optional<Double> findAverageRatingByProductId(Integer productId);
}
