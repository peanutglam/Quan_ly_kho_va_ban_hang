package controller.api;

import dto.DashboardApiResponse;
import entity.AppUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.AuthService;
import service.OrderService;
import service.ProductService;
import service.StockImportService;
import service.SupplierService;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {

    private final ProductService productService;
    private final SupplierService supplierService;
    private final OrderService orderService;
    private final StockImportService stockImportService;
    private final AuthService authService;

    public DashboardApiController(ProductService productService,
                                  SupplierService supplierService,
                                  OrderService orderService,
                                  StockImportService stockImportService,
                                  AuthService authService) {
        this.productService = productService;
        this.supplierService = supplierService;
        this.orderService = orderService;
        this.stockImportService = stockImportService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<?> dashboard() {
        try {
            AppUser currentUser = authService.getCurrentUser();
            String role = normalizeRole(currentUser.getRole());

            if (AppUser.ROLE_CUSTOMER.equals(role)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Khách hàng không có quyền xem Dashboard"));
            }

            AppUser owner = authService.getWorkspaceOwner(currentUser);

            DashboardApiResponse response = new DashboardApiResponse();
            response.setTotalProducts(productService.countProducts(owner));
            response.setTotalSuppliers(supplierService.countSuppliers());
            response.setTotalOrders(orderService.countOrders());
            response.setTotalRevenue(orderService.totalRevenue());
            response.setTotalImports(stockImportService.countImports());
            response.setPendingOrders(orderService.countByStatus(OrderService.STATUS_PENDING));
            response.setShippingOrders(orderService.countByStatus(OrderService.STATUS_SHIPPING));
            response.setCompletedOrders(
                    orderService.countByStatus(OrderService.STATUS_COMPLETED)
                            + orderService.countByStatus(OrderService.STATUS_DELIVERED)
            );
            response.setCancelledOrders(orderService.countByStatus(OrderService.STATUS_CANCELLED));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Bạn cần đăng nhập"));
        }
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "";
        }

        String value = role.trim().toUpperCase();
        return value.startsWith("ROLE_") ? value.substring(5) : value;
    }
}
