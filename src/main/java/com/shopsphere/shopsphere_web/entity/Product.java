package com.shopsphere.shopsphere_web.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ProductCategory category;

    @Column(nullable = false, columnDefinition = "VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String name;

    @Column(columnDefinition = "TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String description;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "sales_volume")
    private Integer salesVolume;

    @Column(name = "image_url", columnDefinition = "VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String imageUrl; // 대표 이미지 URL (필요시 유지 또는 ProductImage로 통합 관리)

    // ProductImage와의 일대다 관계 설정
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC") // displayOrder 기준으로 정렬
    private List<ProductImage> images = new ArrayList<>(); // 필드명 images로 통일 (ProductImage 엔티티 리스트)
}
