package com.shopsphere.shopsphere_web.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import com.shopsphere.shopsphere_web.dto.CartItemDTO;
import com.shopsphere.shopsphere_web.dto.UserDTO;

@Data
public class CartDTO {
    private Integer id;
    private Integer userId;
    private LocalDateTime createdAt;
    private List<CartItemDTO> items;

    @Data
    public static class Response {
        private Integer id;
        private UserDTO.Response user;
        private List<CartItemDTO.Response> items;
        private Integer totalAmount;
        private LocalDateTime createdAt;
    }
}
