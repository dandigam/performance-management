package com.rit.performance.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping({"/{path:[^\\.]*}", "/**/{path:[^\\.]*}"})
    public String forwardReactRoutes(HttpServletRequest request, HttpServletResponse response) {
        String path = request.getRequestURI();

        if (path.startsWith("/api/")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/actuator")) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        return "forward:/index.html";
    }
}
