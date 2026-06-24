package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "username khong duoc de trong")
    @Size(min = 4, max = 50, message = "username phai tu 4-50 ky tu")
    private String username;

    @NotBlank(message = "password khong duoc de trong")
    @Size(min = 6, message = "password phai it nhat 6 ky tu")
    private String password;

    @NotBlank(message = "email khong duoc de trong")
    @Email(message = "email khong dung dinh dang")
    private String email;
}
