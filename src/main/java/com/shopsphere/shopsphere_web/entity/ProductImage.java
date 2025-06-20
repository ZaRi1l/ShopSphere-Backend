// src/main/java/com/shopsphere/shopsphere_web/entity/ProductImage.java
package com.shopsphere.shopsphere_web.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_image") // 테이블명: product_image
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long id; // 이미지 고유 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false) // Product 엔티티와 연관
    private Product product;

    @Column(name = "image_url", nullable = false, length = 512)
    private String imageUrl; // 이미지 파일 경로 또는 URL

    @Column(name = "display_order") // 이미지 표시 순서 (0부터 시작, 0이 대표 썸네일 등)
    private Integer displayOrder;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}