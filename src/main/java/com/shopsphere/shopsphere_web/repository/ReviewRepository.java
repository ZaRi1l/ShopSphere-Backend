package com.shopsphere.shopsphere_web.repository;

import com.shopsphere.shopsphere_web.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
    @EntityGraph(attributePaths = {"user", "product"})
    @Override
    Optional<Review> findById(Integer id);
    
    @EntityGraph(attributePaths = {"user", "product"})
    @Query("SELECT r FROM Review r WHERE r.product.id = :productId")
    List<Review> findByProduct_Id(@Param("productId") Integer productId);
    
    @EntityGraph(attributePaths = {"user", "product"})
    @Query("SELECT r FROM Review r WHERE r.user.id = :userId")
    List<Review> findByUser_Id(@Param("userId") String userId);
    // 특정 상품 ID에 대한 리뷰 개수를 세는 메서드
    Long countByProductId(Integer productId);

    // 특정 상품 ID에 대한 평균 평점을 찾는 메서드
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Optional<Double> findAverageRatingByProductId(Integer productId);
    
    // 사용자 ID와 상품 ID로 리뷰 존재 여부 확인
    boolean existsByUser_IdAndProduct_Id(String userId, Integer productId);
}
