package com.shopsphere.shopsphere_web.repository;

import com.shopsphere.shopsphere_web.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Sort; // Sort 임포트

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByCategory_Id(Integer categoryId);
    List<Product> findByUser_Id(String userId);
    List<Product> findByUser_Id(String userId, Sort sort);
    List<Product> findByNameContainingIgnoreCase(String keyword);

    @Query("SELECT p FROM Product p WHERE p.category.id IN :categoryIds")
    List<Product> findByCategory_IdIn(@Param("categoryIds") List<Integer> categoryIds);

    // 가격 범위로 상품 조회 (카테고리 ID는 선택적)
    // JPQL을 사용하여 동적으로 조건을 구성하는 것은 복잡하므로, 여러 메서드를 만들거나 Specification 사용.
    // 여기서는 간단한 예시로, 모든 파라미터가 제공될 때의 경우를 가정.
    // 또는 서비스 레이어에서 여러 Repository 메소드 호출 결과를 조합.

    // 예시 1: 카테고리 ID 와 가격 범위로 조회 (Sort 객체 추가)
    List<Product> findByCategory_IdAndPriceBetween(Integer categoryId, Integer minPrice, Integer maxPrice, Sort sort);
    List<Product> findByCategory_IdAndPriceGreaterThanEqual(Integer categoryId, Integer minPrice, Sort sort);
    List<Product> findByCategory_IdAndPriceLessThanEqual(Integer categoryId, Integer maxPrice, Sort sort);
    List<Product> findByCategory_Id(Integer categoryId, Sort sort); // 카테고리만 있고 가격 필터 없을 때

    // 예시 2: 카테고리 없이 가격 범위로만 조회 (Sort 객체 추가)
    List<Product> findByPriceBetween(Integer minPrice, Integer maxPrice, Sort sort);
    List<Product> findByPriceGreaterThanEqual(Integer minPrice, Sort sort);
    List<Product> findByPriceLessThanEqual(Integer maxPrice, Sort sort);

    // findAll에 Sort 적용 가능
    List<Product> findAll(Sort sort);

    // Specification<Product> 사용 예시 (더 유연함)
    // List<Product> findAll(Specification<Product> spec, Sort sort);
}