package com.shopsphere.shopsphere_web.controller;

import com.shopsphere.shopsphere_web.dto.inquiry.InquiryChatDto;
import com.shopsphere.shopsphere_web.dto.inquiry.InquiryChatRequestDto;
import com.shopsphere.shopsphere_web.dto.inquiry.InquiryChatRoomDto;
import com.shopsphere.shopsphere_web.service.InquiryChatService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inquiry-chats")
public class InquiryChatController {

    private final InquiryChatService inquiryChatService;

    // 채팅방 생성 또는 조회
    @PostMapping("/rooms")
    public ResponseEntity<?> getOrCreateChatRoom(
            @RequestParam Integer orderItemId,
            HttpSession session) {
        
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인이 필요합니다."));
        }

        try {
            InquiryChatRoomDto chatRoom = inquiryChatService.getOrCreateChatRoom(userId, orderItemId);
            return ResponseEntity.ok(chatRoom);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "채팅방 생성 또는 조회 중 오류가 발생했습니다.", "error", e.getMessage()));
        }
    }

    // 채팅 메시지 전송
    @PostMapping("/messages")
    public ResponseEntity<?> sendMessage(
            @RequestBody InquiryChatRequestDto requestDto,
            HttpSession session) {
        
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인이 필요합니다."));
        }

        try {
            InquiryChatDto message = inquiryChatService.sendMessage(userId, requestDto);
            return ResponseEntity.ok(message);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "메시지 전송 중 오류가 발생했습니다.", "error", e.getMessage()));
        }
    }

    // 채팅방의 모든 메시지 조회
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<?> getChatMessages(
            @PathVariable Long roomId,
            HttpSession session) {
        
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인이 필요합니다."));
        }

        try {
            List<InquiryChatDto> messages = inquiryChatService.getChatMessages(roomId, userId);
            return ResponseEntity.ok(messages);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "메시지 조회 중 오류가 발생했습니다.", "error", e.getMessage()));
        }
    }

    // 사용자의 모든 채팅방 조회
    @GetMapping("/rooms")
    public ResponseEntity<?> getUserChatRooms(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인이 필요합니다."));
        }

        try {
            List<InquiryChatRoomDto> chatRooms = inquiryChatService.getUserChatRooms(userId);
            return ResponseEntity.ok(chatRooms);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "채팅방 목록 조회 중 오류가 발생했습니다.", "error", e.getMessage()));
        }
    }
}
