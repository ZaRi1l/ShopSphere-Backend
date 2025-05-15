package com.shopsphere.shopsphere_web.controller;

import com.shopsphere.shopsphere_web.dto.UserDTO;
import com.shopsphere.shopsphere_web.entity.User;
import com.shopsphere.shopsphere_web.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth") // 기존 로그인 API 경로 (필요에 따라 수정)
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserDTO userDTO) {
        User user = userService.authenticate(userDTO.getId(), userDTO.getPassword());
        if (user != null) {
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.status(401).body("{\"message\": \"Invalid ID or password\"}");
    }

    @GetMapping("/oauth/kakao/callback")
    public ResponseEntity<?> kakaoLoginCallback(@RequestParam("code") String code) {
        // 1. 인가 코드로 카카오 Access Token 요청
        String accessToken = userService.getKakaoAccessToken(code);

        // 2. Access Token으로 카카오 사용자 정보 요청 및 처리
        User user = userService.processKakaoLogin(accessToken);

        if (user != null) {
            // 3. 로그인 성공 처리 (JWT 토큰 생성 및 반환 등)
            return ResponseEntity.ok(user); // 예시: User 객체 반환
        } else {
            return ResponseEntity.status(401).body("{\"message\": \"Kakao login failed\"}");
        }
    }
}
