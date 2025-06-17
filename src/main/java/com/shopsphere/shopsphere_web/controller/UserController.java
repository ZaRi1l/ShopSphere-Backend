package com.shopsphere.shopsphere_web.controller;

import com.shopsphere.shopsphere_web.dto.UserDTO;
import com.shopsphere.shopsphere_web.entity.User;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import com.shopsphere.shopsphere_web.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
 
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDTO.RegisterRequest userDTO) {
        try {


            
            UserDTO.Response user = userService.register(userDTO);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
                    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserDTO.LoginRequest loginRequestDTO, HttpSession session) {
        try {
            User authenticatedUser = userService.authenticate(loginRequestDTO.getId(), loginRequestDTO.getPassword());
            if (authenticatedUser != null) {
                // 세션에 사용자 ID 저장
                session.setAttribute("userId", authenticatedUser.getId());
                session.setMaxInactiveInterval(1800); // 30분 세션 유지
                
                UserDTO.Response userResponse = UserDTO.Response.builder()
                        .id(authenticatedUser.getId())
                        .name(authenticatedUser.getName())
                        .email(authenticatedUser.getEmail())
                        .phoneNumber(authenticatedUser.getPhoneNumber())
                        .address(authenticatedUser.getAddress())
                        .role(authenticatedUser.getRole())
                        .build();
                return ResponseEntity.ok(userResponse);
            }
            return ResponseEntity.status(401).body(Map.of("message", "아이디 또는 비밀번호가 일치하지 않습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "로그인 처리 중 오류가 발생했습니다."));
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        try {
            session.invalidate(); // 세션 무효화
            return ResponseEntity.ok(Map.of("message", "로그아웃되었습니다. logout clear"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "로그아웃 처리 중 오류가 발생했습니다."));
        }
    }
    
    @GetMapping("/check")
    public ResponseEntity<?> checkLoginStatus(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId != null) {
            return ResponseEntity.ok(Map.of("isLoggedIn", true, "userId", userId));
        }
        return ResponseEntity.ok(Map.of("isLoggedIn", false));
    }

    // @PatchMapping("/{id}")
    // public ResponseEntity<UserDTO.Response> updateUser(@PathVariable String id,
    //         @RequestBody UserDTO.UpdateRequest request) {
    //     try {
    //         UserDTO.Response updatedUser = userService.updateUser(id, request);
    //         return ResponseEntity.ok(updatedUser);
    //     } catch (RuntimeException e) {
    //         e.printStackTrace();
    //         return ResponseEntity.badRequest().body(new UserDTO.Response());
    //     }
    // }

    @PatchMapping("/update")
    public ResponseEntity<UserDTO.Response> updateUser(@RequestBody UserDTO.UpdateRequest request, HttpSession session) {
        try {
            String userId = (String) session.getAttribute("userId");
            UserDTO.Response updatedUser = userService.updateUser(userId, request);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new UserDTO.Response());
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUser(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        userService.deleteById(userId); // 서비스에서 삭제 처리
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/password")
    public ResponseEntity<?> updatePassword(
            @RequestBody UserDTO.PasswordUpdateRequest request, HttpSession session) {
        try {
            String userId = (String) session.getAttribute("userId");
            userService.updatePassword(userId, request);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            // 👇 반드시 메시지를 포함해서 보내야 함
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(HttpSession session) {
    try {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));
                
        UserDTO.Response userResponse = UserDTO.Response.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .role(user.getRole())
                .build();
                
        return ResponseEntity.ok(userResponse);
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
    }
}


}
