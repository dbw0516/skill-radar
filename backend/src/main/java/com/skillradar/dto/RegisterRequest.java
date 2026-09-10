package com.skillradar.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, message = "密码至少 6 位")
    private String password;

    private String nickname;
    private Long majorId;
    private String location;
    private String targetLocation;
}
