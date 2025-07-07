package com.shopsphere.shopsphere_web.repository;

import com.shopsphere.shopsphere_web.entity.InquiryChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InquiryChatRoomRepository extends JpaRepository<InquiryChatRoom, Long> {
    // 구매자 ID와 판매자 ID, 주문 항목 ID로 채팅방 조회
    @Query("SELECT r FROM InquiryChatRoom r WHERE r.buyer.id = :buyerId AND r.seller.id = :sellerId AND r.orderItem.id = :orderItemId")
    Optional<InquiryChatRoom> findByBuyerIdAndSellerIdAndOrderItemId(
        @Param("buyerId") String buyerId, 
        @Param("sellerId") String sellerId, 
        @Param("orderItemId") Integer orderItemId
    );
    
    // 구매자 ID로 모든 채팅방 조회
    @Query("SELECT r FROM InquiryChatRoom r WHERE r.buyer.id = :buyerId")
    List<InquiryChatRoom> findByBuyerId(@Param("buyerId") String buyerId);
    
    // 판매자 ID로 모든 채팅방 조회
    @Query("SELECT r FROM InquiryChatRoom r WHERE r.seller.id = :sellerId")
    List<InquiryChatRoom> findBySellerId(@Param("sellerId") String sellerId);
    
    // 주문 항목 ID로 채팅방 조회
    @Query("SELECT r FROM InquiryChatRoom r WHERE r.orderItem.id = :orderItemId")
    List<InquiryChatRoom> findByOrderItemId(@Param("orderItemId") Integer orderItemId);
}
