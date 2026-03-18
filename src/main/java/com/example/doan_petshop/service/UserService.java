package com.example.doan_petshop.service;

import com.example.doan_petshop.dto.ChangePasswordDTO;
import com.example.doan_petshop.dto.UserRegisterDTO;
import com.example.doan_petshop.entity.Cart;
import com.example.doan_petshop.entity.Role;
import com.example.doan_petshop.entity.User;
import com.example.doan_petshop.repository.CartRepository;
import com.example.doan_petshop.repository.RoleRepository;
import com.example.doan_petshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;

    // ========================
    // ĐĂNG KÝ tài khoản mới
    // ========================
    @Transactional
    public User register(UserRegisterDTO dto) {
        // Kiểm tra username đã tồn tại
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập đã được sử dụng");
        }
        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }
        // Kiểm tra mật khẩu khớp
        if (!dto.isPasswordMatching()) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }

        // Lấy role USER mặc định
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy role USER"));

        // Tạo User mới
        User user = User.builder()
                .username(dto.getUsername().trim())
                .email(dto.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(dto.getPassword()))
                .fullName(dto.getFullName().trim())
                .phone(dto.getPhone())
                .enabled(true)
                .build();
        user.addRole(userRole);

        User savedUser = userRepository.save(user);

        // Tự động tạo giỏ hàng cho user mới
        Cart cart = Cart.builder().user(savedUser).build();
        cartRepository.save(cart);

        return savedUser;
    }

    // ========================
    // CẬP NHẬT thông tin cá nhân
    // ========================
    @Transactional
    public User updateProfile(Long userId, String fullName, String phone, String address) {
        User user = findById(userId);
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setAddress(address);
        return userRepository.save(user);
    }

    // ========================
    // ĐỔI MẬT KHẨU
    // ========================
    @Transactional
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        User user = findById(userId);

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
        }
        if (!dto.isPasswordMatching()) {
            throw new IllegalArgumentException("Mật khẩu mới xác nhận không khớp");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    // ========================
    // ADMIN - Khóa / Mở khóa tài khoản
    // ========================
    @Transactional
    public void toggleUserEnabled(Long userId) {
        User user = findById(userId);
        user.setEnabled(!user.getEnabled());
        userRepository.save(user);
    }

    // ========================
    // ADMIN - Phân quyền
    // ========================
    @Transactional
    public void changeRole(Long userId, String roleName) {
        User user = findById(userId);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy role: " + roleName));
        user.getRoles().clear();
        user.addRole(role);
        userRepository.save(user);
    }

    // ========================
    // HELPERS
    // ========================
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user id: " + id));
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
