package com.shopsphere.shopsphere_web.dto.inquiry;

import com.shopsphere.shopsphere_web.entity.InquiryChatRoom;
import com.shopsphere.shopsphere_web.entity.Product;
import com.shopsphere.shopsphere_web.entity.User;
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
    private LocalDateTime createdAt;

    public InquiryChatRoomDto(InquiryChatRoom chatRoom) {
        this.id = chatRoom.getId();
        this.buyerId = chatRoom.getBuyer().getId();
        this.buyerName = chatRoom.getBuyer().getName();
        this.sellerId = chatRoom.getSeller().getId();
        this.sellerName = chatRoom.getSeller().getName();
        this.orderItemId = chatRoom.getOrderItemId();
        this.createdAt = chatRoom.getCreatedAt();
    }
}
