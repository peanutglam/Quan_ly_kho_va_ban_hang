package controller;

import dto.CartItemDTO;
import entity.AppUser;
import entity.Order;
import entity.Product;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import service.CustomerAccountService;
import service.OrderService;
import service.ProductService;
import service.PublicOrderCustomerBinderService;
import service.ShopProfileService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class CartController {

    private static final String CART_SESSION_KEY = "SHOP_CART";

    private final ProductService productService;
    private final OrderService orderService;
    private final ShopProfileService shopProfileService;
    private final PublicOrderCustomerBinderService publicOrderCustomerBinderService;
    private final CustomerAccountService customerAccountService;

    public CartController(ProductService productService,
                          OrderService orderService,
                          ShopProfileService shopProfileService,
                          PublicOrderCustomerBinderService publicOrderCustomerBinderService,
                          CustomerAccountService customerAccountService) {
        this.productService = productService;
        this.orderService = orderService;
        this.shopProfileService = shopProfileService;
        this.publicOrderCustomerBinderService = publicOrderCustomerBinderService;
        this.customerAccountService = customerAccountService;
    }

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
            Product product = productService.getPublicProductById(productId);

            int currentStock = product.getQuantity() == null ? 0 : product.getQuantity();

            if (currentStock <= 0) {
                redirectAttrs.addFlashAttribute("errorMessage", "Sản phẩm đã hết hàng");
                return "redirect:/shop";
            }

            Map<Long, CartItemDTO> cart = getCart(session);

            CartItemDTO existing = cart.get(productId);

            int addQuantity = Math.max(1, quantity);
            int newQuantity = (existing == null ? 0 : existing.getQuantity()) + addQuantity;
            int maxQuantity = currentStock;

            if (newQuantity > maxQuantity) {
                newQuantity = maxQuantity;
                redirectAttrs.addFlashAttribute("warnMessage", "Chỉ còn " + maxQuantity + " sản phẩm trong kho");
            }

            cart.put(productId, new CartItemDTO(
                    productId,
                    product.getCode(),
                    product.getName(),
                    product.getSalePrice(),
                    product.getEffectiveSalePrice(),
                    product.isPromotionCurrentlyActive(),
                    product.getDiscountPercentDisplay(),
                    newQuantity,
                    maxQuantity
            ));

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
                int maxQuantity = item.getMaxStock();
                item.setQuantity(Math.min(quantity, maxQuantity));
            }
        }

        session.setAttribute(CART_SESSION_KEY, cart);

        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam Long productId,
                                 HttpSession session) {
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

    @GetMapping("/checkout")
    public String checkoutForm(HttpSession session,
                               HttpServletRequest request,
                               Model model) {
        Map<Long, CartItemDTO> cart = getCart(session);

        if (cart.isEmpty()) {
            return "redirect:/shop";
        }

        BigDecimal total = cart.values().stream()
                .map(CartItemDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        AppUser currentCustomer = getCurrentCustomerSafely(request);

        model.addAttribute("cartItems", new ArrayList<>(cart.values()));
        model.addAttribute("cartTotal", total);
        model.addAttribute("shopProfile", shopProfileService.getPublicProfile());

        model.addAttribute("checkoutName", currentCustomer != null ? currentCustomer.getFullName() : "");
        model.addAttribute("checkoutPhone", currentCustomer != null ? currentCustomer.getPhone() : "");
        model.addAttribute("checkoutAddress", currentCustomer != null ? currentCustomer.getAddress() : "");

        return "cart/checkout";
    }

    @PostMapping("/checkout")
    public String placeOrder(@RequestParam String customerName,
                             @RequestParam String customerPhone,
                             @RequestParam(required = false) String customerAddress,
                             @RequestParam(required = false) String note,
                             HttpServletRequest request,
                             HttpSession session,
                             RedirectAttributes redirectAttrs) {
        Map<Long, CartItemDTO> cart = getCart(session);

        if (cart.isEmpty()) {
            return "redirect:/shop";
        }

        try {
            Map<Long, Integer> items = new LinkedHashMap<>();

            for (CartItemDTO item : cart.values()) {
                items.put(item.getProductId(), item.getQuantity());
            }

            Order order = orderService.createPublicOrder(
                    customerName,
                    customerPhone,
                    customerAddress == null ? "" : customerAddress,
                    note,
                    items
            );

            /*
             * Bước 11:
             * Nếu khách hàng đã đăng nhập thì gắn đơn vừa tạo với tài khoản khách.
             * Nếu khách chưa đăng nhập thì đơn vẫn là đơn public bình thường.
             */
            order = publicOrderCustomerBinderService.bindCustomerToOrder(request, order);

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

    private AppUser getCurrentCustomerSafely(HttpServletRequest request) {
        try {
            return customerAccountService.getCurrentCustomerOrNull(request);
        } catch (Exception ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Long, CartItemDTO> getCart(HttpSession session) {
        Object cart = session.getAttribute(CART_SESSION_KEY);

        if (cart instanceof Map<?, ?> map) {
            return (Map<Long, CartItemDTO>) map;
        }

        Map<Long, CartItemDTO> newCart = new LinkedHashMap<>();
        session.setAttribute(CART_SESSION_KEY, newCart);

        return newCart;
    }
}