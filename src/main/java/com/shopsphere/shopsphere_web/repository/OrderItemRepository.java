package com.shopsphere.shopsphere_web.repository;

import com.shopsphere.shopsphere_web.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    List<OrderItem> findByOrder_Id(Integer orderId);
    
    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.product p WHERE oi.product.id IN :productIds " +
           "AND oi.createdAt BETWEEN :startDate AND :endDate")
    List<OrderItem> findByProductIdInAndCreatedAtBetween(
            @Param("productIds") List<Integer> productIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT DISTINCT oi FROM OrderItem oi " +
           "JOIN FETCH oi.product p " +
           "LEFT JOIN FETCH p.images " +
           "WHERE oi.product.id IN :productIds " +
           "AND oi.createdAt BETWEEN :startDate AND :endDate")
    List<OrderItem> findByProductIdInAndCreatedAtBetweenWithImages(
            @Param("productIds") List<Integer> productIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT oi FROM OrderItem oi " +
            "JOIN FETCH oi.product p " +
            "WHERE p.id IN :productIds " +
            "AND oi.createdAt BETWEEN :startDate AND :endDate " +
            "AND (:category IS NULL OR :category = '' OR CAST(p.category AS string) = :category)")
    List<OrderItem> findByProductIdInAndCreatedAtBetweenAndCategory(
            @Param("productIds") List<Integer> productIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("category") String category);
}
