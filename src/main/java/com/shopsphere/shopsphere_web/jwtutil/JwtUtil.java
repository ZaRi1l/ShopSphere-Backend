package com.shopsphere.shopsphere_web.jwtutil;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component; // 🌟 @Component 어노테이션 추가 (JwtUtil이 Bean으로 등록되도록)

import javax.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;

@Component // 이 어노테이션이 있어야 Spring이 JwtUtil을 Bean으로 관리하고 주입할 수 있습니다.
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expirationTime; // milliseconds

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    // 🌟🌟🌟 createToken 메서드 수정: String role 파라미터 추가 및 claim("role", role) 추가 🌟🌟🌟
    public String createToken(String userId, String role) { // <-- 여기에 'String role' 추가!
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .setSubject(userId)
                .claim("role", role) // 🌟 'role' 클레임 추가
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            // 토큰 유효성 검사 실패 시 로그 추가 (디버깅 용이)
            System.err.println("JWT Token validation failed: " + e.getMessage());
            return false;
        }
    }

    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    // 🌟🌟🌟 getRoleFromToken 메서드 추가 (옵션, 필요 시 사용) 🌟🌟🌟
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return (String) claims.get("role"); // "role" 클레임에서 역할 정보 추출
    }
}