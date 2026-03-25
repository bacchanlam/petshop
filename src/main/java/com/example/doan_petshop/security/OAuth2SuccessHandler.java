package com.example.doan_petshop.security;

import com.example.doan_petshop.entity.Cart;
import com.example.doan_petshop.entity.Role;
import com.example.doan_petshop.entity.User;
import com.example.doan_petshop.repository.CartRepository;
import com.example.doan_petshop.repository.RoleRepository;
import com.example.doan_petshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CartRepository cartRepository;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                       HttpServletResponse response,
                                       Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        
        try {
            String email = oAuth2User.getAttribute("email");
            String givenName = oAuth2User.getAttribute("given_name");
            String familyName = oAuth2User.getAttribute("family_name");
            String name = oAuth2User.getAttribute("name");
            String picture = oAuth2User.getAttribute("picture");

            // Tạo fullName từ given_name + family_name, hoặc dùng name
            String fullName = "";
            if (givenName != null && !givenName.isEmpty()) {
                fullName = givenName;
                if (familyName != null && !familyName.isEmpty()) {
                    fullName += " " + familyName;
                }
            } else if (name != null && !name.isEmpty()) {
                fullName = name;
            }

            log.info("OAuth2 login: email={}, fullName={}, picture={}", email, fullName, picture);

            // Tìm hoặc tạo user
            Optional<User> userOpt = userRepository.findByEmail(email);
            User user;

            if (userOpt.isEmpty()) {
                // Tạo user mới từ OAuth2 info
                user = createNewUser(email, fullName, picture);
                log.info("New user created via OAuth2: {}", email);
            } else {
                user = userOpt.get();
                log.info("User found with email: {}", email);
            }

            // Load CustomUserDetails từ database và update security context
            CustomUserDetails userDetails = (CustomUserDetails) customUserDetailsService.loadUserByUsername(user.getUsername());
            
            // Tạo Authentication mới với CustomUserDetails
            UsernamePasswordAuthenticationToken newAuth = 
                new UsernamePasswordAuthenticationToken(
                    userDetails, 
                    null, 
                    userDetails.getAuthorities()
                );
            newAuth.setDetails(authentication.getDetails());
            SecurityContextHolder.getContext().setAuthentication(newAuth);

            // Chuyển hướng sau khi đăng nhập thành công
            response.sendRedirect("/");
        } catch (Exception e) {
            log.error("Error in OAuth2SuccessHandler: {}", e.getMessage(), e);
            response.sendRedirect("/auth/login?error=oauth2");
        }
    }

    private User createNewUser(String email, String fullName, String picture) {
        // Lấy role USER mặc định
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy role USER"));

        // Tạo username từ email (lấy phần trước dấu @)
        String username = email.split("@")[0];
        int counter = 1;
        String originalUsername = username;
        
        // Nếu username đã tồn tại, thêm số vào
        while (userRepository.existsByUsername(username)) {
            username = originalUsername + counter;
            counter++;
        }

        // Tạo User mới
        User user = User.builder()
                .username(username)
                .email(email.toLowerCase())
                .password(null) // OAuth2 users không có password (login bằng Google)
                .fullName(fullName != null && !fullName.isEmpty() ? fullName : username)
                .avatar(picture)
                .enabled(true)
                .emailVerified(true) // Đã verify qua OAuth2 provider
                .build();
        
        user.addRole(userRole);
        User savedUser = userRepository.save(user);

        // Tạo giỏ hàng cho user mới
        Cart cart = Cart.builder().user(savedUser).build();
        cartRepository.save(cart);

        return savedUser;
    }
}
