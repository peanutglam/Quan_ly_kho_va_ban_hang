package controller.api;

import dto.CreateOrderApiRequest;
import dto.OrderApiResponse;
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
            AppUser owner = authService.getWorkspaceOwner(currentUser);

            Page<Order> orderPage = orderService.getOrdersPageForApi(owner, page, size, keyword, status);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("content", orderPage.getContent().stream().map(OrderApiResponse::new).toList());
            body.put("currentPage", orderPage.getNumber());
            body.put("totalPages", orderPage.getTotalPages());
            body.put("totalElements", orderPage.getTotalElements());
            body.put("size", orderPage.getSize());

            return ResponseEntity.ok(body);
        } catch (Exception e) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", false);
            body.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(body);
        }
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderApiRequest request) {
        try {
            AppUser currentUser = authService.getCurrentUser();
            AppUser owner = authService.getWorkspaceOwner(currentUser);

            Order order = orderService.createOrderFromMobileApi(owner, request);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", true);
            body.put("message", "Tạo đơn hàng thành công");
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