package com.example.doan_petshop.controller;

import com.example.doan_petshop.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.*;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;

    // Lưu toàn bộ dữ liệu chat trên server
    // Key: sessionId → thông tin phiên
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> chatSessionToWsSessions = new ConcurrentHashMap<>();
    private final Map<String, String> wsSessionToChatSession = new ConcurrentHashMap<>();

    // Grace period: chờ 10 giây trước khi broadcast LEAVE,
    // để user chuyển trang kịp reconnect mà không bị đánh dấu offline
    private final ScheduledExecutorService leaveScheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, ScheduledFuture<?>> pendingLeaves = new ConcurrentHashMap<>();

    // Inner class lưu thông tin 1 phiên chat
    static class SessionInfo {
        String sessionId;
        String customerName;
        boolean online = true;
        List<ChatMessage> messages = new CopyOnWriteArrayList<>();

        SessionInfo(String sessionId, String customerName) {
            this.sessionId    = sessionId;
            this.customerName = customerName;
        }
    }

    // GET /admin/chat
    @GetMapping("/admin/chat")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminChat(Model model) {
        model.addAttribute("sessions", sessions.values());
        return "admin/chat";
    }

    // REST API: Admin lấy lịch sử tin nhắn của 1 session
    // GET /admin/chat/history/{sessionId}
    @GetMapping("/admin/chat/history/{sessionId}")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public List<ChatMessage> getHistory(@PathVariable String sessionId) {
        SessionInfo s = sessions.get(sessionId);
        return s != null ? s.messages : Collections.emptyList();
    }

    // REST API: Admin lấy danh sách sessions hiện tại
    // GET /admin/chat/sessions
    @GetMapping("/admin/chat/sessions")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> getSessions() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (SessionInfo s : sessions.values()) {
            // Bỏ qua session chưa có tin nhắn CHAT thực sự từ khách
            boolean hasCustomerChat = s.messages.stream().anyMatch(msg ->
                    msg != null && !msg.isFromAdmin() &&
                    (msg.getType() == ChatMessage.MessageType.CHAT || msg.getType() == null));
            if (!hasCustomerChat) continue;

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("sessionId",    s.sessionId);
            m.put("customerName", s.customerName);
            m.put("online",       s.online);
            m.put("lastMessage",  s.messages.isEmpty() ? null
                    : s.messages.get(s.messages.size() - 1));

            // unread = số tin nhắn từ khách kể từ sau tin nhắn CHAT gần nhất của admin
            // (JOIN/LEAVE/TYPING/CLOSE không tính unread)
            int unreadCount = 0;
            for (int i = s.messages.size() - 1; i >= 0; i--) {
                ChatMessage msg = s.messages.get(i);
                if (msg == null) continue;
                boolean isCustomerChat =
                        !msg.isFromAdmin() &&
                        (msg.getType() == ChatMessage.MessageType.CHAT || msg.getType() == null);

                boolean isAdminChat =
                        msg.isFromAdmin() &&
                        (msg.getType() == ChatMessage.MessageType.CHAT || msg.getType() == null);

                if (isAdminChat) break;
                if (isCustomerChat) unreadCount++;
            }

            m.put("unread", unreadCount);
            result.add(m);
        }
        result.sort((a, b) -> Boolean.compare(
                !(Boolean) b.get("online"), !(Boolean) a.get("online")));
        return result;
    }

    // Khách gửi tin nhắn
    // /app/chat.send
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage message) {
        message.setTime(ChatMessage.nowTime());
        message.setFromAdmin(false);

        // Tạo session nếu chưa có
        sessions.computeIfAbsent(message.getSessionId(),
                sid -> new SessionInfo(sid, message.getSender()));

        // Lưu tin nhắn vào server
        sessions.get(message.getSessionId()).messages.add(message);

        // Gửi đến admin
        messagingTemplate.convertAndSend("/topic/admin", message);
        // Gửi echo lại cho khách
        messagingTemplate.convertAndSend(
                "/topic/chat." + message.getSessionId(), message);
    }

    // Admin reply
    // /app/admin.reply
    @MessageMapping("/admin.reply")
    public void adminReply(@Payload ChatMessage message) {
        message.setTime(ChatMessage.nowTime());
        message.setFromAdmin(true);
        message.setSender("PetShop");

        // Lưu tin nhắn của admin vào session
        SessionInfo s = sessions.get(message.getSessionId());
        if (s != null) s.messages.add(message);

        // Gửi đến khách
        messagingTemplate.convertAndSend(
                "/topic/chat." + message.getSessionId(), message);
        // Gửi bản copy cho admin panel
        messagingTemplate.convertAndSend("/topic/admin", message);
    }

    // Khách kết nối
    // /app/chat.join
    @MessageMapping("/chat.join")
    public void join(
            @Payload ChatMessage message,
            @Header("simpSessionId") String wsSessionId
    ) {
        message.setType(ChatMessage.MessageType.JOIN);
        message.setTime(ChatMessage.nowTime());
        message.setFromAdmin(false);

        // Nếu đã có session cùng customerName → dùng lại SID cũ
        String existingSid = sessions.values().stream()
                .filter(s -> s.customerName.equals(message.getSender()))
                .map(s -> s.sessionId)
                .findFirst()
                .orElse(null);

        if (existingSid != null && !existingSid.equals(message.getSessionId())) {
            sessions.remove(message.getSessionId());
            SessionInfo s = sessions.get(existingSid);
            s.online = true;
            message.setSessionId(existingSid);
        } else {
            SessionInfo s = sessions.computeIfAbsent(
                    message.getSessionId(),
                    sid -> new SessionInfo(sid, message.getSender()));
            s.online       = true;
            s.customerName = message.getSender();
        }

        // Hủy LEAVE đang chờ (user chuyển trang rồi reconnect → không cần offline)
        ScheduledFuture<?> pending = pendingLeaves.remove(message.getSessionId());
        if (pending != null) pending.cancel(false);

        messagingTemplate.convertAndSend("/topic/admin", message);
        messagingTemplate.convertAndSend(
                "/topic/chat." + message.getSessionId(), message);
        if (wsSessionId != null) {
            wsSessionToChatSession.put(wsSessionId, message.getSessionId());
            chatSessionToWsSessions
                    .computeIfAbsent(message.getSessionId(), k -> ConcurrentHashMap.newKeySet())
                    .add(wsSessionId);
        }
    }
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String wsSessionId = sha.getSessionId();
        if (wsSessionId == null) return;
        String chatSessionId = wsSessionToChatSession.remove(wsSessionId);
        if (chatSessionId == null) return;
        Set<String> wsSet = chatSessionToWsSessions.get(chatSessionId);
        if (wsSet != null) {
            wsSet.remove(wsSessionId);
            if (wsSet.isEmpty()) {
                chatSessionToWsSessions.remove(chatSessionId);
                SessionInfo s = sessions.get(chatSessionId);
                if (s == null) return;
                s.online = false;
                // Delay 10 giây: nếu user chỉ chuyển trang thì sẽ reconnect kịp
                // và JOIN sẽ hủy task này → admin không thấy offline/LEAVE
                final String sid = chatSessionId;
                ScheduledFuture<?> task = leaveScheduler.schedule(() -> {
                    SessionInfo si = sessions.get(sid);
                    if (si != null && !si.online) {
                        ChatMessage leaveMsg = ChatMessage.builder()
                                .type(ChatMessage.MessageType.LEAVE)
                                .sessionId(sid)
                                .sender(si.customerName)
                                .fromAdmin(false)
                                .time(ChatMessage.nowTime())
                                .content(null)
                                .build();
                        messagingTemplate.convertAndSend("/topic/admin", leaveMsg);
                    }
                    pendingLeaves.remove(sid);
                }, 10, TimeUnit.SECONDS);
                pendingLeaves.put(chatSessionId, task);
            }
        }
    }
    

    // Khách rời đi
    // /app/chat.leave
    @MessageMapping("/chat.leave")
    public void leave(@Payload ChatMessage message) {
        message.setTime(ChatMessage.nowTime());

        // Xử lý CLOSE: user chủ động đóng cuộc trò chuyện
        if ("CLOSE".equals(message.getType() != null ? message.getType().name() : "")) {
            sessions.remove(message.getSessionId());
            messagingTemplate.convertAndSend("/topic/admin", message);
            return;
        }

        // LEAVE bình thường: đánh dấu offline, giữ lịch sử
        message.setType(ChatMessage.MessageType.LEAVE);
        SessionInfo s = sessions.get(message.getSessionId());
        if (s != null) s.online = false;

        messagingTemplate.convertAndSend("/topic/admin", message);
    }

    // Typing indicator
    // /app/chat.typing
    @MessageMapping("/chat.typing")
    public void typing(@Payload ChatMessage message) {
        message.setType(ChatMessage.MessageType.TYPING);
        if (message.isFromAdmin()) {
            messagingTemplate.convertAndSend(
                    "/topic/chat." + message.getSessionId(), message);
        } else {
            messagingTemplate.convertAndSend("/topic/admin", message);
        }
    }
    @GetMapping("/chat/history/{sessionId}")
    @ResponseBody
    public List<ChatMessage> getUserHistory(@PathVariable String sessionId) {
        SessionInfo s = sessions.get(sessionId);
        return s != null ? s.messages : Collections.emptyList();
    }

    // Xóa session khi user logout (gọi qua REST, đảm bảo hoạt động dù WebSocket ngắt)
    // POST /chat/close
    @PostMapping("/chat/close")
    @ResponseBody
    public ResponseEntity<Void> closeSessionRest(@RequestParam String sessionId) {
        // Hủy pending leave nếu có
        ScheduledFuture<?> pending = pendingLeaves.remove(sessionId);
        if (pending != null) pending.cancel(false);

        SessionInfo s = sessions.remove(sessionId);
        if (s != null) {
            ChatMessage closeMsg = ChatMessage.builder()
                    .type(ChatMessage.MessageType.CLOSE)
                    .sessionId(sessionId)
                    .sender(s.customerName)
                    .fromAdmin(false)
                    .time(ChatMessage.nowTime())
                    .content(null)
                    .build();
            messagingTemplate.convertAndSend("/topic/admin", closeMsg);
        }
        return ResponseEntity.ok().build();
    }

}