package com.example.doan_petshop.security;

import com.example.doan_petshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailVerificationAuthenticationProvider implements AuthenticationProvider {

    private final UserDetailsService userDetailsService;
    private final ObjectProvider<PasswordEncoder> passwordEncoderProvider;
    private final UserRepository userRepository;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = (String) authentication.getCredentials();

        try {
            // Load user từ database
            CustomUserDetails userDetails = 
                (CustomUserDetails) userDetailsService.loadUserByUsername(username);

            // Kiểm tra mật khẩu - dùng ObjectProvider để lazy-load PasswordEncoder
            PasswordEncoder passwordEncoder = passwordEncoderProvider.getIfAvailable();
            if (passwordEncoder == null) {
                throw new BadCredentialsException("PasswordEncoder not configured");
            }
            
            if (!passwordEncoder.matches(password, userDetails.getPassword())) {
                throw new BadCredentialsException("Tên đăng nhập hoặc mật khẩu không đúng");
            }

            //  Kiểm tra email đã verify chưa
            if (!userDetails.getUser().getEmailVerified()) {
                throw new EmailNotVerifiedException(
                    "Email chưa được xác thực. Vui lòng kiểm tra email của bạn và click vào link xác nhận."
                );
            }

            // Nếu tất cả ok, return authenticated token
            return new UsernamePasswordAuthenticationToken(
                userDetails,
                password,
                userDetails.getAuthorities()
            );

        } catch (BadCredentialsException e) {
            throw e;
        } catch (EmailNotVerifiedException e) {
            //  Để EmailNotVerifiedException pass qua
            throw e;
        } catch (Exception e) {
            throw new BadCredentialsException("Tên đăng nhập hoặc mật khẩu không đúng");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
