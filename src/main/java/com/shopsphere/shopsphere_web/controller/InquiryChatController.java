    package com.shopsphere.shopsphere_web.controller;

    import com.shopsphere.shopsphere_web.dto.inquiry.InquiryChatDto;
    import com.shopsphere.shopsphere_web.dto.inquiry.InquiryChatRequestDto;
    import com.shopsphere.shopsphere_web.dto.inquiry.InquiryChatRoomDto;
    import com.shopsphere.shopsphere_web.service.InquiryChatService;
    import jakarta.servlet.http.HttpSession;
    import lombok.RequiredArgsConstructor;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;
    import org.springframework.messaging.handler.annotation.DestinationVariable;
    import org.springframework.messaging.handler.annotation.MessageMapping;
    import org.springframework.messaging.handler.annotation.Payload;
    import org.springframework.messaging.simp.SimpMessagingTemplate;
    import org.springframework.stereotype.Controller;
    import org.springframework.web.bind.annotation.*;

    import java.util.List;
    import java.util.Map;

    @Controller
    @RequiredArgsConstructor
    @RequestMapping("/api/inquiry-chats")
    public class InquiryChatController {

        private final InquiryChatService inquiryChatService;
        private final SimpMessagingTemplate messagingTemplate;  // WebSocket 메시징을 위한 템플릿

        // WebSocket을 통한 실시간 채팅 메시지 처리
        @MessageMapping("/inquiry-chat/{roomId}")
        public void handleChatMessage(
                @DestinationVariable Long roomId,
                @Payload InquiryChatRequestDto requestDto) {
            
            System.out.println("WebSocket 메시지 수신 - roomId: " + roomId + ", 요청: " + requestDto);
            
            // 요청에 roomId 설정
            requestDto.setChatRoomId(roomId);
            
            try {
                // 메시지 저장 및 저장된 메시지 정보 반환
                InquiryChatDto message = inquiryChatService.sendMessage(requestDto.getSenderId(), requestDto);
                System.out.println("메시지 저장 성공: " + message);
                
                // 해당 채팅방 구독자에게 메시지 브로드캐스트
                messagingTemplate.convertAndSend("/topic/inquiry-chat/" + roomId, message);
            } catch (Exception e) {
                System.err.println("메시지 처리 중 오류 발생: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // 채팅방 생성 또는 조회
        @ResponseBody
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

        // 채팅 메시지 전송 (REST API - WebSocket과 병행 사용)
        @ResponseBody
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
                
                // WebSocket을 통해서도 메시지 브로드캐스트 (REST API로 전송된 메시지도 실시간으로 전달)
                messagingTemplate.convertAndSend(
                    "/topic/inquiry-chat/" + requestDto.getChatRoomId(), 
                    message
                );
                
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
        @ResponseBody
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
        @ResponseBody
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