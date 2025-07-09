package com.shopsphere.shopsphere_web.dto.inquiry;

import com.shopsphere.shopsphere_web.entity.InquiryChat;
import com.shopsphere.shopsphere_web.entity.InquiryChatRoom;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class InquiryChatRoomDto {
    private Long id;
    private String buyerId;
    private String buyerName;
    private String sellerId;
    private String sellerName;
    private Integer orderItemId;
    private String productName; // 상품 이름
    private String lastMessage; // 마지막 메시지 내용
    private LocalDateTime lastMessageAt; // 마지막 메시지 시간
    private LocalDateTime createdAt;

    public InquiryChatRoomDto(InquiryChatRoom chatRoom) {
        this(chatRoom, null);
    }

    public InquiryChatRoomDto(InquiryChatRoom chatRoom, InquiryChat lastChat) {
        this.id = chatRoom.getId();
        this.buyerId = chatRoom.getBuyer().getId();
        this.buyerName = chatRoom.getBuyer().getName();
        this.sellerId = chatRoom.getSeller().getId();
        this.sellerName = chatRoom.getSeller().getName();
        this.orderItemId = chatRoom.getOrderItemId();
        this.productName = chatRoom.getOrderItem() != null && chatRoom.getOrderItem().getProduct() != null 
            ? chatRoom.getOrderItem().getProduct().getName() 
            : null;
        this.lastMessage = lastChat != null ? lastChat.getMessage() : null;
        this.lastMessageAt = lastChat != null ? lastChat.getSentAt() : null;
        this.createdAt = chatRoom.getCreatedAt();
    }
}
