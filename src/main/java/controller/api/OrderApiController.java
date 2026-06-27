package controller.api;

import dto.CreateOrderApiRequest;
import dto.OrderApiResponse;
import dto.UpdateOrderStatusApiRequest;
import entity.AppUser;
import entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.AuthService;
import service.OrderService;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderApiController {

    private final OrderService orderService;
    private final AuthService authService;

    public OrderApiController(OrderService orderService,
                              AuthService authService) {
        this.orderService = orderService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<?> listOrders(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size,
                                        @RequestParam(defaultValue = "") String keyword,
                                        @RequestParam(defaultValue = "") String status) {
        try {
            AppUser currentUser = authService.getCurrentUser();
            authService.requireRole("OWNER", "SALE");
            AppUser owner = authService.getWorkspaceOwner(currentUser);

            Page<Order> orderPage = orderService.getOrdersPageForApi(owner, page, size, keyword, status);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", true);
            body.put("content", orderPage.getContent().stream().map(OrderApiResponse::new).toList());
            body.put("currentPage", orderPage.getNumber());
            body.put("totalPages", orderPage.getTotalPages());
            body.put("totalElements", orderPage.getTotalElements());
            body.put("size", orderPage.getSize());

            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderDetail(@PathVariable Long id) {
        try {
            AppUser currentUser = authService.getCurrentUser();
            authService.requireRole("OWNER", "SALE");
            AppUser owner = authService.getWorkspaceOwner(currentUser);

            Order order = orderService.getOrderDetailForApi(owner, id);

            return ResponseEntity.ok(new OrderApiResponse(order));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderApiRequest request) {
        try {
            AppUser currentUser = authService.getCurrentUser();
            authService.requireRole("OWNER", "SALE");
            AppUser owner = authService.getWorkspaceOwner(currentUser);

            Order order = orderService.createOrderFromMobileApi(owner, request);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", true);
            body.put("message", "Tạo đơn hàng thành công");
            body.put("id", order.getId());
            body.put("orderCode", order.getOrderCode());
            body.put("status", order.getStatus());
            body.put("totalAmount", order.getTotalAmount());
            body.put("totalBill", order.getTotalBill());
            body.put("order", new OrderApiResponse(order));

            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id,
                                               @RequestBody(required = false) UpdateOrderStatusApiRequest request) {
        try {
            AppUser currentUser = authService.getCurrentUser();
            authService.requireRole("OWNER", "SALE");
            AppUser owner = authService.getWorkspaceOwner(currentUser);

            String status = request == null ? "" : request.getStatus();

            if (status == null || status.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(error("Trạng thái đơn hàng không được để trống"));
            }

            Order order = orderService.updateStatusForApi(owner, id, status);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", true);
            body.put("message", "Cập nhật trạng thái thành công");
            body.put("id", order.getId());
            body.put("orderCode", order.getOrderCode());
            body.put("status", order.getStatus());
            body.put("totalAmount", order.getTotalAmount());
            body.put("totalBill", order.getTotalBill());
            body.put("order", new OrderApiResponse(order));

            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", message == null || message.isBlank() ? "Có lỗi xảy ra" : message);
        return body;
    }
}