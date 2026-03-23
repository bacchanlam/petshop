package com.example.doan_petshop.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    public enum MessageType {
        CHAT,       // tin nhắn thường
        JOIN,       // khách kết nối
        LEAVE,
        CLOSE,// khách rời đi
        TYPING      // đang gõ
    }

    private MessageType type;
    private String      content;
    private String      sender;       // tên người gửi
    private String      sessionId;    // ID phiên chat của khách
    private String      time;         // thời gian gửi (HH:mm)
    private boolean     fromAdmin;    // true = từ admin, false = từ khách

    // Tạo thời gian hiện tại tự động
    public static String nowTime() {
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}