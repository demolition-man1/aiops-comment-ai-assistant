package com.aiops.dto;

import lombok.Data;

@Data
public class RegisterDTO {
    private String username;
    private String password;
    private String nickname;
    private String email;
}

