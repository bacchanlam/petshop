package com.example.doan_petshop.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailFrom;

    public void sendVerificationEmail(String toEmail, String username, String verificationUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Xác nhận Email - PetShop");
            helper.setFrom(mailFrom);

            // HTML content
            String htmlContent = String.format(
                    "<html>" +
                    "<body style='font-family: Arial, sans-serif; color: #333;'>" +
                    "  <div style='max-width: 600px; margin: 0 auto;'>" +
                    "    <h2 style='color: #28a745;'>Xác nhận Email PetShop</h2>" +
                    "    <p>Xin chào <strong>%s</strong>,</p>" +
                    "    <p>Cảm ơn bạn đã đăng ký tài khoản tại PetShop. Để hoàn tất quá trình đăng ký, " +
                    "vui lòng nhấn vào nút bên dưới để xác nhận email của bạn:</p>" +
                    "    <p style='text-align: center; margin: 30px 0;'>" +
                    "      <a href='%s' " +
                    "         style='display: inline-block; background-color: #28a745; color: white; " +
                    "                padding: 12px 30px; text-decoration: none; border-radius: 5px; " +
                    "                font-weight: bold;'>Xác nhận Email</a>" +
                    "    </p>" +
                    "    <p>Hoặc sao chép và dán link này vào trình duyệt:</p>" +
                    "    <p style='word-break: break-all; background-color: #f5f5f5; padding: 10px; border-radius: 5px;'>" +
                    "      %s" +
                    "    </p>" +
                    "    <p style='color: #666; font-size: 12px;'>Link này sẽ hết hạn sau 24 giờ.</p>" +
                    "    <hr style='border: none; border-top: 1px solid #ddd; margin: 20px 0;'>" +
                    "    <p style='color: #666; font-size: 12px;'>" +
                    "      Nếu bạn không yêu cầu đăng ký tài khoản này, vui lòng bỏ qua email này." +
                    "    </p>" +
                    "    <p style='color: #666; font-size: 12px;'>" +
                    "      © 2026 PetShop. All rights reserved." +
                    "    </p>" +
                    "  </div>" +
                    "</body>" +
                    "</html>",
                    username,
                    verificationUrl,
                    verificationUrl
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);

            log.info("Verification email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
            throw new RuntimeException("Không thể gửi email xác nhận", e);
        }
    }

    /**
     * Gửi email thông báo tài khoản được kích hoạt
     */
    public void sendWelcomeEmail(String toEmail, String username) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Chào mừng đến PetShop - Tài khoản đã được kích hoạt");
            helper.setFrom(mailFrom);

            String htmlContent = String.format(
                    "<html>" +
                    "<body style='font-family: Arial, sans-serif; color: #333;'>" +
                    "  <div style='max-width: 600px; margin: 0 auto;'>" +
                    "    <h2 style='color: #28a745;'>Chào mừng đến PetShop!</h2>" +
                    "    <p>Xin chào <strong>%s</strong>,</p>" +
                    "    <p>Tài khoản của bạn đã được kích hoạt thành công! 🎉</p>" +
                    "    <p>Bây giờ bạn có thể:</p>" +
                    "    <ul>" +
                    "      <li>Mua sắm lựa chọn đồ cho thú cưng của bạn</li>" +
                    "      <li>Thêm sản phẩm vào giỏ hàng</li>" +
                    "      <li>Đặt hàng và thanh toán</li>" +
                    "      <li>Xem lịch sử đơn hàng</li>" +
                    "      <li>Đánh giá sản phẩm</li>" +
                    "    </ul>" +
                    "    <p style='margin-top: 30px;'>" +
                    "      <a href='http://localhost:8080/' " +
                    "         style='display: inline-block; background-color: #28a745; color: white; " +
                    "                padding: 12px 30px; text-decoration: none; border-radius: 5px; " +
                    "                font-weight: bold;'>Bắt đầu mua sắm</a>" +
                    "    </p>" +
                    "    <hr style='border: none; border-top: 1px solid #ddd; margin: 20px 0;'>" +
                    "    <p style='color: #666; font-size: 12px;'>" +
                    "      © 2026 PetShop. All rights reserved." +
                    "    </p>" +
                    "  </div>" +
                    "</body>" +
                    "</html>",
                    username
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);

            log.info("Welcome email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send welcome email to: {}", toEmail, e);
        }
    }

    /**
     * Gửi email reset password (quên mật khẩu)
     */
    public void sendPasswordResetEmail(String toEmail, String username, String resetUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Đặt lại mật khẩu - PetShop");
            helper.setFrom(mailFrom);

            String htmlContent = String.format(
                    "<html>" +
                    "<body style='font-family: Arial, sans-serif; color: #333;'>" +
                    "  <div style='max-width: 600px; margin: 0 auto;'>" +
                    "    <h2 style='color: #0d6efd;'>Đặt lại mật khẩu PetShop</h2>" +
                    "    <p>Xin chào <strong>%s</strong>,</p>" +
                    "    <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn. " +
                    "Nếu bạn không yêu cầu này, vui lòng bỏ qua email này.</p>" +
                    "    <p>Để đặt lại mật khẩu, vui lòng nhấn vào nút bên dưới:</p>" +
                    "    <p style='text-align: center; margin: 30px 0;'>" +
                    "      <a href='%s' " +
                    "         style='display: inline-block; background-color: #0d6efd; color: white; " +
                    "                padding: 12px 30px; text-decoration: none; border-radius: 5px; " +
                    "                font-weight: bold;'>Đặt lại mật khẩu</a>" +
                    "    </p>" +
                    "    <p>Hoặc sao chép và dán link này vào trình duyệt:</p>" +
                    "    <p style='word-break: break-all; background-color: #f5f5f5; padding: 10px; border-radius: 5px;'>" +
                    "      %s" +
                    "    </p>" +
                    "    <p style='color: #666; font-size: 12px;'>Link này sẽ hết hạn sau 24 giờ.</p>" +
                    "    <hr style='border: none; border-top: 1px solid #ddd; margin: 20px 0;'>" +
                    "    <p style='color: #666; font-size: 12px;'>" +
                    "      © 2026 PetShop. All rights reserved." +
                    "    </p>" +
                    "  </div>" +
                    "</body>" +
                    "</html>",
                    username,
                    resetUrl,
                    resetUrl
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);

            log.info("Password reset email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Không thể gửi email đặt lại mật khẩu", e);
        }
    }

    /**
     * Gửi email xác nhận mật khẩu đã được đặt lại
     */
    public void sendPasswordResetConfirmation(String toEmail, String username) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Mật khẩu đã được đặt lại thành công - PetShop");
            helper.setFrom(mailFrom);

            String htmlContent = String.format(
                    "<html>" +
                    "<body style='font-family: Arial, sans-serif; color: #333;'>" +
                    "  <div style='max-width: 600px; margin: 0 auto;'>" +
                    "    <h2 style='color: #28a745;'>Mật khẩu đã được đặt lại</h2>" +
                    "    <p>Xin chào <strong>%s</strong>,</p>" +
                    "    <p>Mật khẩu của bạn đã được đặt lại thành công! </p>" +
                    "    <p>Bạn có thể dùng mật khẩu mới để đăng nhập vào tài khoản của mình.</p>" +
                    "    <p style='margin-top: 30px;'>" +
                    "      <a href='http://localhost:8080/auth/login' " +
                    "         style='display: inline-block; background-color: #28a745; color: white; " +
                    "                padding: 12px 30px; text-decoration: none; border-radius: 5px; " +
                    "                font-weight: bold;'>Đăng nhập</a>" +
                    "    </p>" +
                    "    <hr style='border: none; border-top: 1px solid #ddd; margin: 20px 0;'>" +
                    "    <p style='color: #666; font-size: 12px;'>" +
                    "      Nếu bạn không thực hiện thay đổi này, vui lòng liên hệ với chúng tôi ngay lập tức." +
                    "    </p>" +
                    "    <p style='color: #666; font-size: 12px;'>" +
                    "      © 2026 PetShop. All rights reserved." +
                    "    </p>" +
                    "  </div>" +
                    "</body>" +
                    "</html>",
                    username
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);

            log.info("Password reset confirmation email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send password reset confirmation email to: {}", toEmail, e);
        }
    }
}
