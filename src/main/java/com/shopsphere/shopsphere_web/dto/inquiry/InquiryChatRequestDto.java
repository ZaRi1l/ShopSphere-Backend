package com.shopsphere.shopsphere_web.dto.inquiry;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InquiryChatRequestDto {
    private Long chatRoomId;
    private String message;
    
    public InquiryChatRequestDto(Long chatRoomId, String message) {
        this.chatRoomId = chatRoomId;
        this.message = message;
    }
}
