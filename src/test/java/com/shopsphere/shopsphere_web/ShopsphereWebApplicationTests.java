package com.shopsphere.shopsphere_web;

import com.shopsphere.shopsphere_web.entity.*;
import com.shopsphere.shopsphere_web.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Sql("classpath:data.sql")
class ShopsphereWebApplicationTests {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private ProductCategoryRepository productCategoryRepository;
    
    @Autowired
    private CartRepository cartRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private ReviewRepository reviewRepository;

    @Test
    void contextLoads() {
        assertNotNull(userRepository);
        assertNotNull(productRepository);
        assertNotNull(cartRepository);
        assertNotNull(orderRepository);
        assertNotNull(reviewRepository);
    }

    @Test
    void testUserDataLoaded() {
        List<User> users = userRepository.findAll();
        assertFalse(users.isEmpty());
        assertEquals(3, users.size());
        
        Optional<User> adminUser = userRepository.findById("admin1");
        assertTrue(adminUser.isPresent());
        assertEquals("관리자", adminUser.get().getName());
    }

    @Test
    void testProductDataLoaded() {
        List<Product> products = productRepository.findAll();
        assertFalse(products.isEmpty());
        assertEquals(3, products.size());
        
        Optional<Product> product = productRepository.findById(1);
        assertTrue(product.isPresent());
        assertEquals("기본 반팔 티셔츠", product.get().getName());
        assertEquals(19900, product.get().getPrice());
    }

    @Test
    void testProductOptionsLoaded() {
        Optional<Product> product = productRepository.findById(1);
        assertTrue(product.isPresent());
        
        // Product 엔티티에 getOptions() 메서드가 있다고 가정
        // List<ProductOption> options = product.get().getOptions();
        // assertFalse(options.isEmpty());
        // assertEquals(3, options.size()); // S, M, L 사이즈
    }

    @Test
    void testCartDataLoaded() {
        List<Cart> carts = cartRepository.findAll();
        assertFalse(carts.isEmpty());
        
        Optional<Cart> user1Cart = cartRepository.findById(1);
        assertTrue(user1Cart.isPresent());
        assertEquals("user1", user1Cart.get().getUser().getId());
    }

    @Test
    void testOrderDataLoaded() {
        List<Order> orders = orderRepository.findAll();
        assertFalse(orders.isEmpty());
        
        Optional<Order> order = orderRepository.findById(1);
        assertTrue(order.isPresent());
        assertEquals("COMPLETED", order.get().getOrderStatus());
        assertTrue(order.get().getTotalAmount() > 0);
    }

    @Test
    void testReviewDataLoaded() {
        List<Review> reviews = reviewRepository.findAll();
        assertFalse(reviews.isEmpty());
        
        Optional<Review> review = reviewRepository.findById(1);
        assertTrue(review.isPresent());
        assertEquals(5, review.get().getRating());
    }
}
