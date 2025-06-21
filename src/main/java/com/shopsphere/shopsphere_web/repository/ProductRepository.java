package com.shopsphere.shopsphere_web.repository;

import com.shopsphere.shopsphere_web.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByCategory_Id(Integer categoryId);
    List<Product> findByUser_Id(String userId);

    // 상품명에 키워드가 포함된 상품 검색 (대소문자 무시)
    List<Product> findByNameContainingIgnoreCase(String keyword);

    // 여러 카테고리 ID에 해당하는 상품들 검색
    // JPQL을 사용하여 Product 엔티티의 category 필드의 id를 기준으로 조회
    @Query("SELECT p FROM Product p WHERE p.category.id IN :categoryIds")
    List<Product> findByCategory_IdIn(@Param("categoryIds") List<Integer> categoryIds);
}
