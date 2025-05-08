package com.shopsphere.shopsphere_web.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String password;
    private String name;
    private String phoneNumber;
    private String address;
}