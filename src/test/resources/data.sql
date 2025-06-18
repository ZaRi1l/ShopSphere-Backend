-- 사용자 데이터
INSERT INTO `user` (`user_id`, `email`, `password`, `name`, `phone_number`, `address`, `role`, `created_at`) VALUES
('user1', 'user1@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MQDq5phQaV8mzstzWfJ3rKBVq0JQr6y', '김사용자', '010-1234-5678', '서울시 강남구 테헤란로 123', 'USER', NOW()),
('user2', 'user2@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MQDq5phQaV8mzstzWfJ3rKBVq0JQr6y', '이사용자', '010-8765-4321', '서울시 서초구 반포대로 456', 'USER', NOW()),
('admin1', 'admin@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MQDq5phQaV8mzstzWfJ3rKBVq0JQr6y', '관리자', '010-1111-2222', '서울시 종로구 세종대로 1', 'ADMIN', NOW());

-- 상품 카테고리
INSERT INTO `product_category` (`category_id`, `name`, `parent_id`, `created_at`) VALUES
(1, '의류', NULL, NOW()),
(2, '상의', 1, NOW()),
(3, '하의', 1, NOW()),
(4, '신발', NULL, NOW()),
(5, '운동화', 4, NOW()),
(6, '구두', 4, NOW());

-- 상품
INSERT INTO `product` (`product_id`, `category_id`, `name`, `description`, `price`, `stock_quantity`, `image_url`, `created_at`, `user_id`, `sales_volume`) VALUES
(1, 2, '기본 반팔 티셔츠', '편안한 소재의 기본 반팔 티셔츠', 19900, 100, '/uploads/images/products/1/1.png', NOW(), 'user1', 50),
(2, 3, '슬림 핏 청바지', '세련된 핏의 슬림 청바지', 49900, 50, '/uploads/images/products/2/1.png', NOW(), 'user1', 30),
(3, 5, '러닝화', '가벼운 착용감의 러닝화', 89000, 30, '/uploads/images/products/3/1.png', NOW(), 'user2', 20);

-- 상품 옵션
INSERT INTO `product_option` (`option_id`, `product_id`, `size`, `stock_quantity`, `additional_price`) VALUES
(1, 1, 'S', 30, 0),
(2, 1, 'M', 40, 0),
(3, 1, 'L', 30, 2000),
(4, 2, '28', 20, 0),
(5, 2, '30', 20, 0),
(6, 2, '32', 10, 0),
(7, 3, '250', 10, 0),
(8, 3, '260', 10, 0),
(9, 3, '270', 10, 0);

-- 상품 이미지
INSERT INTO `product_image` (`product_image_id`, `product_id`, `image_url`, `created_at`) VALUES
(1, 1, '/uploads/images/products/1/1.png', NOW()),
(2, 1, '/uploads/images/products/1/2.png', NOW()),
(3, 2, '/uploads/images/products/2/1.png', NOW()),
(4, 3, '/uploads/images/products/3/1.png', NOW()),
(5, 3, '/uploads/images/products/3/2.png', NOW());

-- 장바구니
INSERT INTO `cart` (`cart_id`, `user_id`, `created_at`) VALUES
(1, 'user1', NOW()),
(2, 'user2', NOW());

-- 장바구니 상품
INSERT INTO `cart_item` (`cart_item_id`, `cart_id`, `product_id`, `quantity`, `created_at`) VALUES
(1, 1, 1, 2, NOW()),
(2, 1, 2, 1, NOW()),
(3, 2, 3, 1, NOW());

-- 주문
INSERT INTO `orders` (`order_id`, `user_id`, `order_date`, `order_status`, `shipping_address`, `total_amount`, `payment_method`, `transaction_id`, `created_at`) VALUES
(1, 'user1', NOW(), 'COMPLETED', '서울시 강남구 테헤란로 123', 89700, 'CREDIT_CARD', 'txn_123456789', NOW()),
(2, 'user2', NOW(), 'PROCESSING', '서울시 서초구 반포대로 456', 89000, 'KAKAO_PAY', 'txn_987654321', NOW());

-- 주문 상품
INSERT INTO `order_item` (`order_item_id`, `order_id`, `product_id`, `option_id`, `quantity`, `price`, `created_at`) VALUES
(1, 1, 1, 2, 2, 39800, NOW()),
(2, 1, 2, 4, 1, 49900, NOW()),
(3, 2, 3, 8, 1, 89000, NOW());

-- 리뷰
INSERT INTO `review` (`review_id`, `user_id`, `product_id`, `rating`, `comment`, `created_at`) VALUES
(1, 'user1', 1, 5, '너무 편안하고 좋아요!', NOW()),
(2, 'user2', 3, 4, '가볍고 달리기 좋아요', NOW());
