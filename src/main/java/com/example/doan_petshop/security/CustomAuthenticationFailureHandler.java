package com.example.doan_petshop.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {

        String errorMessage = "Tên đăng nhập hoặc mật khẩu không đúng!";

        // Kiểm tra loại exception
        if (exception instanceof EmailNotVerifiedException) {
            errorMessage = exception.getMessage();
        }

        // Lưu message vào session để hiển thị ở login page
        HttpSession session = request.getSession();
        session.setAttribute("errorMsg", errorMessage);

        // Redirect về login page
        response.sendRedirect(request.getContextPath() + "/auth/login");
    }
}
