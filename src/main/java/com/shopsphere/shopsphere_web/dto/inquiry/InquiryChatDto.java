package com.shopsphere.shopsphere_web.dto.inquiry;

import com.shopsphere.shopsphere_web.entity.InquiryChat;
import com.shopsphere.shopsphere_web.entity.User;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class InquiryChatDto {
    private Long id;
    private Long chatRoomId;
    private String senderId;
    private String senderName;
    private String message;
    private LocalDateTime sentAt;

    public InquiryChatDto(InquiryChat chat) {
        this.id = chat.getId();
        this.chatRoomId = chat.getChatRoom().getId();
        this.senderId = chat.getSender().getId();
        this.senderName = chat.getSender().getName();
        this.message = chat.getMessage();
        this.sentAt = chat.getSentAt();
    }
}
