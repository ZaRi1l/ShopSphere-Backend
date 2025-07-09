package com.shopsphere.shopsphere_web.service;

import com.shopsphere.shopsphere_web.dto.inquiry.InquiryChatDto;
import com.shopsphere.shopsphere_web.dto.inquiry.InquiryChatRequestDto;
import com.shopsphere.shopsphere_web.dto.inquiry.InquiryChatRoomDto;
import com.shopsphere.shopsphere_web.entity.InquiryChat;
import com.shopsphere.shopsphere_web.entity.InquiryChatRoom;
import com.shopsphere.shopsphere_web.entity.OrderItem;
import com.shopsphere.shopsphere_web.entity.Product;
import com.shopsphere.shopsphere_web.entity.User;
import com.shopsphere.shopsphere_web.repository.InquiryChatRepository;
import com.shopsphere.shopsphere_web.repository.InquiryChatRoomRepository;
import com.shopsphere.shopsphere_web.repository.OrderItemRepository;
import com.shopsphere.shopsphere_web.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InquiryChatService {

    private final InquiryChatRoomRepository chatRoomRepository;
    private final InquiryChatRepository chatRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    // 채팅방 생성 또는 조회
    @Transactional
    public InquiryChatRoomDto getOrCreateChatRoom(String buyerId, Integer orderItemId) {
        try {
            // 주문 항목 조회
            OrderItem orderItem = orderItemRepository.findById(orderItemId)
                    .orElseThrow(() -> new IllegalArgumentException("주문 항목을 찾을 수 없습니다."));

            // 주문 항목에서 상품과 판매자 정보 가져오기
            Product product = orderItem.getProduct();
            User seller = product.getUser();
            String sellerId = seller.getId();

            // 이미 존재하는 채팅방이 있는지 확인
            return chatRoomRepository.findByBuyerIdAndSellerIdAndOrderItemId(buyerId, sellerId, orderItemId)
                    .map(InquiryChatRoomDto::new)
                    .orElseGet(() -> createNewChatRoom(buyerId, sellerId, orderItemId));
        } catch (Exception e) {
            log.error("Error in getOrCreateChatRoom: {}", e.getMessage(), e);
            throw new IllegalArgumentException("채팅방을 생성하거나 조회하는 중 오류가 발생했습니다.");
        }
    }

    // 새 채팅방 생성
    @Transactional
    public InquiryChatRoomDto createNewChatRoom(String buyerId, String sellerId, Integer orderItemId) {
        try {
            if (buyerId == null || buyerId.isEmpty() || sellerId == null || sellerId.isEmpty() || orderItemId == null) {
                throw new IllegalArgumentException("구매자 ID, 판매자 ID, 주문 항목 ID는 필수입니다.");
            }

            User buyer = userRepository.findById(buyerId)
                    .orElseThrow(() -> new IllegalArgumentException("구매자를 찾을 수 없습니다."));
            
            User seller = userRepository.findById(sellerId)
                    .orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다."));
            
            OrderItem orderItem = orderItemRepository.findById(orderItemId)
                    .orElseThrow(() -> new IllegalArgumentException("주문 항목을 찾을 수 없습니다."));

            // 이미 존재하는 채팅방이 없는지 다시 확인 (동시 요청 방지)
            Optional<InquiryChatRoom> existingRoom = chatRoomRepository
                    .findByBuyerIdAndSellerIdAndOrderItemId(buyerId, sellerId, orderItemId);
            
            if (existingRoom.isPresent()) {
                return new InquiryChatRoomDto(existingRoom.get());
            }

            InquiryChatRoom chatRoom = InquiryChatRoom.builder()
                    .buyer(buyer)
                    .seller(seller)
                    .orderItem(orderItem)
                    .build();

            return new InquiryChatRoomDto(chatRoomRepository.save(chatRoom));
        } catch (Exception e) {
            log.error("Error in createNewChatRoom: {}", e.getMessage(), e);
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    // 채팅 메시지 전송
    @Transactional
    public InquiryChatDto sendMessage(String senderId, InquiryChatRequestDto requestDto) {
        try {
            if (senderId == null || senderId.isEmpty() || requestDto == null) {
                throw new IllegalArgumentException("요청 정보가 올바르지 않습니다.");
            }

            if (requestDto.getMessage() == null || requestDto.getMessage().trim().isEmpty()) {
                throw new IllegalArgumentException("메시지 내용은 필수입니다.");
            }

            User sender = userRepository.findById(senderId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            
            InquiryChatRoom chatRoom = chatRoomRepository.findById(requestDto.getChatRoomId())
                    .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
            
            // 권한 확인: 채팅방의 구매자 또는 판매자만 메시지를 보낼 수 있음
            if (!chatRoom.getBuyer().getId().equals(senderId) && 
                !chatRoom.getSeller().getId().equals(senderId)) {
                throw new SecurityException("이 채팅방에 메시지를 보낼 권한이 없습니다.");
            }


            InquiryChat chat = InquiryChat.builder()
                    .chatRoom(chatRoom)
                    .sender(sender)
                    .message(requestDto.getMessage())
                    .build();

            // 수동으로 시간 설정
            chat.setSentAt(LocalDateTime.now());

            return new InquiryChatDto(chatRepository.save(chat));
        } catch (IllegalArgumentException | SecurityException e) {
            throw e; // 이미 처리된 예외는 그대로 전달
        } catch (Exception e) {
            log.error("Error in sendMessage: {}", e.getMessage(), e);
            throw new RuntimeException("메시지 전송 중 오류가 발생했습니다.", e);
        }
    }

    // 채팅방의 모든 메시지 조회
    @Transactional(readOnly = true)
    public List<InquiryChatDto> getChatMessages(Long chatRoomId, String userId) {
        try {
            if (chatRoomId == null || userId == null || userId.isEmpty()) {
                throw new IllegalArgumentException("채팅방 ID와 사용자 ID는 필수입니다.");
            }

            InquiryChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                    .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
            
            // 권한 확인: 채팅방의 구매자 또는 판매자만 메시지를 조회할 수 있음
            if (!chatRoom.getBuyer().getId().equals(userId) && 
                !chatRoom.getSeller().getId().equals(userId)) {
                throw new SecurityException("이 채팅방의 메시지를 볼 권한이 없습니다.");
            }

            return chatRepository.findByChatRoomIdOrderBySentAtAsc(chatRoomId).stream()
                    .map(InquiryChatDto::new)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException | SecurityException e) {
            throw e; // 이미 처리된 예외는 그대로 전달
        } catch (Exception e) {
            log.error("Error in getChatMessages: {}", e.getMessage(), e);
            throw new RuntimeException("메시지 조회 중 오류가 발생했습니다.", e);
        }
    }

    // 사용자의 모든 채팅방 조회 (구매자 또는 판매자 기준)
    // WebSocket을 통해 메시지 전송을 위한 메서드 (기존 sendMessage와 유사하지만 senderId를 requestDto에서 가져옴)
    @Transactional
    public InquiryChatDto sendMessage(InquiryChatRequestDto requestDto) {
        try {
            if (requestDto == null || requestDto.getSenderId() == null || requestDto.getSenderId().isEmpty()) {
                throw new IllegalArgumentException("요청 정보가 올바르지 않습니다.");
            }

            if (requestDto.getMessage() == null || requestDto.getMessage().trim().isEmpty()) {
                throw new IllegalArgumentException("메시지 내용은 필수입니다.");
            }

            return sendMessage(requestDto.getSenderId(), requestDto);
        } catch (IllegalArgumentException | SecurityException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error in sendMessage via WebSocket: {}", e.getMessage(), e);
            throw new RuntimeException("메시지 전송 중 오류가 발생했습니다.", e);
        }
    }

    @Transactional(readOnly = true)
    public List<InquiryChatRoomDto> getUserChatRooms(String userId) {
        try {
            if (userId == null || userId.isEmpty()) {
                throw new IllegalArgumentException("사용자 ID는 필수입니다.");
            }

            // 구매자 또는 판매자로 등록된 모든 채팅방 조회
            List<InquiryChatRoom> buyerRooms = chatRoomRepository.findByBuyerId(userId);
            List<InquiryChatRoom> sellerRooms = chatRoomRepository.findBySellerId(userId);

            // 중복 제거를 위해 Set 사용
            Set<InquiryChatRoom> uniqueRooms = new HashSet<>();
            uniqueRooms.addAll(buyerRooms);
            uniqueRooms.addAll(sellerRooms);

            // 채팅방 ID 목록 추출
            List<Long> chatRoomIds = uniqueRooms.stream()
                    .map(InquiryChatRoom::getId)
                    .collect(Collectors.toList());

            // 모든 채팅방의 마지막 메시지 일괄 조회 (N+1 문제 방지)
            List<InquiryChat> lastMessages = chatRoomIds.isEmpty() 
                    ? List.of() 
                    : chatRepository.findLastMessagesByChatRoomIds(chatRoomIds);

            // 채팅방 ID를 키로 하는 마지막 메시지 맵 생성
            Map<Long, InquiryChat> lastMessageMap = lastMessages.stream()
                    .collect(Collectors.toMap(
                            chat -> chat.getChatRoom().getId(),
                            chat -> chat,
                            (existing, replacement) -> existing.getSentAt().isAfter(replacement.getSentAt()) ? existing : replacement
                    ));

            // DTO로 변환하여 반환
            return uniqueRooms.stream()
                    .map(room -> {
                        // 채팅방의 마지막 메시지 가져오기
                        InquiryChat lastMessage = lastMessageMap.get(room.getId());
                        
                        // 마지막 메시지가 있으면 두 개의 매개변수를 받는 생성자 사용
                        // 없으면 InquiryChatRoom만 받는 생성자 사용
                        return lastMessage != null 
                                ? new InquiryChatRoomDto(room, lastMessage)
                                : new InquiryChatRoomDto(room);
                    })
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw e; // 이미 처리된 예외는 그대로 전달
        } catch (Exception e) {
            log.error("Error in getUserChatRooms: {}", e.getMessage(), e);
            throw new RuntimeException("채팅방 목록 조회 중 오류가 발생했습니다.", e);
        }
    }
}
