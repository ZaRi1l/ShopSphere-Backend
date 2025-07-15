// 파일 경로: com/shopsphere/shopsphere_web/repository/OrderRepository.java
package com.shopsphere.shopsphere_web.repository;

import com.shopsphere.shopsphere_web.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    /**
     * 특정 사용자의 모든 주문 목록을 관련 엔티티와 함께 한 번의 쿼리로 가져옵니다.
     * (N+1 문제 해결을 위한 Fetch Join)
     * 주문 목록 조회 시 사용됩니다.
     */
    @Query("SELECT DISTINCT o FROM Order o " +
            "JOIN FETCH o.user u " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "LEFT JOIN FETCH oi.product p " +
            "LEFT JOIN FETCH p.user " + // 상품 판매자 정보
            "LEFT JOIN FETCH oi.productOption " +
            "WHERE o.user.id = :userId " +
            "ORDER BY o.orderDate DESC") // 최신 주문 순으로 정렬
    List<Order> findByUser_IdWithDetails(@Param("userId") String userId);

    /**
     * transactionId(토스페이먼츠용 주문 ID)를 사용하여 특정 주문 하나를
     * 관련 엔티티와 함께 한 번의 쿼리로 가져옵니다.
     * 결제 승인(confirmTossPayment) 시 사용됩니다.
     */
    @Query("SELECT o FROM Order o " +
            "JOIN FETCH o.user u " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "LEFT JOIN FETCH oi.product p " +
            "LEFT JOIN FETCH p.user " + // 상품 판매자 정보
            "LEFT JOIN FETCH oi.productOption " +
            "WHERE o.transactionId = :transactionId")
    Optional<Order> findByTransactionIdWithDetails(@Param("transactionId") String transactionId);

    /**
     * 결제 승인 로직의 검증 단계에서만 사용됩니다.
     * 불필요한 join 없이 빠르게 주문 존재 여부만 확인합니다.
     */
    Optional<Order> findByTransactionId(String transactionId);

    /**
     * 특정 사용자의 주문 목록을 간단히 조회할 때 사용됩니다. (Fetch Join 없음)
     * 이 메소드는 현재 getUserOrders에서 사용되지 않으므로,
     * 만약 다른 곳에서 사용하지 않는다면 삭제해도 무방합니다.
     */
    List<Order> findByUser_Id(String userId);

}
