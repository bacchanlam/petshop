package com.example.doan_petshop.service;

import com.example.doan_petshop.dto.OrderRequestDTO;
import com.example.doan_petshop.entity.*;
import com.example.doan_petshop.enums.OrderStatus;
import com.example.doan_petshop.enums.PaymentMethod;
import com.example.doan_petshop.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository     orderRepository;
    private final CartRepository      cartRepository;
    private final CartItemRepository  cartItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository   productRepository;
    private final UserRepository      userRepository;
    private final CartService         cartService;

    // ========================
    // ĐẶT HÀNG từ giỏ hàng
    // ========================
    @Transactional
    public Order placeOrder(Long userId, OrderRequestDTO dto) {
        // Lấy giỏ hàng
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng trống"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Giỏ hàng đang trống, vui lòng thêm sản phẩm");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        // Tính tổng tiền sản phẩm & kiểm tra tồn kho
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem ci : cart.getItems()) {
            Product p = ci.getProduct();
            if (!p.getActive()) {
                throw new IllegalStateException("Sản phẩm \"" + p.getName() + "\" không còn kinh doanh");
            }
            if (p.getStock() < ci.getQuantity()) {
                throw new IllegalStateException(
                        "Sản phẩm \"" + p.getName() + "\" chỉ còn " + p.getStock() + " trong kho"
                );
            }
            total = total.add(p.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
        }

        // Tính phí ship: miễn phí nếu tổng sản phẩm >= 300,000
        BigDecimal shippingFee = total.compareTo(new BigDecimal("300000")) >= 0
                ? BigDecimal.ZERO
                : new BigDecimal("30000");
        BigDecimal totalWithShipping = total.add(shippingFee);

        // Tạo Order
        Order order = Order.builder()
                .user(user)
                .fullName(dto.getFullName())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .note(dto.getNote())
                .paymentMethod(dto.getPaymentMethod())
                .status(OrderStatus.PENDING)
                .totalAmount(totalWithShipping)
                .build();

        // Tạo OrderItem từ CartItem
        for (CartItem ci : cart.getItems()) {
            Product p = ci.getProduct();
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(p)
                    .productName(p.getName())
                    .productImg(p.getThumbnail())
                    .quantity(ci.getQuantity())
                    .unitPrice(p.getPrice())
                    .build();
            order.getOrderItems().add(item);

            // Trừ tồn kho
            p.setStock(p.getStock() - ci.getQuantity());
            productRepository.save(p);
        }

        Order saved = orderRepository.save(order);

        cartService.clearCart(userId);
        cart.getItems().clear();

        return saved;
    }

    // ========================
    // Lịch sử đơn hàng của user
    // ========================
    public List<Order> getOrderHistory(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // ========================
    // Chi tiết đơn hàng (kiểm tra quyền)
    // ========================
    public Order getOrderDetail(Long orderId, Long userId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        if (!isAdmin && !order.getUser().getId().equals(userId)) {
            throw new SecurityException("Bạn không có quyền xem đơn hàng này");
        }
        return order;
    }

    // ========================
    // USER - Hủy đơn hàng (chỉ được hủy khi PENDING)
    // ========================
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Order order = getOrderDetail(orderId, userId, false);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể hủy đơn hàng đang chờ xác nhận");
        }
        order.setStatus(OrderStatus.CANCELLED);

        // Hoàn lại tồn kho
        for (OrderItem item : order.getOrderItems()) {
            if (item.getProduct() != null) {
                Product p = item.getProduct();
                p.setStock(p.getStock() + item.getQuantity());
                productRepository.save(p);
            }
        }
        orderRepository.save(order);
    }

    // ========================
    // ADMIN - Cập nhật trạng thái đơn
    // ========================
    @Transactional
    public void updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng id: " + orderId));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Không thể cập nhật đơn hàng đã hủy");
        }
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Đơn hàng đã hoàn thành");
        }

        // Nếu admin hủy → hoàn lại tồn kho
        if (newStatus == OrderStatus.CANCELLED) {
            for (OrderItem item : order.getOrderItems()) {
                if (item.getProduct() != null) {
                    Product p = item.getProduct();
                    p.setStock(p.getStock() + item.getQuantity());
                    productRepository.save(p);
                }
            }
        }

        order.setStatus(newStatus);
        orderRepository.save(order);
    }

    // ========================
    // ADMIN - Danh sách đơn có filter + phân trang
    // ========================
    public Page<Order> findByAdminFilters(OrderStatus status, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        return orderRepository.findByAdminFilters(status, kw, pageable);
    }

    // ========================
    // Kiểm tra user đã mua sản phẩm chưa
    // ========================
    public boolean hasUserPurchased(Long userId, Long productId) {
        return orderRepository.hasUserPurchasedProduct(userId, productId);
    }

    // ========================
    // Dashboard stats
    // ========================
    public long countByStatus(OrderStatus status) {
        return orderRepository.countByStatus(status);
    }

    public BigDecimal revenueThisMonth() {
        LocalDateTime start = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);
        LocalDateTime end   = LocalDateTime.now();
        return orderRepository.sumRevenueByDateRange(start, end);
    }

    public long countToday() {
        LocalDateTime start = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime end   = LocalDateTime.now();
        return orderRepository.countByDateRange(start, end);
    }

    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng id: " + id));
    }

    public int countItems(Long orderId) {
        return orderItemRepository.countByOrderId(orderId);
    }

    @Transactional
    public void confirmPayment(Long orderId, PaymentMethod paymentMethod) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng id: " + orderId));

        // Chỉ xác nhận khi đơn đang PENDING
        if (order.getStatus() != OrderStatus.PENDING) {
            log.info("Order #{} already in status {}, skip confirm", orderId, order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentMethod(paymentMethod);
        orderRepository.save(order);
        log.info("Order #{} confirmed with payment method {}", orderId, paymentMethod);
    }

    // ========================
    // Hủy đơn hàng (khi thanh toán thất bại)
    // Không check userId - dùng nội bộ
    // ========================
    @Transactional
    public void cancelOrderByAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng id: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) return;

        order.setStatus(OrderStatus.CANCELLED);

        // Hoàn lại tồn kho
        for (OrderItem item : order.getOrderItems()) {
            if (item.getProduct() != null) {
                Product p = item.getProduct();
                p.setStock(p.getStock() + item.getQuantity());
                productRepository.save(p);
            }
        }
        orderRepository.save(order);
        log.info("Order #{} cancelled by system (payment failed)", orderId);
    }
}