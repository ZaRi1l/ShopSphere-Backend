package com.shopsphere.shopsphere_web.service;

import com.shopsphere.shopsphere_web.dto.*;
import com.shopsphere.shopsphere_web.entity.*;
import com.shopsphere.shopsphere_web.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 클래스 레벨 기본 트랜잭션: 읽기 전용
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductOptionRepository optionRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository; // 리뷰 정보 연동을 위해 유지

    /**
     * 새로운 상품을 생성합니다. (이미지 정보 포함)
     *
     * @param userId  상품을 등록하는 사용자 ID
     * @param request 생성할 상품 정보 DTO
     * @return 생성된 상품 정보 DTO
     * @throws IllegalArgumentException 사용자 또는 카테고리를 찾을 수 없는 경우
     */
    @Transactional // 쓰기 트랜잭션 적용
    public ProductDTO.Response createProduct(String userId, ProductDTO.CreateRequest request) {
        User seller = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다. ID: " + userId));

        ProductCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다. ID: " + request.getCategoryId()));

        Product product = Product.builder()
                .category(category)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .user(seller)
                .salesVolume(0) // 초기 판매량 0
                .createdAt(LocalDateTime.now())
                .images(new ArrayList<>()) // 이미지 리스트 초기화
                .options(new ArrayList<>()) // 옵션 리스트 초기화
                .build();

        // Product 엔티티 1차 저장 (ID 확보 목적)
        Product savedProduct = productRepository.save(product);

        // 대표 이미지 처리 (Product 엔티티의 imageUrl 필드를 사용하지 않고 ProductImage로 통합 관리하는 경우)
        // CreateRequest에 imageUrl이 대표 이미지 URL이라고 가정
        if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
            ProductImage mainImage = ProductImage.builder()
                    .product(savedProduct)
                    .imageUrl(request.getImageUrl())
                    .displayOrder(0) // 대표 이미지는 0번
                    .build();
            productImageRepository.save(mainImage);
            // savedProduct.getImages().add(mainImage); // 양방향 관계 설정 시 필요할 수 있음 (JPA가 관리)
        }
        // ProductDTO.CreateRequest에 List<ProductImageDTO.CreateRequest> 같은 형태로 여러 이미지 URL을 받는다면 여기서 추가 처리

        // 상품 옵션 저장
        if (request.getOptions() != null && !request.getOptions().isEmpty()) {
            request.getOptions().forEach(optionRequest -> {
                ProductOption option = ProductOption.builder()
                        .product(savedProduct)
                        .size(optionRequest.getSize())
                        .stockQuantity(optionRequest.getStockQuantity())
                        .additionalPrice(optionRequest.getAdditionalPrice())
                        .build();
                optionRepository.save(option);
                // savedProduct.getOptions().add(option); // 양방향 관계 설정 시 필요할 수 있음 (JPA가 관리)
            });
        }
        
        // Product 엔티티의 images나 options 컬렉션이 변경되었다면, 명시적으로 productRepository.save(savedProduct)를 호출하거나,
        // Cascade 설정 및 JPA 변경 감지에 의해 자동으로 처리될 수 있습니다.
        // 가장 확실한 것은 연관 엔티티 저장 후 부모 엔티티를 다시 조회하는 것입니다.
        Product finalProduct = productRepository.findById(savedProduct.getId())
                 .orElseThrow(() -> new IllegalStateException("상품 저장 후 조회에 실패했습니다. ID: " + savedProduct.getId()));

        return convertToProductResponse(finalProduct);
    }

    /**
     * 특정 ID의 상품 상세 정보를 조회합니다.
     *
     * @param productId 조회할 상품 ID
     * @return 조회된 상품 DTO, 없으면 null
     */
    public ProductDTO.Response getProduct(Integer productId) {
        return productRepository.findById(productId)
                .map(this::convertToProductResponse)
                .orElse(null);
    }

    /**
     * 필터링 조건에 맞는 상품 목록을 조회합니다.
     *
     * @param categoryId 카테고리 ID (선택 사항)
     * @param minPrice   최소 가격 (선택 사항)
     * @param maxPrice   최대 가격 (선택 사항)
     * @param sortOption 정렬 옵션 문자열
     * @return 조건에 맞는 상품 DTO 목록
     */
    public List<ProductDTO.Response> getProducts(
            Integer categoryId,
            Integer minPrice,
            Integer maxPrice,
            String sortOption) {

        Sort sort = determineSort(sortOption);
        List<Product> products;

        // Specification<Product>을 사용하면 이 조건 조합 부분을 더 깔끔하게 만들 수 있습니다.
        // 여기서는 Repository에 정의된 여러 findBy... 메서드를 조합하는 예시입니다.
        if (categoryId != null) {
            if (minPrice != null && maxPrice != null) {
                products = productRepository.findByCategory_IdAndPriceBetween(categoryId, minPrice, maxPrice, sort);
            } else if (minPrice != null) {
                products = productRepository.findByCategory_IdAndPriceGreaterThanEqual(categoryId, minPrice, sort);
            } else if (maxPrice != null) {
                products = productRepository.findByCategory_IdAndPriceLessThanEqual(categoryId, maxPrice, sort);
            } else {
                products = productRepository.findByCategory_Id(categoryId, sort);
            }
        } else { // categoryId가 null인 경우
            if (minPrice != null && maxPrice != null) {
                products = productRepository.findByPriceBetween(minPrice, maxPrice, sort);
            } else if (minPrice != null) {
                products = productRepository.findByPriceGreaterThanEqual(minPrice, sort);
            } else if (maxPrice != null) {
                products = productRepository.findByPriceLessThanEqual(maxPrice, sort);
            } else {
                products = productRepository.findAll(sort);
            }
        }

        return products.stream()
                .map(this::convertToProductResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 모든 상품 목록을 조회합니다. (getProducts 호출)
     * @return 모든 상품 DTO 목록
     */
    public List<ProductDTO.Response> getAllProducts() {
        return getProducts(null, null, null, "musinsa_recommend"); // 기본 정렬 적용
    }

    /**
     * 특정 카테고리에 속한 모든 상품 목록을 조회합니다. (getProducts 호출)
     * @param categoryId 카테고리 ID
     * @return 해당 카테고리의 상품 DTO 목록
     */
    public List<ProductDTO.Response> getProductsByCategory(Integer categoryId) {
         return getProducts(categoryId, null, null, "musinsa_recommend"); // 기본 정렬 적용
    }


    /**
     * 특정 판매자가 등록한 모든 상품 목록을 조회합니다.
     *
     * @param userId 판매자 ID
     * @return 해당 판매자의 상품 DTO 목록
     */
    public List<ProductDTO.Response> getProductsBySeller(String userId) {
        // 판매자 ID로 상품 조회 시에도 정렬 옵션을 적용할 수 있도록 확장 가능
        Sort sort = determineSort("created_at_desc"); // 예: 판매자 상품은 최신순 기본 정렬
        return productRepository.findByUser_Id(userId, sort).stream()
                .map(this::convertToProductResponse)
                .collect(Collectors.toList());
    }

    /**
     * 기존 상품 정보를 수정합니다.
     *
     * @param userId    수정을 시도하는 사용자 ID (권한 확인용)
     * @param productId 수정할 상품 ID
     * @param request   수정할 상품 정보 DTO
     * @return 수정된 상품 DTO
     * @throws IllegalArgumentException 상품 또는 카테고리를 찾을 수 없는 경우
     * @throws SecurityException        상품 수정 권한이 없는 경우
     */
    @Transactional // 쓰기 트랜잭션 적용
    public ProductDTO.Response updateProduct(String userId, Integer productId, ProductDTO.UpdateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + productId));

        if (!product.getUser().getId().equals(userId)) {
            throw new SecurityException("상품을 수정할 권한이 없습니다.");
        }

        if (request.getCategoryId() != null) {
            ProductCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다. ID: " + request.getCategoryId()));
            product.setCategory(category);
        }
        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getStockQuantity() != null) product.setStockQuantity(request.getStockQuantity());
        
        // Product 엔티티의 imageUrl 필드를 직접 사용한다면 아래 코드 사용.
        // 현재는 ProductImage로 관리하므로, 이미지 수정은 별도 로직 필요 (예: 기존 이미지 삭제, 새 이미지 추가)
        // if (request.getImageUrl() != null) product.setImageUrl(request.getImageUrl());

        // 이미지 수정 로직 (ProductImage 엔티티 기준)
        // 1. request에 삭제할 이미지 ID 목록이 있다면 productImageRepository.deleteAllByIds(...)
        // 2. request에 추가할 이미지 URL 목록이 있다면 새로 ProductImage 생성 및 저장
        // 이 부분은 복잡하므로 ProductImageService 등을 만들어 위임하는 것이 좋음. 여기서는 생략.

        // 옵션 수정 로직
        if (request.getOptions() != null) {
            // 기존 옵션 전체 삭제 후 새로 추가하는 방식 또는 개별 옵션 업데이트/추가/삭제 방식
            // 여기서는 기존 옵션은 유지하고, 요청에 ID가 있는 옵션은 업데이트, ID가 없는 옵션은 새로 추가하는 예시 (삭제는 별도)
            optionRepository.deleteAll(product.getOptions()); // 일단 기존 옵션 모두 삭제 (간단한 방식)
            product.getOptions().clear(); // 컬렉션에서도 제거

            for (ProductOptionDTO.UpdateRequest optionRequest : request.getOptions()) {
                 ProductOption option = ProductOption.builder()
                        .product(product)
                        .size(optionRequest.getSize())
                        .stockQuantity(optionRequest.getStockQuantity())
                        .additionalPrice(optionRequest.getAdditionalPrice())
                        .build();
                optionRepository.save(option);
                product.getOptions().add(option);
            }
        }

        Product updatedProduct = productRepository.save(product); // 변경 감지로 자동 업데이트되지만, 명시적 save
        return convertToProductResponse(productRepository.findById(updatedProduct.getId())
                                     .orElseThrow(() -> new IllegalStateException("상품 업데이트 후 조회에 실패했습니다. ID: " + updatedProduct.getId())));
    }

    /**
     * 특정 상품을 삭제합니다.
     *
     * @param userId    삭제를 시도하는 사용자 ID (권한 확인용)
     * @param productId 삭제할 상품 ID
     * @throws IllegalArgumentException 상품을 찾을 수 없는 경우
     * @throws SecurityException        상품 삭제 권한이 없는 경우
     */
    @Transactional // 쓰기 트랜잭션 적용
    public void deleteProduct(String userId, Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + productId));

        if (!product.getUser().getId().equals(userId)) {
            throw new SecurityException("상품을 삭제할 권한이 없습니다.");
        }
        
        // Product 엔티티의 CascadeType.ALL, orphanRemoval=true 설정에 의해
        // 연관된 ProductOption, ProductImage도 함께 삭제됨.
        productRepository.deleteById(productId);
    }

    /**
     * 키워드를 사용하여 상품명 또는 카테고리명에서 상품을 검색하고 정렬합니다.
     *
     * @param keyword    검색 키워드
     * @param sortOption 정렬 옵션 문자열
     * @return 검색된 상품 DTO 목록
     */
    public List<ProductDTO.Response> searchProductsByKeyword(String keyword, String sortOption) {
        List<Product> productsByName = productRepository.findByNameContainingIgnoreCase(keyword);
        
        List<ProductCategory> foundCategories = categoryRepository.findByNameContainingIgnoreCase(keyword);
        List<Product> productsByCategoryName = new ArrayList<>();
        if (!foundCategories.isEmpty()) {
            List<Integer> categoryIds = foundCategories.stream().map(ProductCategory::getId).collect(Collectors.toList());
            productsByCategoryName = productRepository.findByCategory_IdIn(categoryIds);
        }
    
        Set<Product> combinedProductSet = new HashSet<>(productsByName);
        combinedProductSet.addAll(productsByCategoryName);
    
        List<Product> sortedProducts = new ArrayList<>(combinedProductSet);
        
        // 정렬 로직 (determineSort 사용 가능하나, 여기서는 Comparator 예시 유지)
        Comparator<Product> comparator = Comparator.comparing(Product::getSalesVolume, Comparator.nullsLast(Comparator.reverseOrder()))
                                                .thenComparing(Product::getId); // 기본 정렬: 판매량 많은 순
        switch (sortOption) {
            case "sales_volume_desc": // 이미 기본값
                break;
            case "created_at_desc":
                comparator = Comparator.comparing(Product::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                                   .thenComparing(Product::getId);
                break;
            case "price_asc":
                comparator = Comparator.comparing(Product::getPrice).thenComparing(Product::getId);
                break;
            case "price_desc":
                comparator = Comparator.comparing(Product::getPrice, Comparator.reverseOrder())
                                   .thenComparing(Product::getId);
                break;
            case "musinsa_recommend": // 기본 정렬과 동일하게 처리
            default:
                break; 
        }
        sortedProducts.sort(comparator);
    
        return sortedProducts.stream()
                .map(this::convertToProductResponse)
                .collect(Collectors.toList());
    }
    
    // --- Helper Methods ---

    /**
     * 정렬 옵션 문자열에 따라 Sort 객체를 결정합니다.
     */
    private Sort determineSort(String sortOption) {
        switch (sortOption) {
            case "sales_volume_desc":
                return Sort.by(Sort.Direction.DESC, "salesVolume").and(Sort.by(Sort.Direction.ASC, "id"));
            case "created_at_desc":
                return Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.ASC, "id"));
            case "price_asc":
                return Sort.by(Sort.Direction.ASC, "price").and(Sort.by(Sort.Direction.ASC, "id"));
            case "price_desc":
                return Sort.by(Sort.Direction.DESC, "price").and(Sort.by(Sort.Direction.ASC, "id"));
            case "musinsa_recommend":
            default: // 기본은 판매량 많은 순
                return Sort.by(Sort.Direction.DESC, "salesVolume").and(Sort.by(Sort.Direction.ASC, "id"));
        }
    }

    /**
     * Product 엔티티를 ProductDTO.Response 로 변환합니다.
     */
    public ProductDTO.Response convertToProductResponse(Product product) { // 메서드 이름을 맞춰주거나, OrderService에서 호출하는 이름을 변경
        if (product == null) return null;

        ProductDTO.Response response = new ProductDTO.Response();
        response.setId(product.getId());
        // product.getCategory()가 null일 수 있으므로 null 체크 추가 또는 convertToCategoryResponse 내부에서 처리
        if (product.getCategory() != null) {
            response.setCategory(convertToCategoryResponse(product.getCategory()));
        }
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setStockQuantity(product.getStockQuantity());
        response.setCreatedAt(product.getCreatedAt());
        // product.getUser()가 null일 수 있으므로 null 체크 추가 또는 convertToUserResponse 내부에서 처리
        if (product.getUser() != null) {
            response.setSeller(convertToUserResponse(product.getUser()));
        }
        response.setSalesVolume(product.getSalesVolume());

        if (product.getImages() != null) {
            response.setImages(product.getImages().stream()
                    .map(this::convertToProductImageDTO) // 이 메서드들도 ProductService 내에 존재해야 함
                    .collect(Collectors.toList()));
        } else {
            response.setImages(new ArrayList<>());
        }

        if (product.getOptions() != null) {
             response.setOptions(product.getOptions().stream()
                    .map(this::convertToOptionResponse) // 이 메서드들도 ProductService 내에 존재해야 함
                    .collect(Collectors.toList()));
        } else {
            response.setOptions(new ArrayList<>());
        }
       
        // reviewRepository가 null이 아닌지 확인 (주입되었는지)
        if (reviewRepository != null) {
            response.setReviewCount(reviewRepository.countByProductId(product.getId()));
            response.setAverageRating(reviewRepository.findAverageRatingByProductId(product.getId()).orElse(0.0));
        } else {
            response.setReviewCount(0L); // reviewRepository가 없다면 기본값
            response.setAverageRating(0.0);
        }
        response.setInterestCount(0L); // 관심 수 기능 제외로 0 설정

        return response;
    }

    // convertToProductResponse 내부에서 사용되는 private 헬퍼 메서드들
    // (이전 ProductService 코드에 있던 private 메서드들을 여기에 그대로 두거나,
    // 필요에 따라 public으로 변경해야 할 수도 있지만, convertToProductResponse가 public이므로
    // 내부에서 호출하는 private 메서드는 그대로 두어도 됩니다.)

    private ProductImageDTO convertToProductImageDTO(ProductImage image) {
        if (image == null) return null;
        ProductImageDTO dto = new ProductImageDTO();
        dto.setId(image.getId());
        dto.setImageUrl(image.getImageUrl());
        dto.setDisplayOrder(image.getDisplayOrder());
        dto.setCreatedAt(image.getCreatedAt());
        return dto;
    }

    private ProductOptionDTO.Response convertToOptionResponse(ProductOption option) {
        if (option == null) return null;
        ProductOptionDTO.Response dto = new ProductOptionDTO.Response();
        dto.setId(option.getId());
        dto.setSize(option.getSize());
        dto.setStockQuantity(option.getStockQuantity());
        dto.setAdditionalPrice(option.getAdditionalPrice());
        return dto;
    }

    private ProductCategoryDTO.Response convertToCategoryResponse(ProductCategory category) {
        if (category == null) return null;
        ProductCategoryDTO.Response dto = new ProductCategoryDTO.Response();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setCreatedAt(category.getCreatedAt());
        return dto;
    }

    private UserDTO.Response convertToUserResponse(User user) {
        if (user == null) return null;
        UserDTO.Response dto = new UserDTO.Response();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setAddress(user.getAddress());
        if (user.getRole() != null) {
            dto.setRole(user.getRole().toString());
        }
        return dto;
    }
}