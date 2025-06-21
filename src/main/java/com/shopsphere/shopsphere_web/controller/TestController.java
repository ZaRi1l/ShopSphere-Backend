package com.shopsphere.shopsphere_web.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class TestController {
    @PostMapping(value = "/api/test-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> handleFileUpload(@RequestPart("description") String description,
                                                 @RequestPart(value = "file", required = false) MultipartFile file) {
        System.out.println("Description: " + description);
        if (file != null && !file.isEmpty()) {
            System.out.println("File name: " + file.getOriginalFilename());
            System.out.println("File size: " + file.getSize());
        } else {
            System.out.println("No file uploaded.");
        }
        return ResponseEntity.ok("Test upload successful");
    }
}
