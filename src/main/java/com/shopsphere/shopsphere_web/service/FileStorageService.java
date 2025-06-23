// src/main/java/com/shopsphere/shopsphere_web/service/FileStorageService.java
package com.shopsphere.shopsphere_web.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path baseStorageLocation;
    private final Path profileImageStorageLocation;
    private final Path reviewImageStorageLocation;

    private final String configuredBaseUploadDir; // @Value로 주입받은 원본 문자열 경로 저장
    private final String profileImageSubDir;
    private final String reviewImageSubDir;

    public FileStorageService(
            @Value("${file.base-upload-dir}") String baseUploadDirValue, // 주입받는 파라미터 이름 변경
            @Value("${file.profile-image-subdir}") String profileSubDir,
            @Value("${file.review-image-subdir}") String reviewSubDir) {

        // 👇 주입받은 값에 trim() 적용
        this.configuredBaseUploadDir = baseUploadDirValue.trim(); 
        this.profileImageSubDir = profileSubDir.trim(); // 혹시 모를 공백 제거
        this.reviewImageSubDir = reviewSubDir.trim();   // 혹시 모를 공백 제거

        this.baseStorageLocation = Paths.get(this.configuredBaseUploadDir).toAbsolutePath().normalize();
        this.profileImageStorageLocation = this.baseStorageLocation.resolve(this.profileImageSubDir).normalize();
        this.reviewImageStorageLocation = this.baseStorageLocation.resolve(this.reviewImageSubDir).normalize();

        try {
            Files.createDirectories(this.baseStorageLocation);
            Files.createDirectories(this.profileImageStorageLocation);
            Files.createDirectories(this.reviewImageStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("이미지를 업로드할 디렉토리를 생성할 수 없습니다.", ex);
        }
    }

    // --- 프로필 이미지 관련 메소드 ---
    public String storeProfileImage(MultipartFile file, String userId) {
        // ... (기존 로직 동일)
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = "";
        int i = originalFileName.lastIndexOf('.');
        if (i > 0) {
            extension = originalFileName.substring(i);
        }
        String fileName = userId + "-" + UUID.randomUUID().toString() + extension;

        try {
            if (file.isEmpty()) {
                throw new RuntimeException("프로필 이미지가 비어있습니다.");
            }
            if (fileName.contains("..")) {
                throw new RuntimeException("파일명에 유효하지 않은 경로 시퀀스가 포함되어 있습니다: " + fileName);
            }
            Path targetLocation = this.profileImageStorageLocation.resolve(fileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }
            return fileName;
        } catch (IOException ex) {
            throw new RuntimeException(fileName + " 프로필 이미지를 저장할 수 없습니다. 다시 시도해 주세요.", ex);
        }
    }

    public void deleteProfileImage(String fileName) {
        // ... (기존 로직 동일)
        if (fileName == null || fileName.isEmpty()) return;
        try {
            Path filePath = this.profileImageStorageLocation.resolve(fileName).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            System.err.println(fileName + " 프로필 이미지 파일 삭제 실패: " + ex.getMessage());
        }
    }

    // --- 리뷰 이미지 관련 메소드 ---
    public String storeReviewImage(MultipartFile file, String userId, Integer productId) {
        // ... (기존 로직 동일)
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = "";
        int i = originalFileName.lastIndexOf('.');
        if (i > 0) {
            extension = originalFileName.substring(i);
        }
        String fileName = userId + "-" + productId + "-" + UUID.randomUUID().toString() + extension;

        try {
            if (file.isEmpty()) {
                throw new RuntimeException("리뷰 이미지가 비어있습니다.");
            }
            if (fileName.contains("..")) {
                throw new RuntimeException("파일명에 유효하지 않은 경로 시퀀스가 포함되어 있습니다: " + fileName);
            }
            Path productSpecificReviewDir = this.reviewImageStorageLocation.resolve(String.valueOf(productId));
            Files.createDirectories(productSpecificReviewDir);
            Path targetLocation = productSpecificReviewDir.resolve(fileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }
            return String.valueOf(productId) + "/" + fileName;
        } catch (IOException ex) {
            throw new RuntimeException(fileName + " 리뷰 이미지를 저장할 수 없습니다. 다시 시도해 주세요.", ex);
        }
    }

    public void deleteReviewImage(String filePathSegment) {
        // ... (기존 로직 동일)
         if (filePathSegment == null || filePathSegment.isEmpty()) return;
        try {
            Path filePath = this.reviewImageStorageLocation.resolve(filePathSegment).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            System.err.println(filePathSegment + " 리뷰 이미지 파일 삭제 실패: " + ex.getMessage());
        }
    }

    public String getBaseUploadUrlSegment() {
        String tempBaseDir = this.configuredBaseUploadDir; // ✅ 클래스 필드 사용
        if (tempBaseDir.startsWith("./")) {
            tempBaseDir = tempBaseDir.substring(2);
        } else if (tempBaseDir.startsWith("/")) {
            tempBaseDir = tempBaseDir.substring(1);
        }
        // 경로에 /가 포함되어 있다면 첫 부분만 사용 (예: "uploads/another" -> "uploads")
        // 또는 전체 경로를 사용해야 한다면 이 부분 로직 수정 필요
        if (tempBaseDir.contains("/")) {
            // 웹 URL 세그먼트이므로, 가장 앞단의 디렉토리 이름만 사용하거나,
            // 또는 WebConfig 에서 사용하는 방식과 일치하도록 전체 상대경로를 반환해야 합니다.
            // WebConfig에서 /${baseUploadUrlSegment}/** 와 같이 사용한다면,
            // 여기서는 baseUploadUrlSegment 에 해당하는 부분만 반환해야 합니다.
            // 현재는 첫번째 디렉토리명만 반환하게 되어있음 (예: "uploads")
            return tempBaseDir.substring(0, tempBaseDir.indexOf("/"));
            // 만약 WebConfig 에서 /uploads/profile_images 처럼 전체 경로를 기대한다면
            // return tempBaseDir; // 이렇게 반환해야 할 수도 있습니다.
            // WebConfig 와 FileStorageService 의 URL 생성 로직이 일관되어야 합니다.
        }
        return tempBaseDir; // "uploads" 와 같은 단일 디렉토리 이름
    }

    public String getProfileImageSubDir() {
        return this.profileImageSubDir;
    }

    public String getReviewImageSubDir() {
        return this.reviewImageSubDir;
    }
}