package controller.api;

import dto.DashboardApiResponse;
import entity.AppUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {

    private final AuthService authService;
    private final ProductService productService;
    private final SupplierService supplierService;
    private final OrderService orderService;
    private final StockImportService stockImportService;

    public DashboardApiController(AuthService authService,
                                  ProductService productService,
                                  SupplierService supplierService,
                                  OrderService orderService,
                                  StockImportService stockImportService) {
        this.authService = authService;
        this.productService = productService;
        this.supplierService = supplierService;
        this.orderService = orderService;
        this.stockImportService = stockImportService;
    }

    @GetMapping
    public ResponseEntity<?> dashboard() {
        AppUser currentUser = authService.getCurrentUser();
        AppUser owner = authService.getWorkspaceOwner(currentUser);

        DashboardApiResponse response = new DashboardApiResponse(
                productService.countProducts(owner),
                supplierService.getAllSuppliers().size(),
                orderService.countOrders(),
                stockImportService.getAllImports().size(),
                orderService.totalRevenue(),
                orderService.countByStatus(OrderService.STATUS_PENDING),
                orderService.countByStatus(OrderService.STATUS_SHIPPING),
                orderService.countByStatus(OrderService.STATUS_COMPLETED),
                orderService.countByStatus(OrderService.STATUS_CANCELLED),
                productService.getLowStockProducts(owner).size(),
                productService.getExpiringProducts(owner).size()
        );

        return ResponseEntity.ok(response);
    }
}