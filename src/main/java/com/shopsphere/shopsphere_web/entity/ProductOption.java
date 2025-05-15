package com.shopsphere.shopsphere_web.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_option")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private String size;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Column(name = "additional_price", nullable = false)
    private Integer additionalPrice;
}
