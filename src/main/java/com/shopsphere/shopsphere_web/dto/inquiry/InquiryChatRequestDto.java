package com.shopsphere.shopsphere_web.dto.inquiry;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InquiryChatRequestDto {
    private Long chatRoomId;
    private String message;
    private String senderId;  // 메시지 발신자 ID
    
    public InquiryChatRequestDto(Long chatRoomId, String message, String senderId) {
        this.chatRoomId = chatRoomId;
        this.message = message;
        this.senderId = senderId;
    }
}
