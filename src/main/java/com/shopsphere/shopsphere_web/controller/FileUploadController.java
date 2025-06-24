// src/main/java/com/shopsphere/shopsphere_web/controller/FileUploadController.java
package com.shopsphere.shopsphere_web.controller;

import com.shopsphere.shopsphere_web.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
// import java.util.stream.Collectors; // 현재 미사용

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileStorageService fileStorageService;

    // uploadProductImage, uploadMultipleProductImages 메소드는 이전 답변과 동일

    @PostMapping("/upload/product-image/{productId}")
    public ResponseEntity<?> uploadProductImage(@RequestParam("file") MultipartFile file, @PathVariable Integer productId) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "업로드할 파일이 없습니다."));
        }
        try {
            String fileUrl = fileStorageService.storeProductImage(file, productId);
            return ResponseEntity.ok(Map.of("imageUrl", fileUrl));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "파일 업로드 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/upload/product-images/{productId}")
    public ResponseEntity<?> uploadMultipleProductImages(@RequestParam("files") List<MultipartFile> files, @PathVariable Integer productId) {
        if (files == null || files.isEmpty() || files.stream().allMatch(MultipartFile::isEmpty)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "업로드할 파일이 없습니다."));
        }

        List<String> uploadedUrls = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                try {
                    String fileUrl = fileStorageService.storeProductImage(file, productId);
                    uploadedUrls.add(fileUrl);
                } catch (RuntimeException e) {
                    errorMessages.add(file.getOriginalFilename() + ": " + e.getMessage());
                }
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("uploadedImageUrls", uploadedUrls);
        if (!errorMessages.isEmpty()) {
            response.put("errors", errorMessages);
            if (uploadedUrls.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        }
        return ResponseEntity.ok(response);
    }
    
    /**
     * 저장된 상품 이미지를 삭제합니다.
     * 클라이언트가 전달하는 filePathSegment는 productImageSubDir 다음부터의 경로여야 합니다.
     * 예: FileStorageService.storeProductImage 가 "/uploads/images/products/1/uuid.jpg" 를 반환했고,
     *    application.properties의 file.base-resource-handler-path="/uploads" 이고,
     *    file.product-image-subdir="images/products" 라면,
     *    클라이언트는 "1/uuid.jpg" 를 filePathSegment로 전달해야 합니다.
     *
     * @param payload "filePathSegment" 키로 "productId/fileName.jpg" 형태의 경로를 포함하는 Map
     * @return 성공 또는 실패 메시지
     */
    @DeleteMapping("/delete/product-image")
    public ResponseEntity<?> deleteProductImage(@RequestBody Map<String, String> payload) {
        String filePathSegment = payload.get("filePathSegment"); // 예: "1/uuid.jpg"
        if (filePathSegment == null || filePathSegment.isBlank()) {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "삭제할 파일 경로 세그먼트가 필요합니다. (예: productId/fileName.jpg)"));
        }
        try {
            fileStorageService.deleteProductImage(filePathSegment);
            return ResponseEntity.ok(Map.of("message", 
                fileStorageService.getConfiguredProductImageSubDir() + "/" + filePathSegment + " 파일이 성공적으로 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("message", 
                                 fileStorageService.getConfiguredProductImageSubDir() + "/" + filePathSegment + " 파일 삭제 실패: " + e.getMessage()));
        }
    }
}