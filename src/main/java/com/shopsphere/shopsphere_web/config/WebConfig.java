// src/main/java/com/shopsphere/shopsphere_web/config/WebConfig.java
package com.shopsphere.shopsphere_web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.base-upload-dir}")
    private String baseUploadDirProperty; // 원본 프로퍼티 값

    @Value("${file.profile-image-subdir}")
    private String profileImageSubDir;

    @Value("${file.review-image-subdir}")
    private String reviewImageSubDir;

    @Value("${file.product-image-subdir}") // 이 값을 application.properties에 추가
    private String productImageSubDir; // 예: "images/products"

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 주입받은 프로퍼티 값에서 앞뒤 공백 제거
        String trimmedBaseUploadDir = baseUploadDirProperty.trim();
        String trimmedProfileSubDir = profileImageSubDir.trim();
        String trimmedReviewSubDir = reviewImageSubDir.trim();

        Path absoluteBaseUploadPath = Paths.get(trimmedBaseUploadDir).toAbsolutePath().normalize();
        String baseUploadUrlSegment = extractBaseUrlSegment(trimmedBaseUploadDir);

        // 1. 프로필 이미지 리소스 핸들러
        Path absoluteProfileImagePath = absoluteBaseUploadPath.resolve(trimmedProfileSubDir).normalize();
        String profileImageWebPathPattern = "/" + baseUploadUrlSegment + "/" + trimmedProfileSubDir + "/**";
        String profileImageDiskLocation = "file:" + absoluteProfileImagePath.toString() + File.separator;

        System.out.println("--- 프로필 이미지 핸들러 설정 ---");
        System.out.println("웹 경로 패턴: " + profileImageWebPathPattern);
        System.out.println("디스크 위치: " + profileImageDiskLocation);
        registry.addResourceHandler(profileImageWebPathPattern)
                .addResourceLocations(profileImageDiskLocation)
                .setCachePeriod(3600);

        // 2. 리뷰 이미지 리소스 핸들러
        Path absoluteReviewImagePath = absoluteBaseUploadPath.resolve(trimmedReviewSubDir).normalize();
        String reviewImageWebPathPattern = "/" + baseUploadUrlSegment + "/" + trimmedReviewSubDir + "/**";
        String reviewImageDiskLocation = "file:" + absoluteReviewImagePath.toString() + File.separator;

        System.out.println("--- 리뷰 이미지 핸들러 설정 ---");
        System.out.println("웹 경로 패턴: " + reviewImageWebPathPattern);
        System.out.println("디스크 위치: " + reviewImageDiskLocation);
        registry.addResourceHandler(reviewImageWebPathPattern)
                .addResourceLocations(reviewImageDiskLocation)
                .setCachePeriod(3600);
        
        System.out.println("--- 디렉토리 검사 ---");
        printDirectoryStatus("기본 업로드", absoluteBaseUploadPath);
        printDirectoryStatus("프로필 이미지", absoluteProfileImagePath);
        printDirectoryStatus("리뷰 이미지", absoluteReviewImagePath);

        String trimmedProductImageSubDir = "images" + File.separator + "products"; // 임시 하드코딩, 실제로는 프로퍼티 사용
    
        Path absoluteProductImagePath = absoluteBaseUploadPath.resolve(trimmedProductImageSubDir).normalize();
        String productImageWebPathPattern = "/" + baseUploadUrlSegment + "/" + trimmedProductImageSubDir.replace(File.separator, "/") + "/**";
        // 예: /uploads/images/products/**
        String productImageDiskLocation = "file:" + absoluteProductImagePath.toString() + File.separator;
    
        System.out.println("--- 상품 이미지 핸들러 설정 ---");
        System.out.println("웹 경로 패턴: " + productImageWebPathPattern);
        System.out.println("디스크 위치: " + productImageDiskLocation);
        registry.addResourceHandler(productImageWebPathPattern)
                .addResourceLocations(productImageDiskLocation)
                .setCachePeriod(3600);
    
    }

    // URL 세그먼트 추출 헬퍼 메소드
    private String extractBaseUrlSegment(String pathProperty) {
        String tempPath = pathProperty;
        if (tempPath.startsWith("./")) {
            tempPath = tempPath.substring(2);
        } else if (tempPath.startsWith("/")) {
            tempPath = tempPath.substring(1);
        }
        // 첫 번째 디렉토리 이름만 사용하거나, 전체 상대 경로를 사용하도록 조정 가능
        // 현재는 첫 번째 디렉토리 이름만 사용 (예: "uploads")
        if (tempPath.contains(File.separator)) { // OS 독립적인 구분자 사용
             return tempPath.substring(0, tempPath.indexOf(File.separator));
        }
        return tempPath;
    }

    private void printDirectoryStatus(String type, Path path) {
        // ... (이전과 동일)
        File dirCheck = path.toFile();
        System.out.println("확인된 " + type + " 디렉토리 경로: " + dirCheck.getAbsolutePath());
        System.out.println(type + " 디렉토리 존재 여부: " + dirCheck.exists());
        if (dirCheck.exists() && !dirCheck.isDirectory()) {
            System.out.println("경고: " + type + " 경로가 디렉토리가 아닙니다!");
        } else if (dirCheck.exists() && dirCheck.isDirectory()) {
             System.out.println(type + " 디렉토리는 정상적으로 존재합니다.");
        } else {
             System.out.println(type + " 디렉토리가 존재하지 않습니다. 애플리케이션이 생성할 예정입니다 (FileStorageService에서).");
        }
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/{spring:[\\w\\-]+}")
                .setViewName("forward:/index.html");
        registry.addViewController("/**/{spring:[\\w\\-]+}")
                .setViewName("forward:/index.html");
        registry.addViewController("/{spring:[\\w\\-]+}/**{spring:?!(\\.js|\\.css|\\.png|\\.jpg|\\.jpeg|\\.svg|\\.gif)$}")
                .setViewName("forward:/index.html");
    }

}   