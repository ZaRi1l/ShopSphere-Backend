package com.shopsphere.shopsphere_web.controller;

import com.shopsphere.shopsphere_web.dto.UserDTO;
import com.shopsphere.shopsphere_web.entity.User;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import com.shopsphere.shopsphere_web.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserDTO.Response> register(@RequestBody UserDTO.RegisterRequest userDTO) {
        try {
            UserDTO.Response user = userService.register(userDTO);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            e.printStackTrace(); // ✅ 예외 로그를 출력해서 원인 파악 가능하게
            return ResponseEntity.badRequest().body(new UserDTO.Response());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<UserDTO.Response> login(@RequestBody UserDTO.LoginRequest loginRequestDTO) {
        User authenticatedUser = userService.authenticate(loginRequestDTO.getId(), loginRequestDTO.getPassword());
        if (authenticatedUser != null) {
            UserDTO.Response userResponse = UserDTO.Response.builder()
                    .id(authenticatedUser.getId())
                    .name(authenticatedUser.getName())
                    .email(authenticatedUser.getEmail()) // User 엔티티에 해당 필드가 있다면
                    .phoneNumber(authenticatedUser.getPhoneNumber()) // User 엔티티에 해당 필드가 있다면
                    .address(authenticatedUser.getAddress()) // User 엔티티에 해당 필드가 있다면
                    .role(authenticatedUser.getRole()) // User 엔티티에 해당 필드가 있다면
                    .build();
            return ResponseEntity.ok(userResponse);
        }
        UserDTO.Response errorResponse = UserDTO.Response.builder().build();
        return ResponseEntity.status(401).body(errorResponse);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserDTO.Response> updateUser(@PathVariable String id,
            @RequestBody UserDTO.UpdateRequest request) {
        try {
            UserDTO.Response updatedUser = userService.updateUser(id, request);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new UserDTO.Response());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        userService.deleteById(id); // 서비스에서 삭제 처리
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<?> updatePassword(
            @PathVariable String id,
            @RequestBody UserDTO.PasswordUpdateRequest request) {
        try {
            userService.updatePassword(id, request);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            // 👇 반드시 메시지를 포함해서 보내야 함
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

}
