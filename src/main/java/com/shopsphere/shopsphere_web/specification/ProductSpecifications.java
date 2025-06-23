package com.shopsphere.shopsphere_web.specification;

import com.shopsphere.shopsphere_web.entity.Product;
import com.shopsphere.shopsphere_web.entity.ProductCategory; // ProductCategory 엔티티 import
import jakarta.persistence.criteria.Join; // Join import
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecifications {

    public static Specification<Product> withCategoryId(Integer categoryId) {
        return (root, query, criteriaBuilder) -> {
            if (categoryId == null) {
                return criteriaBuilder.isTrue(criteriaBuilder.literal(true)); // 항상 참 조건
            }
            // Product 엔티티의 category 필드를 통해 ProductCategory 엔티티의 id 필드와 조인하여 비교
            Join<Product, ProductCategory> categoryJoin = root.join("category");
            return criteriaBuilder.equal(categoryJoin.get("id"), categoryId);
        };
    }

    public static Specification<Product> minPrice(Integer minPrice) {
        return (root, query, criteriaBuilder) -> {
            if (minPrice == null) {
                return criteriaBuilder.isTrue(criteriaBuilder.literal(true));
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
        };
    }

    public static Specification<Product> maxPrice(Integer maxPrice) {
        return (root, query, criteriaBuilder) -> {
            if (maxPrice == null) {
                return criteriaBuilder.isTrue(criteriaBuilder.literal(true));
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
        };
    }

    // 필요에 따라 다른 Specification 추가 (예: 키워드 검색, 재고 유무 등)
}