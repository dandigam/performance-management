package com.rit.performance.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "userId is required")
    @JsonAlias("username")
    private String userId;

    @NotBlank(message = "pwd is required")
    @JsonAlias("password")
    private String pwd;
}
