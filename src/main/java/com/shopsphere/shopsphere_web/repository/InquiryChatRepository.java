package com.shopsphere.shopsphere_web.repository;

import com.shopsphere.shopsphere_web.entity.InquiryChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InquiryChatRepository extends JpaRepository<InquiryChat, Long> {
    // 채팅방 ID로 모든 채팅 메시지 조회 (최신 메시지 순으로 정렬)
    @Query("SELECT c FROM InquiryChat c WHERE c.chatRoom.id = :chatRoomId ORDER BY c.sentAt ASC")
    List<InquiryChat> findByChatRoomIdOrderBySentAtAsc(@Param("chatRoomId") Long chatRoomId);
    
    // 채팅방 ID와 메시지 ID로 특정 메시지 이후의 메시지들 조회 (최신 메시지 순으로 정렬)
    @Query("SELECT c FROM InquiryChat c WHERE c.chatRoom.id = :chatRoomId AND c.id > :lastMessageId ORDER BY c.sentAt ASC")
    List<InquiryChat> findByChatRoomIdAndIdGreaterThanOrderBySentAtAsc(
        @Param("chatRoomId") Long chatRoomId, 
        @Param("lastMessageId") Long lastMessageId
    );
}
