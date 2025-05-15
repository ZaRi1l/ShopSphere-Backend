package com.shopsphere.shopsphere_web.repository;

import com.shopsphere.shopsphere_web.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
    List<Review> findByProduct_Id(Integer productId);
    List<Review> findByUser_Id(String userId);
}
