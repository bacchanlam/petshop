package com.example.doan_petshop.security;

import com.example.doan_petshop.entity.User;
import com.example.doan_petshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // Spring Security gọi method này khi login
    // username ở đây có thể là username HOẶC email
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Không tìm thấy tài khoản: " + username)
                );
        return new CustomUserDetails(user);
    }
}
