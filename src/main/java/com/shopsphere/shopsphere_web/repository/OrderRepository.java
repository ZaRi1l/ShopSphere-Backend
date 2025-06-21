package com.shopsphere.shopsphere_web.repository;

import com.shopsphere.shopsphere_web.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.user u " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.product p " +
           "LEFT JOIN FETCH p.category " + // Product의 Category도 Eager하게 가져오기
           "LEFT JOIN FETCH p.user " +     // Product의 판매자(User)도 Eager하게 가져오기
           "LEFT JOIN FETCH oi.productOption po " +
           "WHERE o.user.id = :userId")
    List<Order> findByUser_IdWithDetails(@Param("userId") String userId);
    
    List<Order> findByUser_Id(String userId);
    // findById는 JpaRepository에서 이미 제공하므로 제거
}
