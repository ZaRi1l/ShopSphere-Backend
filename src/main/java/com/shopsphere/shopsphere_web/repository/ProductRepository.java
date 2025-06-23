package com.shopsphere.shopsphere_web.repository;

import com.shopsphere.shopsphere_web.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph; // EntityGraph 사용 시
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {

    // JpaSpecificationExecutor의 findAll(Specification, Pageable)을 사용하므로 별도 메소드 불필요
    // 단, N+1 해결을 위해 EntityGraph를 사용하려면 findAll 메소드를 오버라이드 하거나
    // 새로운 이름의 메소드에 @EntityGraph 어노테이션을 추가해야 합니다.

    // 예시: ID로 조회 시 EntityGraph를 사용하여 연관 엔티티 EAGER 로딩 (N+1 방지)
    @Override
    @EntityGraph(attributePaths = {"category", "user", "images", "options"})
    Optional<Product> findById(Integer id);

    // Page<Product> findAll(Specification<Product> spec, Pageable pageable);
    // 위 메소드에 @EntityGraph 적용은 JpaSpecificationExecutor와 함께 사용할 때 주의 필요
    // 보통 Specification과 EntityGraph를 함께 쓰려면 커스텀 Repository 구현이나
    // Criteria API를 직접 사용하는 방식이 더 명확할 수 있습니다.
    // 또는, 서비스 레이어에서 조회 후 LAZY 로딩된 엔티티를 명시적으로 초기화 (비권장)
    // 가장 좋은 것은 DTO 프로젝션을 사용하는 것입니다.

    // 기존 메소드들
    @EntityGraph(attributePaths = {"category", "user", "images"}) // 목록 조회에도 EntityGraph 적용 예시
    List<Product> findByCategory_Id(Integer categoryId);

    @EntityGraph(attributePaths = {"category", "user", "images"})
    List<Product> findByUser_Id(String userId);

    List<Product> findByNameContainingIgnoreCase(String keyword);

    @Query("SELECT p FROM Product p WHERE p.category.id IN :categoryIds")
    List<Product> findByCategory_IdIn(@Param("categoryIds") List<Integer> categoryIds);
}