package com.shopsphere.shopsphere_web.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "inquiry_chat_room")
public class InquiryChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_chat_room_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false, referencedColumnName = "user_id")
    private User buyer; // 구매자 (문의한 사람)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false, referencedColumnName = "user_id")
    private User seller; // 판매자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false, referencedColumnName = "order_item_id")
    private OrderItem orderItem; // 어떤 주문 항목에 대한 문의인지

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public InquiryChatRoom(User buyer, User seller, OrderItem orderItem) {
        if (buyer == null || seller == null || orderItem == null) {
            throw new IllegalArgumentException("구매자, 판매자, 주문 항목 정보는 필수입니다.");
        }
        this.buyer = buyer;
        this.seller = seller;
        this.orderItem = orderItem;
    }

    // Helper method to get buyer ID as String
    public String getBuyerId() {
        return buyer != null ? buyer.getId() : null;
    }

    // Helper method to get seller ID as String
    public String getSellerId() {
        return seller != null ? seller.getId() : null;
    }

    // Helper method to get order item ID as Integer
    public Integer getOrderItemId() {
        return orderItem != null ? orderItem.getId() : null;
    }

    // Helper method to get order item
    public OrderItem getOrderItem() {
        return orderItem;
    }
}