package com.shopsphere.shopsphere_web.controller;

import com.shopsphere.shopsphere_web.dto.UserDTO;
import com.shopsphere.shopsphere_web.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserDTO.Response> register(@RequestBody UserDTO.RegisterRequest userDTO) {
        UserDTO.Response user = userService.register(userDTO);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/login")
    public ResponseEntity<UserDTO.Response> login(@RequestBody UserDTO.LoginRequest userDTO) {
        UserDTO.Response user = userService.authenticate(userDTO);
        if (user != null) {
            return ResponseEntity.ok(user);
        }
        UserDTO.Response errorResponse = new UserDTO.Response();
        return ResponseEntity.status(401).body(errorResponse);
    }
}
