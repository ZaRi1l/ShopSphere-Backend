package com.shopsphere.shopsphere_web.config;

import java.nio.file.Paths;
import java.io.File;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 경로에 대해
                .allowedOriginPatterns("*") // 프론트 도메인
                .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true); // 인증 쿠키 허용 시 필요
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadsDir = Paths.get("uploads").toAbsolutePath().toString();
        System.out.println("Uploads 디렉토리 절대 경로: " + uploadsDir);
        
        // 디렉토리 존재 여부 확인
        File uploadDir = new File(uploadsDir);
        System.out.println("Uploads 디렉토리 존재 여부: " + uploadDir.exists());
        System.out.println("Uploads 디렉토리 경로: " + uploadDir.getAbsolutePath());
        
        // 디렉토리 내 파일 목록 출력
        if (uploadDir.exists() && uploadDir.isDirectory()) {
            System.out.println("Uploads 디렉토리 내용:");
            for (File f : uploadDir.listFiles()) {
                System.out.println("  - " + f.getName() + (f.isDirectory() ? " (디렉토리)" : " (파일)"));
            }
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadsDir + "/")
                .setCachePeriod(3600);
    }
}
