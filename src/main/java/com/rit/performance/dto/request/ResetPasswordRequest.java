package com.rit.performance.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ResetPasswordRequest {
    private String token;
    private String newPassword;
}
