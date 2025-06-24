package com.shopsphere.shopsphere_web.service;

import com.shopsphere.shopsphere_web.config.KakaoProperties;
import com.shopsphere.shopsphere_web.dto.UserDTO;
import com.shopsphere.shopsphere_web.entity.User;
import com.shopsphere.shopsphere_web.repository.UserRepository;
import com.shopsphere.shopsphere_web.jwtutil.JwtUtil; // 🌟 JwtUtil import 추가
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Setter;
import java.util.Optional;
import org.springframework.web.client.HttpClientErrorException;
import java.util.HashMap; // 🌟 HashMap import 추가
import org.springframework.http.HttpStatus; // 🌟 HttpStatus import 추가

@Service
@RequiredArgsConstructor
public class UserService {
    private final KakaoProperties kakaoProperties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final RestTemplate restTemplate;

    public UserDTO.Response register(UserDTO.RegisterRequest userDTO) {
        try {
            Optional<User> existingUser = userRepository.findById(userDTO.getId());
            if (existingUser.isPresent()) {
                throw new RuntimeException("이미 아이디가 존재합니다.");
            }

            User.UserBuilder userBuilder = User.builder()
                    .id(userDTO.getId())
                    .name(userDTO.getName())
                    .email(userDTO.getEmail())
                    .password(passwordEncoder.encode(userDTO.getPassword()))
                    .phoneNumber(userDTO.getPhoneNumber())
                    .address(userDTO.getAddress());

            // role 값 설정 로직 변경
            if (userDTO.getRole() != null && !userDTO.getRole().isEmpty()) {
                // 유효한 role 값인지 검증하는 로직을 추가할 수 있습니다.
                // 예를 들어 "USER", "SELLER"만 허용 등
                if ("SELLER".equalsIgnoreCase(userDTO.getRole()) || "USER".equalsIgnoreCase(userDTO.getRole())) {
                    userBuilder.role(userDTO.getRole().toUpperCase());
                } else {
                    // 허용되지 않는 role 값일 경우 처리 (예: 예외 발생 또는 기본값 USER)
                    throw new RuntimeException("유효하지 않은 역할(role) 값입니다.");
                    // 또는 userBuilder.role("USER");
                }
            } else {
                userBuilder.role("USER"); // role이 전달되지 않으면 기본값 'USER'
            }

            User user = userBuilder.build();
            User savedUser = userRepository.save(user);

            // ... (이하 응답 생성 로직은 동일)
            UserDTO.Response response = new UserDTO.Response();
            response.setId(savedUser.getId());
            response.setName(savedUser.getName());
            response.setEmail(savedUser.getEmail());
            response.setPhoneNumber(savedUser.getPhoneNumber());
            response.setAddress(savedUser.getAddress());
            response.setRole(savedUser.getRole()); // 응답에도 role 포함

            return response;
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("사용자 등록 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    public User authenticate(String id, String password) {
        return userRepository.findById(id)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .orElse(null);
    }

    public String getKakaoAccessToken(String authorizationCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoProperties.getRestapiKey());
        params.add("redirect_uri", kakaoProperties.getRedirectUri());
        params.add("code", authorizationCode);

        if (kakaoProperties.getClientSecret() != null && !kakaoProperties.getClientSecret().isEmpty()) {
            params.add("client_secret", kakaoProperties.getClientSecret());
        }

        System.out.println("--- KAKAO PARAM CHECK ---");
        System.out.println("client_id: " + kakaoProperties.getRestapiKey());
        System.out.println("redirect_uri: " + kakaoProperties.getRedirectUri());
        System.out.println("--------------------------");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    kakaoProperties.getTokenUri(),
                    HttpMethod.POST,
                    request,
                    Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            } else {
                throw new RuntimeException("카카오 토큰 요청 실패: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException e) {
            System.err.println("--- KAKAO API ERROR RESPONSE ---");
            System.err.println("HTTP Status Code: " + e.getStatusCode()); // 이 부분이 401일 것입니다.
            System.err.println("Response Body: " + e.getResponseBodyAsString()); // <<< 이 부분이 가장 중요합니다!
            System.err.println("---------------------------------");
            throw new RuntimeException("카카오 요청 실패 (401 등): " + e.getStatusCode() + " - " + e.getResponseBodyAsString(),
                    e);
        }
    }

    @Transactional
    public User processKakaoLogin(String code) {
        String accessToken = getKakaoAccessToken(code);
        Map<String, Object> userInfo = getKakaoUserInfo(accessToken);

        // --- 1. 카카오 사용자 ID 파싱 (Long -> String 변환) ---
        Long kakaoIdLong = (Long) userInfo.get("id");
        if (kakaoIdLong == null) {
            throw new RuntimeException("카카오 사용자 ID를 가져올 수 없습니다.");
        }
        final String kakaoId = String.valueOf(kakaoIdLong); // <-- final 추가

        // --- 2. 닉네임 파싱 (안전한 null 체크) ---
        String tempNickname = null; // 임시 변수 사용
        if (userInfo.get("properties") instanceof Map) {
            Map<String, Object> properties = (Map<String, Object>) userInfo.get("properties");
            tempNickname = (String) properties.get("nickname");
        }
        if (tempNickname == null || tempNickname.isEmpty()) {
            tempNickname = "카카오유저_" + kakaoId;
            System.out.println("카카오 닉네임이 없어 기본값으로 설정: " + tempNickname);
        }
        final String nickname = tempNickname; // <-- final 추가

        // --- 3. 이메일 파싱 (안전한 null 체크 및 계층 접근) ---
        String tempEmail = null; // 임시 변수 사용
        if (userInfo.get("kakao_account") instanceof Map) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) userInfo.get("kakao_account");

            Boolean hasEmail = (Boolean) kakaoAccount.get("has_email");
            if (hasEmail != null && hasEmail) {
                Boolean emailNeedsAgreement = (Boolean) kakaoAccount.get("email_needs_agreement");
                Boolean isEmailValid = (Boolean) kakaoAccount.get("is_email_valid");
                Boolean isEmailVerified = (Boolean) kakaoAccount.get("is_email_verified");

                if ((emailNeedsAgreement == null || !emailNeedsAgreement) &&
                        (isEmailValid == null || isEmailValid) &&
                        (isEmailVerified == null || isEmailVerified)) {
                    tempEmail = (String) kakaoAccount.get("email");
                } else {
                    System.out.println("카카오 이메일은 있지만, 사용 조건 불충족 (동의 필요/유효X/인증X).");
                }
            } else {
                System.out.println("카카오 이메일 정보에 동의하지 않았습니다.");
            }
        }
        final String email = tempEmail; // <-- final 추가

        // --- 디버깅을 위한 로그 추가 ---
        System.out.println("--- KAKAO USER INFO PARSED ---");
        System.out.println("Parsed Kakao ID: " + kakaoId);
        System.out.println("Parsed Nickname: " + nickname);
        System.out.println("Parsed Email: " + email);
        System.out.println("---------------------------------");

        // 4. DB에서 사용자 찾기 또는 새로 생성
        User user = userRepository.findByKakaoId(kakaoId) // kakaoId는 이미 final 또는 effectively final
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setId("kakao_" + kakaoId);
                    newUser.setKakaoId(kakaoId);
                    newUser.setName(nickname); // nickname은 이미 final 또는 effectively final
                    newUser.setEmail(email); // email은 이미 final 또는 effectively final
                    newUser.setRole("USER");
                    newUser.setCreatedAt(LocalDateTime.now());
                    newUser.setPassword(null);

                    System.out.println(
                            "새로운 카카오 사용자 생성 중: User ID=" + newUser.getId() + ", Kakao ID=" + newUser.getKakaoId());
                    return userRepository.save(newUser);
                });

        // 5. 기존 사용자 정보 업데이트 (닉네임, 이메일 변경 등)
        // 이 부분에서는 람다 외부이므로 final 키워드가 필요 없습니다.
        boolean updated = false;
        if (!user.getName().equals(nickname)) { // nickname은 final 변수
            user.setName(nickname);
            updated = true;
        }
        if (email != null && !email.equals(user.getEmail())) { // email은 final 변수
            user.setEmail(email);
            updated = true;
        }

        if (updated) {
            userRepository.save(user);
            System.out.println("기존 카카오 사용자 정보 업데이트 완료: User ID=" + user.getId());
        } else {
            System.out.println("기존 카카오 사용자 정보 변경 없음: User ID=" + user.getId());
        }

        return user;
    }

    // updateUser, deleteById, updatePassword, findById, updateUserProfileImage,
    // getFileNameFromUrl
    // 나머지 메소드들은 그대로 유지합니다.
    public UserDTO.Response updateUser(String id, UserDTO.UpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());

        userRepository.save(user);

        return UserDTO.Response.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    public void deleteById(String id) {
        userRepository.deleteById(id);
    }

    public void updatePassword(String id, UserDTO.PasswordUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 🔐 현재 비밀번호가 일치하지 않으면 예외
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다.");
        }

        // ✅ 비밀번호 일치 → 새 비밀번호로 변경
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    @Transactional
    public String updateUserProfileImage(String userId, String newImageUrl, String oldFileNameToDelete) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. ID: " + userId));

        // 이전 파일 삭제 (FileStorageService의 프로필 이미지 전용 삭제 메소드 사용)
        if (oldFileNameToDelete != null && !oldFileNameToDelete.isEmpty()) {
            fileStorageService.deleteProfileImage(oldFileNameToDelete);
        }

        user.setProfileImageUrl(newImageUrl);
        userRepository.save(user);
        return newImageUrl;
    }

    public String getFileNameFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }
        try {
            return imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
        } catch (Exception e) {
            System.err.println("URL에서 파일명 추출 실패: " + imageUrl);
            return null;
        }
    }

    private Map<String, Object> getKakaoUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON); // JSON 타입으로 변경 (카카오 API 문서 확인)

        HttpEntity<HttpHeaders> request = new HttpEntity<>(headers);

        try {
            // user_info_uri는 application.properties (또는 yml)에서 주입받은 필드입니다.
            // 이 필드가 UserService 클래스 내에 선언되어 있고, 값이 제대로 주입되는지 확인해야 합니다.
            ResponseEntity<Map> response = restTemplate.exchange(kakaoProperties.getUserInfoUri(), HttpMethod.GET,
                    request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                System.err.println("카카오 사용자 정보 응답 실패: " + response.getStatusCode() + ", Body: " + response.getBody());
                throw new RuntimeException("카카오 사용자 정보 응답 실패");
            }
        } catch (HttpClientErrorException e) {
            System.err.println("카카오 사용자 정보 요청 클라이언트 오류 (HTTP " + e.getStatusCode() + "): " + e.getResponseBodyAsString());
            throw new RuntimeException("카카오 사용자 정보 요청 실패: 유효하지 않은 토큰입니다. (" + e.getStatusCode() + ")", e);
        } catch (Exception e) {
            System.err.println("카카오 사용자 정보 요청 중 예상치 못한 오류 발생: " + e.getMessage());
            throw new RuntimeException("카카오 사용자 정보 요청 실패: " + e.getMessage(), e);
        }
    }
}