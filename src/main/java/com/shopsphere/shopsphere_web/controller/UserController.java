package com.shopsphere.shopsphere_web.controller;

import com.shopsphere.shopsphere_web.dto.UserDTO;
import com.shopsphere.shopsphere_web.entity.User;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import com.shopsphere.shopsphere_web.service.UserService;
import com.shopsphere.shopsphere_web.service.FileStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.core.Authentication; // Authentication 심볼 해결
import org.springframework.security.core.context.SecurityContextHolder; // SecurityContextHolder 심볼 해결
import java.util.Enumeration; // Enumeration 심볼 해결

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final FileStorageService fileStorageService;

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


                // 🌟🌟🌟 일반 로그인 세션 저장 로그 추가 🌟🌟🌟
                System.out.println("[Login] 일반 로그인 성공! Session ID: " + session.getId() + ", Stored userId: " + session.getAttribute("userId"));
                
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
        // 🌟🌟🌟 1. checkLoginStatus 호출 시점 및 현재 세션 ID, userId 확인 🌟�🌟
        String userId = (String) session.getAttribute("userId");
        System.out.println("[Check] checkLoginStatus 호출됨. Current Session ID: " + session.getId());
        System.out.println("[Check] Session userId (raw): " + userId); // 세션에서 직접 가져온 userId 값

        // 🌟🌟🌟 2. Spring Security Authentication 객체 확인 (추가 디버깅 용) 🌟🌟🌟
        // Spring Security가 인증을 처리했다면 여기에 정보가 있을 수 있습니다.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("[Check] SecurityContext Authentication: " + (authentication != null ? authentication.getName() + " (Authenticated: " + authentication.isAuthenticated() + ")" : "null"));

        // 🌟🌟🌟 3. 세션에 저장된 모든 속성 확인 (가장 중요!) 🌟🌟🌟
        System.out.println("[Check] All Session Attributes:");
        Enumeration<String> attributeNames = session.getAttributeNames();
        boolean hasAttributes = false;
        while (attributeNames.hasMoreElements()) {
            hasAttributes = true;
            String name = attributeNames.nextElement();
            Object value = session.getAttribute(name);
            System.out.println("  - " + name + ": " + value + " (Type: " + (value != null ? value.getClass().getName() : "null") + ")");
        }
        if (!hasAttributes) {
            System.out.println("  (No attributes found in this session)");
        }
        System.out.println("----------------------------------------");


        if (userId != null) {
            System.out.println("[Check] 사용자 로그인 상태: true, userId: " + userId);
            return ResponseEntity.ok(Map.of("isLoggedIn", true, "userId", userId));
        }
        System.out.println("[Check] 사용자 로그인 상태: false.");
        return ResponseEntity.ok(Map.of("isLoggedIn", false));
    }

    // @PatchMapping("/{id}")
    // public ResponseEntity<UserDTO.Response> updateUser(@PathVariable String id,
    // @RequestBody UserDTO.UpdateRequest request) {
    // try {
    // UserDTO.Response updatedUser = userService.updateUser(id, request);
    // return ResponseEntity.ok(updatedUser);
    // } catch (RuntimeException e) {
    // e.printStackTrace();
    // return ResponseEntity.badRequest().body(new UserDTO.Response());
    // }
    // }

    @PatchMapping("/update")
    public ResponseEntity<UserDTO.Response> updateUser(@RequestBody UserDTO.UpdateRequest request,
            HttpSession session) {
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
                    .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다. ID: " + userId)); // 예외 메시지에 ID 추가

            // User 엔티티에서 UserDTO.Response로 변환
            UserDTO.Response userResponse = UserDTO.Response.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .phoneNumber(user.getPhoneNumber())
                    .address(user.getAddress())
                    .role(user.getRole())
                    .profileImageUrl(user.getProfileImageUrl()) // --- profileImageUrl 매핑 추가 ---
                    .build();

            return ResponseEntity.ok(userResponse);
        } catch (RuntimeException e) { // 구체적인 예외 처리 또는 로깅 추가 가능
            // 예를 들어, 사용자를 찾지 못한 경우 404 반환
            if (e.getMessage().startsWith("사용자 정보를 찾을 수 없습니다.")) {
                return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
            }
            // 기타 런타임 예외는 500으로 처리
            return ResponseEntity.status(500).body(Map.of("message", "서버 내부 오류: " + e.getMessage()));
        } catch (Exception e) { // 그 외 모든 예외
            return ResponseEntity.status(500).body(Map.of("message", "알 수 없는 오류 발생: " + e.getMessage()));
        }
    }

    // --- 프로필 이미지 업로드/수정 API ---
    @PatchMapping("/profile-image") // 또는 @PostMapping
    public ResponseEntity<?> uploadProfileImage(@RequestParam("profileImageFile") MultipartFile file,
            HttpSession session, HttpServletRequest request) {
        // 1. 사용자 인증 (세션에서 userId 가져오기)
        // 2. 파일 유효성 검사 (비어 있는지, 크기, 타입 등 - Multer 설정 또는 서비스 계층에서 처리 가능)
        // 3. (선택) 이전 이미지 파일명 가져오기 (삭제 목적)
        // 4. FileStorageService를 사용하여 파일 저장
        // 5. 저장된 파일의 접근 URL 생성
        // 6. UserService를 사용하여 데이터베이스에 사용자의 profileImageUrl 업데이트 (이전 파일명 전달)
        // 7. 성공 응답 (새로운 profileImageUrl 포함) 또는 실패 응답 반환
        try {
            String userId = (String) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
            }

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "업로드할 파일을 선택해주세요."));
            }

            User currentUser = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다. ID: " + userId));
            String oldFileName = userService.getFileNameFromUrl(currentUser.getProfileImageUrl());

            String storedFileName = fileStorageService.storeProfileImage(file, userId); // FileStorageService에 프로필 전용
                                                                                        // 메소드 사용

            String webAccessiblePath = "/uploads/profile_images/" + storedFileName; // WebConfig의 resource handler 경로와
                                                                                    // 일치
            // String fileDownloadUri = request.getScheme() + "://" + request.getServerName() + ":"
            //         + request.getServerPort() +
            //         (request.getContextPath() != null ? request.getContextPath() : "") + webAccessiblePath;

            userService.updateUserProfileImage(userId, webAccessiblePath, oldFileName);

            return ResponseEntity.ok(Map.of(
                    "message", "프로필 이미지가 성공적으로 업데이트되었습니다.",
                    "profileImageUrl", webAccessiblePath));

        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of("message", "이미지 처리 중 오류 발생: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "알 수 없는 오류 발생: " + e.getMessage()));
        }
    }

}
