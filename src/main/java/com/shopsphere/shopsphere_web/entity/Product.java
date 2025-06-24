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

    @ManyToOne(fetch = FetchType.LAZY) // LAZY 로딩 유지 (N+1 주의 구간, EntityGraph나 Fetch Join으로 해결)
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

    @ManyToOne(fetch = FetchType.LAZY) // LAZY 로딩 유지
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "sales_volume")
    private Integer salesVolume;

    // ProductImage와의 일대다 관계 설정
    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY) // LAZY 로딩 유지
    @OrderBy("displayOrder ASC")
    private List<ProductImage> images = new ArrayList<>();

    // ProductOption과의 일대다 관계 설정
    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY) // LAZY 로딩 유지
    private List<ProductOption> options = new ArrayList<>();
    
    // 대표 이미지 URL 필드는 ProductImage로 통합 관리한다면 제거. 여기서는 유지한다고 가정.
    @Column(name = "image_url", columnDefinition = "VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String imageUrl;
}