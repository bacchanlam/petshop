package com.example.doan_petshop.service;

import com.example.doan_petshop.entity.*;
import com.example.doan_petshop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository     cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository  productRepository;
    private final UserRepository     userRepository;

    // ========================
    // Lấy giỏ hàng theo userId
    // Nếu chưa có → tự tạo mới
    // ========================
    @Transactional
    public Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
            Cart cart = Cart.builder().user(user).build();
            return cartRepository.save(cart);
        });
    }

    // ========================
    // THÊM sản phẩm vào giỏ
    // Nếu đã có → cộng thêm số lượng
    // ========================
    @Transactional
    public void addToCart(Long userId, Long productId, int quantity) {
        // Validate product
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));
        if (!product.getActive()) {
            throw new IllegalStateException("Sản phẩm không còn kinh doanh");
        }
        if (product.getStock() < quantity) {
            throw new IllegalStateException("Sản phẩm chỉ còn " + product.getStock() + " trong kho");
        }

        Cart cart = getOrCreateCart(userId);

        // Kiểm tra sản phẩm đã trong giỏ chưa
        Optional<CartItem> existing = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), productId);

        if (existing.isPresent()) {
            // Cộng thêm số lượng
            CartItem item = existing.get();
            int newQty = item.getQuantity() + quantity;
            if (newQty > product.getStock()) {
                throw new IllegalStateException("Vượt quá số lượng tồn kho");
            }
            item.setQuantity(newQty);
            cartItemRepository.save(item);
        } else {
            // Thêm mới
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .build();
            cartItemRepository.save(item);
        }
    }

    // ========================
    // CẬP NHẬT số lượng
    // ========================
    @Transactional
    public void updateQuantity(Long userId, Long itemId, int quantity) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy item"));

        // Bảo mật: chỉ được sửa item của chính mình
        if (!item.getCart().getUser().getId().equals(userId)) {
            throw new SecurityException("Không có quyền thực hiện");
        }

        if (quantity <= 0) {
            cartItemRepository.deleteById(itemId);
            return;
        }

        Product product = item.getProduct();
        if (quantity > product.getStock()) {
            throw new IllegalStateException("Chỉ còn " + product.getStock() + " sản phẩm trong kho");
        }

        item.setQuantity(quantity);
        cartItemRepository.save(item);
    }

    // ========================
    // XÓA một sản phẩm khỏi giỏ
    // ========================
    @Transactional
    public void removeItem(Long userId, Long itemId) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy item"));

        if (!item.getCart().getUser().getId().equals(userId)) {
            throw new SecurityException("Không có quyền thực hiện");
        }

        cartItemRepository.deleteById(itemId);
    }

    // ========================
    // XÓA toàn bộ giỏ hàng
    // ========================
    @Transactional
    public void clearCart(Long userId) {
        cartRepository.findByUserId(userId)
                .ifPresent(cart -> cartItemRepository.deleteByCartId(cart.getId()));
    }

    // ========================
    // Đếm số lượng sản phẩm trong giỏ (hiển thị badge navbar)
    // ========================
    public int countItems(Long userId) {
        return cartRepository.findByUserId(userId)
                .map(cart -> cart.getItems().stream()
                        .mapToInt(CartItem::getQuantity).sum())
                .orElse(0);
    }

    // ========================
    // Lấy giỏ hàng đầy đủ
    // ========================
    @Transactional(readOnly = true)
    public Cart getCart(Long userId) {
        return getOrCreateCart(userId);
    }
}