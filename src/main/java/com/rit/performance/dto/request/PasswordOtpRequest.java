package com.rit.performance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PasswordOtpRequest {
    @NotBlank(message = "Setup or reset token is required.")
    @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "Invalid setup or reset token.")
    private String token;
}
