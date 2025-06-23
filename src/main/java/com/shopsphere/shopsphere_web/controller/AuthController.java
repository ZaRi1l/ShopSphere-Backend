package com.shopsphere.shopsphere_web.controller;

import com.shopsphere.shopsphere_web.dto.UserDTO;
import com.shopsphere.shopsphere_web.entity.User;
import com.shopsphere.shopsphere_web.jwtutil.JwtUtil;
import com.shopsphere.shopsphere_web.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import com.shopsphere.shopsphere_web.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.Map; // Map import 추가

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    // ... (기존 로그인, 회원가입 등 메소드) ...

    // 기존의 중복된 @PostMapping("/api/auth/oauth/kakao/callback")를 제거하거나
    // @RequestMapping에 맞춰 "/oauth/kakao/callback"으로 변경
    // 프론트에서 GET 요청으로 `code`를 보내는 것이 일반적이므로 @GetMapping으로 변경하는 것을 권장합니다.
    // 만약 프론트에서 POST 요청을 보낸다면 @PostMapping 유지
    @PostMapping("/oauth/kakao/callback") // HTTP GET 요청으로 변경 권장
    // @PostMapping("/oauth/kakao/callback") // POST 요청을 유지한다면
    public ResponseEntity<?> kakaoLoginCallback(@RequestParam String code, HttpSession session) {
        try {
            // UserService의 processKakaoLogin 메서드를 호출하여 카카오 사용자 정보를 가져오고 처리
            User kakaoUser = userService.processKakaoLogin(code);

            if (kakaoUser != null) {
                // 2. 일반 로그인과 동일하게 세션에 사용자 ID 저장
                session.setAttribute("userId", kakaoUser.getId());
                session.setMaxInactiveInterval(1800); // 30분 세션 유지

                // 3. 클라이언트로 응답 (세션 ID는 쿠키로 자동 전송되므로 토큰 대신 사용자 정보만 반환)
                UserDTO.Response userResponse = UserDTO.Response.builder()
                        .id(kakaoUser.getId())
                        .name(kakaoUser.getName())
                        .email(kakaoUser.getEmail())
                        .phoneNumber(kakaoUser.getPhoneNumber())
                        .address(kakaoUser.getAddress())
                        .role(kakaoUser.getRole())
                        .profileImageUrl(kakaoUser.getProfileImageUrl()) // 프로필 이미지 URL 추가
                        .build();
                return ResponseEntity.ok(userResponse);
            } else {
                // 카카오 사용자 정보 처리 실패 (예: DB 저장 실패, 유효하지 않은 정보)
                return ResponseEntity.status(500).body(Map.of("message", "카카오 로그인 처리 중 오류가 발생했습니다. (사용자 정보 없음)"));
            }

        } catch (Exception e) {
            System.err.println("카카오 로그인 콜백 처리 중 오류: " + e.getMessage());
            // 디버깅을 위해 스택 트레이스도 출력하는 것이 좋습니다.
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "카카오 로그인 처리 중 오류가 발생했습니다."));
        }
    }
}
