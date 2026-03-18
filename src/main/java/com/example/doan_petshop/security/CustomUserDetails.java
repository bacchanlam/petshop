package com.example.doan_petshop.security;

import com.example.doan_petshop.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

@Getter
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    // Lấy danh sách quyền từ roles của User
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    // Tài khoản không hết hạn
    @Override
    public boolean isAccountNonExpired() { return true; }

    // Tài khoản không bị khóa (dựa vào enabled)
    @Override
    public boolean isAccountNonLocked() { return user.getEnabled(); }

    // Credentials không hết hạn
    @Override
    public boolean isCredentialsNonExpired() { return true; }

    // Tài khoản có được kích hoạt không
    @Override
    public boolean isEnabled() { return user.getEnabled(); }

    // Helper - lấy thông tin user tiện dùng trong Controller/Thymeleaf
    public Long getId()       { return user.getId(); }
    public String getFullName() { return user.getFullName(); }
    public String getEmail()  { return user.getEmail(); }
    public boolean isAdmin()  { return user.isAdmin(); }
}
