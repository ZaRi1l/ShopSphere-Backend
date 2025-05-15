package com.shopsphere.shopsphere_web.service;

import com.shopsphere.shopsphere_web.dto.UserDTO;
import com.shopsphere.shopsphere_web.entity.User;
import com.shopsphere.shopsphere_web.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDTO.Response register(UserDTO.RegisterRequest request) {
        User user = User.builder()
                .id(request.getId())
                .email(request.getId())  // id를 email로도 사용
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .role("USER")
                .createdAt(LocalDateTime.now())
                .build();


        user = userRepository.save(user);

        return convertToResponse(user);
    }

    public UserDTO.Response authenticate(UserDTO.LoginRequest request) {
        System.out.println("------ 여기까지");
        return userRepository.findById(request.getId())
                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPassword()))
                .map(this::convertToResponse)
                .orElse(null);
    }

    public UserDTO.Response findById(String id) {
        return userRepository.findById(id)
                .map(this::convertToResponse)
                .orElse(null);
    }

    @Transactional
    public UserDTO.Response update(String id, UserDTO.RegisterRequest request) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setName(request.getName());
                    user.setPhoneNumber(request.getPhoneNumber());
                    user.setAddress(request.getAddress());
                    if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                        user.setPassword(passwordEncoder.encode(request.getPassword()));
                    }
                    return convertToResponse(user);
                })
                .orElse(null);
    }

    private UserDTO.Response convertToResponse(User user) {
        UserDTO.Response response = new UserDTO.Response();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setName(user.getName());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setAddress(user.getAddress());
        response.setRole(user.getRole());
        return response;
    }
}