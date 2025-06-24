package com.shopsphere.shopsphere_web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
public class UserDTO {
    private String id;
    private String email;
    private String password;
    private String name;
    private String phoneNumber;
    private String address;
    private String role;
    private LocalDateTime createdAt;

    @Data
    public static class RegisterRequest {
        private String id;
        private String password;
        private String name;
        private String phoneNumber;
        private String address;
        private String email;
        private String role; // <--- 이 필드를 추가합니다.
    }

    @Data
    public static class LoginRequest {
        private String id; // email 대신 id 사용
        private String password;
    }

    // Response DTOs
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {
        private String id;
        private String email;
        private String name;
        private String phoneNumber;
        private String address;
        private String role;
        // --- 새로 추가된 필드 ---
        private String profileImageUrl; // 클라이언트로 전달될 필드명
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String name;
        private String email;
        private String phoneNumber;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasswordUpdateRequest {
        private String currentPassword;
        private String newPassword;
    }

}