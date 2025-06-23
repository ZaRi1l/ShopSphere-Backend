package com.shopsphere.shopsphere_web.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user") // 테이블명 확인 (소문자 'user'로 되어 있음)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(name = "user_id", nullable = false, unique = true) // DB 컬럼명 user_id
    private String id; // 필드명은 id

    @Column(nullable = true, columnDefinition = "VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String email;

    @Column(nullable = true, columnDefinition = "VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String password;

    @Column(nullable = true, columnDefinition = "VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String name;

    @Column(name = "phone_number", nullable = true, columnDefinition = "VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String phoneNumber;

    @Column(nullable = true, columnDefinition = "TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String address;

    @Column(nullable = true, columnDefinition = "VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String role;

    @Column(name = "created_at", nullable = true)
    private LocalDateTime createdAt;

    // --- 새로 추가된 필드 ---
    @Column(name = "profile_image_url", nullable = true, length = 512) // DB 컬럼명 profile_image_url
    private String profileImageUrl; // 필드명은 profileImageUrl (카멜케이스)

    @Column(name = "kakao_id", unique = true) // 카카오 로그인 연동을 위한 필드 추가
    private String kakaoId;
}