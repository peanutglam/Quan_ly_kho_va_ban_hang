package controller;

import entity.AppUser;
import entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import service.AuthService;
import service.OrderService;
import service.ProductService;
import service.ShopProfileService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final ProductService productService;
    private final AuthService authService;
    private final ShopProfileService shopProfileService;

    private static final List<String> ORDER_STATUSES = List.of(
            OrderService.STATUS_PENDING,
            OrderService.STATUS_SHIPPING,
            OrderService.STATUS_COMPLETED,
            OrderService.STATUS_DELIVERED,
            OrderService.STATUS_CANCELLED
    );

    public OrderController(OrderService orderService,
                           ProductService productService,
                           AuthService authService,
                           ShopProfileService shopProfileService) {
        this.orderService = orderService;
        this.productService = productService;
        this.authService = authService;
        this.shopProfileService = shopProfileService;
    }

    @GetMapping
    public String listOrders(@RequestParam(value = "keyword", required = false) String keyword,
                             @RequestParam(value = "status", required = false) String status,
                             @RequestParam(value = "page", required = false, defaultValue = "0") int page,
                             Model model) {
        authService.requireRole("OWNER", "SALE");
        Page<Order> orderPage = orderService.filterOrdersPaged(keyword, status, page, 30);

        BigDecimal grandTotal = orderPage.getContent()
                .stream()
                .map(o -> o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("orderPage", orderPage);
        model.addAttribute("currentPage", orderPage.getNumber());
        model.addAttribute("totalPages", orderPage.getTotalPages());
        model.addAttribute("totalElements", orderPage.getTotalElements());
        model.addAttribute("grandTotal", grandTotal);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);

        return "orders/list";
    }

    @GetMapping("/create")
    public String showCreateOrder(Model model) {
        authService.requireRole("OWNER", "SALE");
        prepareOrderForm(model, new Order(), false);
        return "orders/form";
    }

    @PostMapping("/create")
    public String createOrder(@RequestParam String customerName,
                              @RequestParam String customerPhone,
                              @RequestParam(value = "customerAddress", required = false) String customerAddress,
                              @RequestParam(value = "status", required = false, defaultValue = OrderService.STATUS_PENDING) String status,
                              @RequestParam(value = "shippingFee", required = false) String shippingFee,
                              @RequestParam(value = "customerDeposit", required = false) String customerDeposit,
                              @RequestParam(value = "productIds", required = false) List<String> productIdStrs,
                              @RequestParam(value = "quantities", required = false) List<String> quantityStrs,
                              @RequestParam(value = "unitPrices", required = false) List<String> unitPriceStrs,
                              Model model,
                              RedirectAttributes redirectAttrs) {
        authService.requireRole("OWNER", "SALE");

        try {
            Order created = orderService.createOrderDetailed(
                    customerName,
                    customerPhone,
                    customerAddress,
                    status,
                    parseMoney(shippingFee),
                    parseMoney(customerDeposit),
                    parseProductIds(productIdStrs),
                    parseQuantities(quantityStrs),
                    parseUnitPrices(unitPriceStrs)
            );

            redirectAttrs.addFlashAttribute("successMessage", "Đã tạo đơn hàng " + created.getOrderCode() + ".");
            return "redirect:/orders";
        } catch (Exception e) {
            Order order = new Order();
            order.setCustomerName(customerName);
            order.setCustomerPhone(customerPhone);
            order.setCustomerAddress(customerAddress);
            order.setStatus(status);
            order.setShippingFee(parseMoney(shippingFee));
            order.setCustomerDeposit(parseMoney(customerDeposit));

            prepareOrderForm(model, order, false);
            model.addAttribute("errorMessage", e.getMessage());
            return "orders/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditOrder(@PathVariable Long id, Model model) {
        authService.requireRole("OWNER", "SALE");
        Order order = orderService.getById(id);
        prepareOrderForm(model, order, true);
        return "orders/form";
    }

    @PostMapping("/edit/{id}")
    public String updateOrder(@PathVariable Long id,
                              @RequestParam String customerName,
                              @RequestParam String customerPhone,
                              @RequestParam(value = "customerAddress", required = false) String customerAddress,
                              @RequestParam(value = "status", required = false, defaultValue = OrderService.STATUS_PENDING) String status,
                              @RequestParam(value = "shippingFee", required = false) String shippingFee,
                              @RequestParam(value = "customerDeposit", required = false) String customerDeposit,
                              @RequestParam(value = "productIds", required = false) List<String> productIdStrs,
                              @RequestParam(value = "quantities", required = false) List<String> quantityStrs,
                              @RequestParam(value = "unitPrices", required = false) List<String> unitPriceStrs,
                              Model model,
                              RedirectAttributes redirectAttrs) {
        authService.requireRole("OWNER", "SALE");

        try {
            orderService.updateOrderInfo(
                    id,
                    customerName,
                    customerPhone,
                    customerAddress,
                    status,
                    parseMoney(shippingFee),
                    parseMoney(customerDeposit),
                    parseProductIds(productIdStrs),
                    parseQuantities(quantityStrs),
                    parseUnitPrices(unitPriceStrs)
            );

            redirectAttrs.addFlashAttribute("successMessage", "Đã cập nhật đơn hàng.");
            return "redirect:/orders/detail/" + id;
        } catch (Exception e) {
            Order order = orderService.getById(id);
            prepareOrderForm(model, order, true);
            model.addAttribute("errorMessage", e.getMessage());
            return "orders/form";
        }
    }

    @GetMapping("/detail/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        authService.requireRole("OWNER", "SALE");
        model.addAttribute("order", orderService.getById(id));
        model.addAttribute("shopProfile", shopProfileService.getCurrentProfile());
        return "orders/detail";
    }

    @PostMapping("/status/{id}")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam String status,
                               RedirectAttributes redirectAttrs) {
        authService.requireRole("OWNER", "SALE");

        try {
            orderService.updateStatus(id, status);
            redirectAttrs.addFlashAttribute("successMessage", "Đã cập nhật trạng thái đơn hàng.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", "Không thể cập nhật trạng thái: " + e.getMessage());
        }

        return "redirect:/orders/detail/" + id;
    }

    @GetMapping("/delete/{id}")
    public String deleteOrder(@PathVariable Long id,
                              RedirectAttributes redirectAttrs) {
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
    public String deleteAllByGet(RedirectAttributes redirectAttrs) {
        redirectAttrs.addFlashAttribute(
                "errorMessage",
                "Vui lòng dùng nút Xóa toàn bộ và nhập mật khẩu Owner để xác nhận."
        );

        return "redirect:/orders";
    }

    @PostMapping("/delete-all")
    public String deleteAll(@RequestParam String ownerPassword,
                            RedirectAttributes redirectAttrs) {
        authService.requireRole("OWNER");

        try {
            authService.verifyOwnerPassword(ownerPassword);
            orderService.deleteAll();

            redirectAttrs.addFlashAttribute(
                    "successMessage",
                    "Đã xóa toàn bộ đơn hàng. Tồn kho của các đơn chưa hủy đã được hoàn lại."
            );
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", "Không thể xóa toàn bộ đơn hàng: " + e.getMessage());
        }

        return "redirect:/orders";
    }

    private void prepareOrderForm(Model model, Order order, boolean editMode) {
        AppUser owner = authService.getWorkspaceOwner();
        model.addAttribute("order", order);
        model.addAttribute("products", productService.getAllProducts(null, owner));
        model.addAttribute("statuses", ORDER_STATUSES);
        model.addAttribute("editMode", editMode);
        model.addAttribute("formTitle", editMode ? "Chỉnh sửa đơn hàng" : "Tạo đơn hàng online");
        model.addAttribute("formSubtitle", editMode
                ? "Cập nhật thông tin khách hàng, sản phẩm, số lượng, giá bán và trạng thái đơn."
                : "Chọn sản phẩm, nhập số lượng tùy ý, hệ thống tự tính tổng tiền và cập nhật tồn kho.");
    }

    private List<Long> parseProductIds(List<String> values) {
        List<Long> result = new ArrayList<>();
        if (values == null) return result;

        for (String value : values) {
            if (value == null || value.isBlank()) {
                result.add(null);
                continue;
            }

            try {
                result.add(Long.parseLong(value.trim()));
            } catch (Exception e) {
                result.add(null);
            }
        }

        return result;
    }

    private List<Integer> parseQuantities(List<String> values) {
        List<Integer> result = new ArrayList<>();
        if (values == null) return result;

        for (String value : values) {
            try {
                result.add(Integer.parseInt(value == null ? "0" : value.trim()));
            } catch (Exception e) {
                result.add(0);
            }
        }

        return result;
    }

    private List<BigDecimal> parseUnitPrices(List<String> values) {
        List<BigDecimal> result = new ArrayList<>();
        if (values == null) return result;

        for (String value : values) {
            result.add(parseMoney(value));
        }

        return result;
    }

    private BigDecimal parseMoney(String value) {
        if (value == null || value.isBlank()) return BigDecimal.ZERO;

        try {
            String clean = value.trim()
                    .replace("đ", "")
                    .replace(" ", "")
                    .replace(",", "");

            if (clean.isBlank()) return BigDecimal.ZERO;

            // Giữ đúng số thập phân kiểu 430000.00, nhưng vẫn hỗ trợ người dùng nhập 430.000
            if (clean.matches("\\d+\\.\\d{1,2}")) {
                return new BigDecimal(clean);
            }

            clean = clean.replace(".", "");
            if (clean.isBlank()) return BigDecimal.ZERO;

            return new BigDecimal(clean);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}