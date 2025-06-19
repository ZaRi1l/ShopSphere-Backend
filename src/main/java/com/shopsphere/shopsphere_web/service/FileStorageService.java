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

    private final Path profileImageStorageLocation; // 프로필 이미지 전용 저장 위치
    private final String profileImageSubDir = "profile_images"; // 프로필 이미지 하위 디렉토리명

    // 생성자에서 application.properties의 기본 업로드 경로를 받아와 프로필 이미지 경로 조합
    public FileStorageService(@Value("${file.upload-dir}") String baseUploadDir) {
        // 기본 업로드 디렉토리와 프로필 이미지 하위 디렉토리 조합
        Path baseDir = Paths.get(baseUploadDir).toAbsolutePath().normalize();
        this.profileImageStorageLocation = baseDir.resolve(this.profileImageSubDir).normalize();

        try {
            Files.createDirectories(this.profileImageStorageLocation); // 프로필 이미지 디렉토리 없으면 생성
        } catch (Exception ex) {
            throw new RuntimeException("프로필 이미지를 업로드할 디렉토리를 생성할 수 없습니다.", ex);
        }
    }

    // 프로필 이미지 저장 메소드
    public String storeProfileImage(MultipartFile file, String userId) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = "";
        int i = originalFileName.lastIndexOf('.');
        if (i > 0) {
            extension = originalFileName.substring(i);
        }
        String fileName = userId + "-" + UUID.randomUUID().toString() + extension;

        try {
            if (fileName.contains("..")) {
                throw new RuntimeException("파일명에 유효하지 않은 경로 시퀀스가 포함되어 있습니다: " + fileName);
            }
            Path targetLocation = this.profileImageStorageLocation.resolve(fileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }
            return fileName; // 저장된 파일명만 반환
        } catch (IOException ex) {
            throw new RuntimeException(fileName + " 프로필 이미지를 저장할 수 없습니다. 다시 시도해 주세요.", ex);
        }
    }

    // 이전 프로필 이미지 삭제 메소드
    public void deleteProfileImage(String fileName) {
        if (fileName == null || fileName.isEmpty()) return;
        try {
            Path filePath = this.profileImageStorageLocation.resolve(fileName).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            System.err.println(fileName + " 프로필 이미지 파일 삭제 실패: " + ex.getMessage());
        }
    }

    // 저장된 프로필 이미지의 상대 경로 반환 (URL 구성용, 예: "uploads/profile_images/")
    public String getProfileImageRelativePath() {
        // @Value("${file.upload-dir}")에서 ./를 제거하고 profileImageSubDir를 붙임
        // 또는 고정된 문자열 반환
        String baseUploadDirConfig = Paths.get("").toAbsolutePath().relativize(this.profileImageStorageLocation.getParent()).toString();
        // 위 코드는 복잡할 수 있으니, 간단하게 application.properties의 file.upload-dir 값과 profileImageSubDir를 조합
        // 예: return "uploads/profile_images/"; (application.properties의 file.upload-dir이 "uploads"일 경우)
        // file.upload-dir이 "./uploads" 라면:
        String configuredBaseDir = this.profileImageStorageLocation.getParent().getFileName().toString(); // "uploads"
        return configuredBaseDir + "/" + this.profileImageSubDir + "/";
    }
}