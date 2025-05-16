package com.shopsphere.shopsphere_web.service;

import com.shopsphere.shopsphere_web.dto.UserDTO;
import com.shopsphere.shopsphere_web.entity.User;
import com.shopsphere.shopsphere_web.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.HttpHeaders;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${kakao.restapi.key}")
    private String kakaoRestApiKey;
    @Value("${kakao.redirect-uri}")
    private String kakaoRedirectUri;
    @Value("${kakao.client-secret}")
    private String kakaoClientSecret;

    public UserDTO.Response register(UserDTO.RegisterRequest userDTO) {
    try {
        System.out.println("--------------request complete");
        User user = User.builder()
                .id(userDTO.getId())
                .name(userDTO.getName())
                .email(userDTO.getEmail()) // email 필드 추가
                .password(passwordEncoder.encode(userDTO.getPassword()))
                .phoneNumber(userDTO.getPhoneNumber())
                .address(userDTO.getAddress())
                .role("USER")  // ㅅㅂㅅㅂㅅㅂㅅㅂㅅㅂㅅㅂ
                .build();
        
        User savedUser = userRepository.save(user);
        System.out.println("--------------save complete");

        // User 객체를 UserDTO.Response로 변환
        UserDTO.Response response = new UserDTO.Response();
        response.setId(savedUser.getId());
        response.setName(savedUser.getName());
        response.setEmail(savedUser.getEmail());
        response.setPhoneNumber(savedUser.getPhoneNumber());
        response.setAddress(savedUser.getAddress());
        response.setRole(savedUser.getRole());

        System.out.println("--------------response complete");

        return response;
    } catch (DataIntegrityViolationException e) {
        // 예외 처리 로직 (예: 로그 기록, 사용자에게 적절한 메시지 반환 등)
        throw new RuntimeException("사용자 등록 중 오류가 발생했습니다: " + e.getMessage());
    }
}

    public User authenticate(String id, String password) {
        return userRepository.findById(id)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .orElse(null);
    }

    public String getKakaoAccessToken(String authorizationCode) {
        String tokenUrl = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoRestApiKey);
        params.add("redirect_uri", kakaoRedirectUri);
        params.add("code", authorizationCode);
        if (kakaoClientSecret != null && !kakaoClientSecret.isEmpty()) {
            params.add("client_secret", kakaoClientSecret);
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<Map> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, request, Map.class);
        return (String) response.getBody().get("access_token");
    }

    // public User processKakaoLogin(String accessToken) {
    //     Map<String, Object> userInfo = getKakaoUserInfo(accessToken);
    //     if (userInfo != null) {
    //         String kakaoId = String.valueOf(userInfo.get("id"));
    //         Map<String, Object> kakaoAccount = (Map<String, Object>) userInfo.get("kakao_account");
    //         String email = kakaoAccount.get("email") != null ? (String) kakaoAccount.get("email") : null;
    //         Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
    //         String nickname = profile.get("nickname") != null ? (String) profile.get("nickname") : null;

    //         Optional<User> existingUser = userRepository.findById(kakaoId);

    //         return existingUser.orElseGet(() -> {
    //             // 카카오 계정으로 처음 로그인하는 경우, 새로운 사용자 생성 및 암호화된 비밀번호 저장
    //             User newUser = User.builder()
    //                     .id(kakaoId) // 카카오 ID를 사용자 ID로 사용
    //                     .password(passwordEncoder.encode("kakao_" + kakaoId)) // 임시 비밀번호 암호화하여 저장
    //                     .name(nickname)
    //                     .phoneNumber(null)
    //                     .address(null)
    //                     .build();
    //             return userRepository.save(newUser);
    //         });
    //     }
    //     return null;
    // }

    private Map<String, Object> getKakaoUserInfo(String accessToken) {
        String apiUrl = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<HttpHeaders> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.GET, request, Map.class);
            return response.getBody();
        } catch (Exception e) {
            // 에러 처리
            return null;
        }
    }

    // JWT 토큰 생성 메서드 (별도 구현 필요)
    // public String createJwtToken(String userId) { ... }

    public User updateUserInfo(String id, UserDTO.UpdateRequest dto) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("사용자 없음"));
    
        if (dto.getName() != null) user.setName(dto.getName());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getPhoneNumber() != null) user.setPhoneNumber(dto.getPhoneNumber());
    
        return userRepository.save(user);
    }
    
}