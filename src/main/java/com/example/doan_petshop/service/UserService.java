package com.example.doan_petshop.service;

import com.example.doan_petshop.dto.ChangePasswordDTO;
import com.example.doan_petshop.dto.UserRegisterDTO;
import com.example.doan_petshop.entity.Cart;
import com.example.doan_petshop.entity.PasswordResetToken;
import com.example.doan_petshop.entity.Role;
import com.example.doan_petshop.entity.User;
import com.example.doan_petshop.entity.VerificationToken;
import com.example.doan_petshop.repository.CartRepository;
import com.example.doan_petshop.repository.PasswordResetTokenRepository;
import com.example.doan_petshop.repository.RoleRepository;
import com.example.doan_petshop.repository.UserRepository;
import com.example.doan_petshop.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CartRepository cartRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

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

        // Tạo User mới - Chưa verify email
        User user = User.builder()
                .username(dto.getUsername().trim())
                .email(dto.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(dto.getPassword()))
                .fullName(dto.getFullName().trim())
                .phone(dto.getPhone())
                .enabled(true)
                .emailVerified(false) // Email chưa được verify
                .build();
        user.addRole(userRole);

        User savedUser = userRepository.save(user);

        // Tự động tạo giỏ hàng cho user mới
        Cart cart = Cart.builder().user(savedUser).build();
        cartRepository.save(cart);

        return savedUser;
    }

    // ========================
    // GỬI email xác nhận
    // ========================
    @Transactional
    public void sendVerificationEmail(Long userId, String appUrl) {
        User user = findById(userId);
        
        // Xóa token cũ nếu có
        verificationTokenRepository.deleteByUserId(userId);
        
        // Tạo token mới
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(24);
        
        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .user(user)
                .expiresAt(expiryDate)
                .build();
        
        verificationTokenRepository.save(verificationToken);
        
        // Tạo URL xác nhận
        String verificationUrl = appUrl + "/auth/verify-email?token=" + token;
        
        // Gửi email
        emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), verificationUrl);
    }

    // ========================
    // XÁC NHẬN email qua token
    // ========================
    @Transactional
    public boolean verifyEmail(String token) {
        Optional<VerificationToken> verificationTokenOpt = verificationTokenRepository.findByToken(token);
        
        if (verificationTokenOpt.isEmpty()) {
            throw new IllegalArgumentException("Token không tồn tại");
        }
        
        VerificationToken verificationToken = verificationTokenOpt.get();
        
        // Kiểm tra token hợp lệ
        if (!verificationToken.isValid()) {
            throw new IllegalArgumentException("Token không hợp lệ hoặc đã hết hạn");
        }
        
        // Cập nhật user
        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        
        // Đánh dấu token đã verify
        verificationToken.verify();
        verificationTokenRepository.save(verificationToken);
        
        // Gửi email chào mừng
        emailService.sendWelcomeEmail(user.getEmail(), user.getUsername());
        
        return true;
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
    // ĐỔI MẬT KHẨU / ĐẶT MẬT KHẨU LẦN ĐẦU
    // ========================
    @Transactional
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        User user = findById(userId);

        // Nếu user có mật khẩu (đăng ký bình thường), kiểm tra mật khẩu cũ
        // Nếu user không có mật khẩu (đăng nhập Google), bỏ qua kiểm tra
        if (user.getPassword() != null) {
            // Case: User thường - phải verify mật khẩu cũ
            if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
                throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
            }
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

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
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

    // ========================
    // XÓA USER (khi email fail)
    // ========================
    @Transactional
    public void deleteUser(Long userId) {
        // Xóa verification token trước
        verificationTokenRepository.deleteByUserId(userId);
        // Sau đó xóa user
        userRepository.deleteById(userId);
    }

    // ========================
    // QUÊN MẬT KHẨU - Gửi email reset password
    // ========================
    @Transactional
    public void sendPasswordResetEmail(String email, String appUrl) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại"));

        // Xóa token cũ nếu có
        passwordResetTokenRepository.deleteByUserId(user.getId());

        // Tạo token mới
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(24);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(expiryDate)
                .build();

        passwordResetTokenRepository.save(resetToken);

        // Tạo URL reset password
        String resetUrl = appUrl + "/auth/reset-password?token=" + token;

        // Gửi email
        emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), resetUrl);
    }

    // ========================
    // QUÊN MẬT KHẨU - Xác nhận reset password
    // ========================
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token không tồn tại"));

        // Kiểm tra token hợp lệ
        if (!resetToken.isValid()) {
            throw new IllegalArgumentException("Token không hợp lệ hoặc đã hết hạn");
        }

        // Cập nhật mật khẩu
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Đánh dấu token đã dùng
        resetToken.markAsUsed();
        passwordResetTokenRepository.save(resetToken);

        // Gửi email xác nhận
        emailService.sendPasswordResetConfirmation(user.getEmail(), user.getUsername());
    }
}
