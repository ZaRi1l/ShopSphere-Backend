package com.shopsphere.shopsphere_web.config; // 패키지 경로 확인

import org.springframework.beans.factory.annotation.Value; // @Value 사용을 위해 추가
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File; // File 클래스 임포트 확인
import java.nio.file.Path; // Path 클래스 임포트 추가
import java.nio.file.Paths; // Paths 클래스 임포트 추가

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // application.properties에서 기본 업로드 디렉토리 경로 주입
    @Value("${file.upload-dir}") // 예: uploads
    private String baseUploadDir;

    // FileStorageService에서 정의한 프로필 이미지 하위 디렉토리명과 일치해야 함
    private final String profileImageSubDir = "profile_images";

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*") // 실제 운영 환경에서는 특정 도메인만 허용하는 것이 좋습니다.
                .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // --- 1. 프로필 이미지 전용 리소스 핸들러 ---
        // 웹 접근 경로: /uploads/profile_images/**
        // 실제 파일 시스템 경로: file:/your/project/path/uploads/profile_images/

        // 기본 업로드 디렉토리의 절대 경로
        Path absoluteBaseUploadPath = Paths.get(baseUploadDir).toAbsolutePath().normalize();
        // 프로필 이미지 디렉토리의 절대 경로
        Path absoluteProfileImagePath = absoluteBaseUploadPath.resolve(profileImageSubDir).normalize();

        // 웹에서 접근할 프로필 이미지 경로 패턴
        // 예: baseUploadDir이 "uploads" 이면 "/uploads/profile_images/**"
        String profileImageWebPathPattern = "/" + absoluteBaseUploadPath.getFileName().toString() + "/" + profileImageSubDir + "/**";

        // 실제 디스크 상의 프로필 이미지 위치 (file: 접두사 필수)
        String profileImageDiskLocation = "file:" + absoluteProfileImagePath.toString() + "/";

        System.out.println("프로필 이미지 웹 경로 패턴: " + profileImageWebPathPattern);
        System.out.println("프로필 이미지 디스크 위치: " + profileImageDiskLocation);

        registry.addResourceHandler(profileImageWebPathPattern)
                .addResourceLocations(profileImageDiskLocation)
                .setCachePeriod(3600); // 캐시 기간 설정 (초 단위)

        // --- 2. (선택 사항) 기존 /uploads/** 핸들러 유지 또는 수정 ---
        // 만약 'uploads' 루트 디렉토리 및 다른 하위 디렉토리에도 정적 파일을 저장하고
        // 웹으로 제공해야 한다면, 이 핸들러를 유지하거나 더 구체적으로 만들 수 있습니다.
        // 프로필 이미지 핸들러가 더 구체적이므로 먼저 매칭됩니다.

        String generalUploadsWebPathPattern = "/" + absoluteBaseUploadPath.getFileName().toString() + "/**"; // 예: "/uploads/**"
        String generalUploadsDiskLocation = "file:" + absoluteBaseUploadPath.toString() + "/";

        System.out.println("일반 업로드 웹 경로 패턴: " + generalUploadsWebPathPattern);
        System.out.println("일반 업로드 디스크 위치: " + generalUploadsDiskLocation);

        // 기존 핸들러를 유지하되, 프로필 이미지 핸들러와 겹치지 않도록 주의
        // 또는, 프로필 이미지를 제외한 다른 uploads 하위 경로만 처리하도록 패턴을 수정할 수도 있습니다.
        // 여기서는 기존 핸들러를 유지하는 것으로 가정합니다.
        // (주의: 프로필 이미지 핸들러가 더 구체적이므로 먼저 적용됩니다.
        //  만약 /uploads/profile_images/some_other_file.txt 가 있고,
        //  /uploads/another_subdir/file.txt 가 있다면 각각 올바르게 매핑됩니다.)
        if (!profileImageWebPathPattern.equals(generalUploadsWebPathPattern)) { // 두 패턴이 다를 경우에만 일반 핸들러 추가
             registry.addResourceHandler(generalUploadsWebPathPattern)
                .addResourceLocations(generalUploadsDiskLocation)
                .setCachePeriod(3600);
        }


        // --- 3. (필수) classpath:/static/ 등 기본 정적 리소스 핸들러 ---
        // 이 부분을 추가하지 않으면 src/main/resources/static/ 에 있는 CSS, JS 파일 등에 접근할 수 없습니다.
        registry.addResourceHandler("/static/**") // 또는 /css/**, /js/** 등 개별 설정
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);

        // 디버깅을 위한 로그 (애플리케이션 시작 시 출력됨)
        File profileDirCheck = absoluteProfileImagePath.toFile();
        System.out.println("확인된 프로필 이미지 디렉토리 경로: " + profileDirCheck.getAbsolutePath());
        System.out.println("프로필 이미지 디렉토리 존재 여부: " + profileDirCheck.exists());
        if (profileDirCheck.exists() && profileDirCheck.isDirectory()) {
            System.out.println("프로필 이미지 디렉토리 내용:");
            File[] files = profileDirCheck.listFiles();
            if (files != null) {
                for (File f : files) {
                    System.out.println("  - " + f.getName() + (f.isDirectory() ? " (디렉토리)" : " (파일)"));
                }
            } else {
                System.out.println("  (내용을 읽을 수 없거나 비어 있음)");
            }
        }
    }
}