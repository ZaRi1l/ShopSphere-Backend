package com.shopsphere.shopsphere_web.repository;

import com.shopsphere.shopsphere_web.entity.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductOptionRepository extends JpaRepository<ProductOption, Integer> {
    List<ProductOption> findByProduct_Id(Integer productId);
}
