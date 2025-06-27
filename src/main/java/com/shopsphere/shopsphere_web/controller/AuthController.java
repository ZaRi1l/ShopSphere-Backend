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
    @PostMapping("/oauth/kakao/callback") // HTTP GET 요청으로 변경 권장
    // @PostMapping("/oauth/kakao/callback") // POST 요청을 유지한다면
    public ResponseEntity<?> kakaoLoginCallback(@RequestParam String code, HttpSession session) {
        System.out.println("--- AuthController kakaoLoginCallback 호출 시작 ---");
        System.out.println("Received Kakao Code: " + code);
        try {
            User kakaoUser = userService.processKakaoLogin(code);

            if (kakaoUser != null) {
                System.out.println("[AuthKakao] userService.processKakaoLogin 결과: User 객체 반환됨.");
                String kakaoUserId = kakaoUser.getId(); // String으로 명시적으로 변수 선언
                System.out.println("  User ID from Kakao (String): " + kakaoUserId);
                System.out.println("  Session ID BEFORE setAttribute: " + session.getId());
                System.out.println("  Is new session BEFORE setAttribute: " + session.isNew());

                session.setAttribute("userId", kakaoUserId); // String 변수 사용

                // 🌟🌟🌟 세션 속성 저장 후, 변경 사항을 즉시 DB에 반영하도록 시도 🌟🌟🌟
                // Spring Session 3.x 버전에서는 session.save() 메소드가 존재하지 않을 수 있습니다.
                // 대신 요청 완료 시 자동으로 저장되지만, 강제 저장을 시도하는 방법이 필요할 수 있습니다.
                // HttpServletRequestWrapper를 통해 Session을 얻어오는 경우에만 가능할 수 있습니다.
                // 이 시도는 주로 RedisSessionRepository 등에서 직접 save()를 제공할 때 유용합니다.
                // JDBC 세션에서는 요청 스코프가 끝날 때 자동으로 저장됩니다.
                // 하지만 강제 변경을 통해 Flush를 유도할 수는 있습니다.

                // 세션 ID를 변경하여 강제 저장을 유도 (디버깅 목적)
                // if (!session.isNew()) { // 새로운 세션일 때는 ID 변경이 불가능하거나 의미 없음
                //     request.changeSessionId();
                //     System.out.println("[AuthKakao] Session ID changed to: " + session.getId());
                // }


                session.setMaxInactiveInterval(1800); // 30분 세션 유지

                System.out.println("[AuthKakao] 세션에 userId 저장 완료. Session ID: " + session.getId() + ", Stored userId: " + session.getAttribute("userId"));
                System.out.println("  Is new session AFTER setAttribute: " + session.isNew());

                UserDTO.Response userResponse = UserDTO.Response.builder()
                        .id(kakaoUser.getId())
                        .name(kakaoUser.getName())
                        .email(kakaoUser.getEmail())
                        .phoneNumber(kakaoUser.getPhoneNumber())
                        .address(kakaoUser.getAddress())
                        .role(kakaoUser.getRole())
                        .profileImageUrl(kakaoUser.getProfileImageUrl())
                        .build();
                System.out.println("[AuthKakao] 응답 데이터 준비 완료. User ID: " + userResponse.getId());
                return ResponseEntity.ok(userResponse);
            } else {
                System.err.println("[AuthKakao] ERROR: userService.processKakaoLogin이 null을 반환했습니다.");
                return ResponseEntity.status(500).body(Map.of("message", "카카오 로그인 처리 중 오류가 발생했습니다. (사용자 정보 없음)"));
            }

        } catch (Exception e) {
            System.err.println("[AuthKakao] Exception in kakaoLoginCallback: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "카카오 로그인 처리 중 오류가 발생했습니다."));
        } finally {
            System.out.println("--- AuthController kakaoLoginCallback 호출 종료 ---");
        }
    }

}