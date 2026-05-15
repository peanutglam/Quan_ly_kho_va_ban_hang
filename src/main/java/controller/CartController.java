package controller;

import dto.CartItemDTO;
import entity.Order;
import entity.Product;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import service.OrderService;
import service.ProductService;
import service.ShopProfileService;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.*;

/**
 * Nhiệm vụ 2: Giỏ hàng và checkout công khai.
 * Cart lưu trong session (Map<productId, CartItemDTO>).
 */
@Controller
public class CartController {

    private static final String CART_SESSION_KEY = "SHOP_CART";

    private final ProductService productService;
    private final OrderService orderService;
    private final ShopProfileService shopProfileService;

    public CartController(ProductService productService, OrderService orderService,
                          ShopProfileService shopProfileService) {
        this.productService = productService;
        this.orderService = orderService;
        this.shopProfileService = shopProfileService;
    }

    // ======================== CART ========================

    @GetMapping("/cart")
    public String viewCart(HttpSession session, Model model) {
        Map<Long, CartItemDTO> cart = getCart(session);
        BigDecimal total = cart.values().stream()
                .map(CartItemDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("cartItems", new ArrayList<>(cart.values()));
        model.addAttribute("cartTotal", total);
        model.addAttribute("shopProfile", shopProfileService.getPublicProfile());
        return "cart/index";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam(defaultValue = "1") int quantity,
                            HttpSession session,
                            RedirectAttributes redirectAttrs) {
        try {
            List<Product> publicProducts = productService.getPublicProducts(null);
            Product product = publicProducts.stream()
                    .filter(p -> productId.equals(p.getId())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));

            if (product.getQuantity() <= 0) {
                redirectAttrs.addFlashAttribute("errorMessage", "Sản phẩm đã hết hàng");
                return "redirect:/shop";
            }

            Map<Long, CartItemDTO> cart = getCart(session);
            CartItemDTO existing = cart.get(productId);
            int newQty = (existing == null ? 0 : existing.getQuantity()) + Math.max(1, quantity);
            int maxQty = product.getQuantity();
            if (newQty > maxQty) {
                newQty = maxQty;
                redirectAttrs.addFlashAttribute("warnMessage", "Chỉ còn " + maxQty + " sản phẩm trong kho");
            }

            cart.put(productId, new CartItemDTO(productId, product.getCode(), product.getName(),
                    product.getSalePrice(), newQty, maxQty));
            session.setAttribute(CART_SESSION_KEY, cart);
            redirectAttrs.addFlashAttribute("successMessage", "Đã thêm vào giỏ hàng");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/update")
    public String updateCart(@RequestParam Long productId,
                             @RequestParam int quantity,
                             HttpSession session) {
        Map<Long, CartItemDTO> cart = getCart(session);
        if (quantity <= 0) {
            cart.remove(productId);
        } else {
            CartItemDTO item = cart.get(productId);
            if (item != null) {
                int maxQty = item.getMaxStock();
                item.setQuantity(Math.min(quantity, maxQty));
            }
        }
        session.setAttribute(CART_SESSION_KEY, cart);
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam Long productId, HttpSession session) {
        Map<Long, CartItemDTO> cart = getCart(session);
        cart.remove(productId);
        session.setAttribute(CART_SESSION_KEY, cart);
        return "redirect:/cart";
    }

    @PostMapping("/cart/clear")
    public String clearCart(HttpSession session) {
        session.removeAttribute(CART_SESSION_KEY);
        return "redirect:/cart";
    }

    // ======================== CHECKOUT ========================

    @GetMapping("/checkout")
    public String checkoutForm(HttpSession session, Model model) {
        Map<Long, CartItemDTO> cart = getCart(session);
        if (cart.isEmpty()) return "redirect:/shop";

        BigDecimal total = cart.values().stream()
                .map(CartItemDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("cartItems", new ArrayList<>(cart.values()));
        model.addAttribute("cartTotal", total);
        model.addAttribute("shopProfile", shopProfileService.getPublicProfile());
        return "cart/checkout";
    }
    @PostMapping("/checkout")
    public String placeOrder(@RequestParam String customerName,
                             @RequestParam String customerPhone,
                             @RequestParam(required = false) String customerAddress,
                             @RequestParam(required = false) String note,
                             HttpSession session,
                             RedirectAttributes redirectAttrs) {
        Map<Long, CartItemDTO> cart = getCart(session);
        if (cart.isEmpty()) return "redirect:/shop";

        try {
            Map<Long, Integer> items = new LinkedHashMap<>();
            for (CartItemDTO item : cart.values()) {
                items.put(item.getProductId(), item.getQuantity());
            }
            Order order = orderService.createPublicOrder(customerName, customerPhone,
                    customerAddress == null ? "" : customerAddress, note, items);

            session.removeAttribute(CART_SESSION_KEY);
            return "redirect:/order-success?code=" + order.getOrderCode();
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/checkout";
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", "Đặt hàng thất bại: " + e.getMessage());
            return "redirect:/checkout";
        }
    }

    @GetMapping("/order-success")
    public String orderSuccess(@RequestParam String code, Model model) {
        model.addAttribute("orderCode", code);
        model.addAttribute("shopProfile", shopProfileService.getPublicProfile());
        return "cart/success";
    }

    @SuppressWarnings("unchecked")
    private Map<Long, CartItemDTO> getCart(HttpSession session) {
        Object cart = session.getAttribute(CART_SESSION_KEY);
        if (cart instanceof Map<?, ?> m) {
            return (Map<Long, CartItemDTO>) m;
        }
        Map<Long, CartItemDTO> newCart = new LinkedHashMap<>();
        session.setAttribute(CART_SESSION_KEY, newCart);
        return newCart;
    }
}