package com.shopsphere.shopsphere_web.repository;

import com.shopsphere.shopsphere_web.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Integer> {
    List<ProductCategory> findByParent_Id(Integer parentId);

    // 카테고리명에 키워드가 포함된 카테고리 검색 (대소문자 무시)
    List<ProductCategory> findByNameContainingIgnoreCase(String keyword);

    // 최상위 카테고리(parent_id가 null인 카테고리) 조회 메서드 추가 (예시)
    List<ProductCategory> findByParentIsNull();
}
