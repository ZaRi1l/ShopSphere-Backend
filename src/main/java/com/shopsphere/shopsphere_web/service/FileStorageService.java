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
    private final Path productImageStorageLocation;

    private final String configuredBaseUploadDir;
    private final String profileImageSubDir;
    private final String reviewImageSubDir;
    private final String productImageSubDir;
    private final String baseResourceHandlerPath;

    public FileStorageService(
            @Value("${file.base-upload-dir}") String baseUploadDirValue,
            @Value("${file.profile-image-subdir}") String profileSubDir,
            @Value("${file.review-image-subdir}") String reviewSubDir,
            @Value("${file.product-image-subdir}") String productSubDir,
            @Value("${file.resource-handler-path}") String resourceHandlerPath
    ) {
        if (resourceHandlerPath == null) {
            throw new IllegalArgumentException("Resource handler path cannot be null");
        }
        this.baseResourceHandlerPath = resourceHandlerPath.trim();
        this.configuredBaseUploadDir = baseUploadDirValue.trim();
        this.profileImageSubDir = profileSubDir.trim();
        this.reviewImageSubDir = reviewSubDir.trim();
        this.productImageSubDir = productSubDir.trim();

        this.baseStorageLocation = Paths.get(this.configuredBaseUploadDir).toAbsolutePath().normalize();
        this.profileImageStorageLocation = this.baseStorageLocation.resolve(this.profileImageSubDir).normalize();
        this.reviewImageStorageLocation = this.baseStorageLocation.resolve(this.reviewImageSubDir).normalize();
        this.productImageStorageLocation = this.baseStorageLocation.resolve(this.productImageSubDir).normalize();

        try {
            Files.createDirectories(this.baseStorageLocation);
            Files.createDirectories(this.profileImageStorageLocation);
            Files.createDirectories(this.reviewImageStorageLocation);
            Files.createDirectories(this.productImageStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("이미지를 업로드할 디렉토리를 생성할 수 없습니다.", ex);
        }
    }

    // --- 프로필 이미지 저장 ---
    public String storeProfileImage(MultipartFile file, String userId) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = "";
        int i = originalFileName.lastIndexOf('.');
        if (i > 0) extension = originalFileName.substring(i);
        String fileName = userId + "-" + UUID.randomUUID().toString() + extension;

        try {
            if (file.isEmpty()) throw new RuntimeException("프로필 이미지가 비어있습니다.");
            if (fileName.contains("..")) throw new RuntimeException("파일명에 유효하지 않은 경로 시퀀스가 포함되어 있습니다: " + fileName);
            Path targetLocation = this.profileImageStorageLocation.resolve(fileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }
            return fileName;
        } catch (IOException ex) {
            throw new RuntimeException(fileName + " 프로필 이미지를 저장할 수 없습니다.", ex);
        }
    }

    public void deleteProfileImage(String fileName) {
        if (fileName == null || fileName.isEmpty()) return;
        try {
            Path filePath = this.profileImageStorageLocation.resolve(fileName).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            System.err.println(fileName + " 프로필 이미지 파일 삭제 실패: " + ex.getMessage());
        }
    }

    // --- 리뷰 이미지 저장 ---
    public String storeReviewImage(MultipartFile file, String userId, Integer productId) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = "";
        int i = originalFileName.lastIndexOf('.');
        if (i > 0) extension = originalFileName.substring(i);
        String fileName = userId + "-" + productId + "-" + UUID.randomUUID().toString() + extension;

        try {
            if (file.isEmpty()) throw new RuntimeException("리뷰 이미지가 비어있습니다.");
            if (fileName.contains("..")) throw new RuntimeException("파일명에 유효하지 않은 경로 시퀀스가 포함되어 있습니다: " + fileName);
            Path productSpecificReviewDir = this.reviewImageStorageLocation.resolve(String.valueOf(productId));
            Files.createDirectories(productSpecificReviewDir);
            Path targetLocation = productSpecificReviewDir.resolve(fileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }
            return productId + "/" + fileName;
        } catch (IOException ex) {
            throw new RuntimeException(fileName + " 리뷰 이미지를 저장할 수 없습니다.", ex);
        }
    }

    public void deleteReviewImage(String filePathSegment) {
        if (filePathSegment == null || filePathSegment.isEmpty()) return;
        try {
            Path filePath = this.reviewImageStorageLocation.resolve(filePathSegment).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            System.err.println(filePathSegment + " 리뷰 이미지 파일 삭제 실패: " + ex.getMessage());
        }
    }

    // --- ✅ 상품 이미지 저장 ---
    // FileStorageService.java 내의 storeProductImage 메소드

    // --- ✅ 상품 이미지 저장 ---
    public String storeProductImage(MultipartFile file, Integer productId) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = "";
        int i = originalFileName.lastIndexOf('.');
        if (i > 0) {
            extension = originalFileName.substring(i);
        }
        
        String fileName = "product-" + productId + "-" + UUID.randomUUID().toString() + extension;
        String productSpecificDirName = String.valueOf(productId); 

        try {
            if (file.isEmpty()) {
                throw new RuntimeException("상품 이미지가 비어있습니다.");
            }
            if (fileName.contains("..") || productSpecificDirName.contains("..")) {
                throw new RuntimeException("파일명 또는 경로에 유효하지 않은 시퀀스가 포함되어 있습니다.");
            }
            
            Path productDir = this.productImageStorageLocation.resolve(productSpecificDirName);
            Files.createDirectories(productDir); 

            Path targetLocation = productDir.resolve(fileName);
            
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            // 웹 접근 URL 생성
            // this.baseResourceHandlerPath 와 this.productImageSubDir 사용
            String webUrlPath = Paths.get(
                this.baseResourceHandlerPath,  // 예: /uploads (클래스 필드, 생성자에서 초기화 가정)
                this.productImageSubDir,       // 예: images/products (생성자에서 초기화된 필드)
                productSpecificDirName,          // 예: "0" (productId)
                fileName                         // 예: "product-0-uuid.jpg"
            ).toString().replace("\\", "/");

            System.out.println("[FileStorageService] Generated Product Image URL: " + webUrlPath);
            return webUrlPath;

        } catch (IOException ex) {
            throw new RuntimeException("상품 이미지 '" + fileName + "'를 저장할 수 없습니다. 원인: " + ex.getMessage(), ex);
        }
    }

    // --- ✅ 상품 이미지 삭제 ---
    public void deleteProductImage(String filePathSegment) {
        if (filePathSegment == null || filePathSegment.isEmpty()) return;
        try {
            Path filePath = this.productImageStorageLocation.resolve(filePathSegment).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            System.err.println(filePathSegment + " 상품 이미지 파일 삭제 실패: " + ex.getMessage());
        }
    }

    // --- ✅ 상품 이미지 서브디렉토리 반환 ---
    public String getConfiguredProductImageSubDir() {
        return this.productImageSubDir;
    }

    public String getProfileImageSubDir() {
        return this.profileImageSubDir;
    }

    public String getReviewImageSubDir() {
        return this.reviewImageSubDir;
    }

    public String getBaseUploadUrlSegment() {
        String tempBaseDir = this.configuredBaseUploadDir;
        if (tempBaseDir.startsWith("./")) tempBaseDir = tempBaseDir.substring(2);
        else if (tempBaseDir.startsWith("/")) tempBaseDir = tempBaseDir.substring(1);
        if (tempBaseDir.contains("/")) {
            return tempBaseDir.substring(0, tempBaseDir.indexOf("/"));
        }
        return tempBaseDir;
    }
}
