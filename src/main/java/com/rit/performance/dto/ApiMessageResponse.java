package com.rit.performance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiMessageResponse(String type, String code, String message) {
    public static ApiMessageResponse success(String message) {
        return new ApiMessageResponse("SUCCESS", null, message);
    }

    public static ApiMessageResponse warning(String code, String message) {
        return new ApiMessageResponse("WARNING", code, message);
    }

    public static ApiMessageResponse error(String code, String message) {
        return new ApiMessageResponse("ERROR", code, message);
    }
}
