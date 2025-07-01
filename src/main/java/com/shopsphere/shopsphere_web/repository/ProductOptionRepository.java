package com.shopsphere.shopsphere_web.repository;

import com.shopsphere.shopsphere_web.entity.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface ProductOptionRepository extends JpaRepository<ProductOption, Integer> {
    List<ProductOption> findByProduct_Id(Integer productId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM ProductOption po WHERE po.product.id = :productId")
    void deleteByProductId(@Param("productId") Integer productId);
}
