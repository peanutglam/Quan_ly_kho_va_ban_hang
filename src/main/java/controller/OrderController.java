package controller;

import entity.AppUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import service.AuthService;
import service.OrderService;
import service.ProductService;
import service.ShopProfileService;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final ProductService productService;
    private final AuthService authService;
    private final ShopProfileService shopProfileService;

    public OrderController(OrderService orderService, ProductService productService,
                           AuthService authService, ShopProfileService shopProfileService) {
        this.orderService = orderService;
        this.productService = productService;
        this.authService = authService;
        this.shopProfileService = shopProfileService;
    }

    @GetMapping
    public String listOrders(@RequestParam(value = "keyword", required = false) String keyword,
                             @RequestParam(value = "status", required = false) String status,
                             Model model) {
        AppUser user = authService.getCurrentUser();
        var orders = orderService.filterOrders(keyword, status);
        // Tính tổng bill
        java.math.BigDecimal grandTotal = orders.stream()
                .map(o -> o.getTotalAmount() == null ? java.math.BigDecimal.ZERO : o.getTotalAmount())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        model.addAttribute("orders", orders);
        model.addAttribute("grandTotal", grandTotal);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        return "orders/list";
    }

    @GetMapping("/create")
    public String showCreateOrder(Model model) {
        authService.requireRole("OWNER", "SALE");
        AppUser user = authService.getCurrentUser();
        model.addAttribute("products", productService.getAllProducts(null, user));
        return "orders/form";
    }

    @PostMapping("/create")
    public String createOrder(@RequestParam String customerName,
                              @RequestParam String customerPhone,
                              @RequestParam String customerAddress,
                              @RequestParam(value = "productIds", required = false) List<String> productIdStrs,
                              @RequestParam(value = "quantities", required = false) List<String> quantityStrs,
                              Model model) {
        authService.requireRole("OWNER", "SALE");
        AppUser user = authService.getCurrentUser();

        // Lọc bỏ các dòng trống
        List<Long> productIds = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();
        if (productIdStrs != null) {
            for (int i = 0; i < productIdStrs.size(); i++) {
                String pid = productIdStrs.get(i);
                if (pid == null || pid.isBlank()) continue;
                try {
                    long id = Long.parseLong(pid.trim());
                    int qty = 1;
                    if (quantityStrs != null && i < quantityStrs.size()) {
                        try { qty = Integer.parseInt(quantityStrs.get(i).trim()); } catch (Exception ignored) {}
                    }
                    if (qty > 0) {
                        productIds.add(id);
                        quantities.add(qty);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        if (productIds.isEmpty()) {
            model.addAttribute("products", productService.getAllProducts(null, user));
            model.addAttribute("errorMessage", "Vui lòng chọn ít nhất một sản phẩm.");
            return "orders/form";
        }

        try {
            orderService.createOrder(customerName, customerPhone, customerAddress, productIds, quantities);
            return "redirect:/orders";
        } catch (IllegalArgumentException e) {
            model.addAttribute("products", productService.getAllProducts(null, user));
            model.addAttribute("errorMessage", e.getMessage());
            return "orders/form";
        }
    }

    @GetMapping("/detail/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getById(id));
        model.addAttribute("shopProfile", shopProfileService.getCurrentProfile());
        return "orders/detail";
    }

    @PostMapping("/status/{id}")
    public String updateStatus(@PathVariable Long id, @RequestParam String status) {
        authService.requireRole("OWNER", "SALE");
        orderService.updateStatus(id, status);
        return "redirect:/orders/detail/" + id;
    }

    @GetMapping("/delete/{id}")
    public String deleteOrder(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        authService.requireRole("OWNER");
        try {
            orderService.deleteOrder(id);
            redirectAttrs.addFlashAttribute("successMessage", "Đã xóa đơn hàng.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", "Không thể xóa: " + e.getMessage());
        }
        return "redirect:/orders";
    }

    @GetMapping("/delete-all")
    public String deleteAll(RedirectAttributes redirectAttrs) {
        authService.requireRole("OWNER");
        try {
            orderService.deleteAll();
            redirectAttrs.addFlashAttribute("successMessage", "Đã xóa toàn bộ đơn hàng.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/orders";
    }
}