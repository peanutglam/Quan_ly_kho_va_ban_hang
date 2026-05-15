package controller;

import entity.AppUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import service.AuthService;
import service.OrderService;
import service.ProductService;
import service.StockImportService;
import service.SupplierService;

@Controller
public class HomeController {

    private final ProductService productService;
    private final SupplierService supplierService;
    private final OrderService orderService;
    private final StockImportService stockImportService;
    private final AuthService authService;

    public HomeController(ProductService productService,
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

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        AppUser currentUser;

        try {
            currentUser = authService.getCurrentUser();
        } catch (Exception e) {
            return "redirect:/login";
        }

        AppUser owner = authService.getWorkspaceOwner(currentUser);

        String role = currentUser.getRole() == null ? "" : currentUser.getRole().trim();
        boolean isOwner = "OWNER".equalsIgnoreCase(role);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("workspaceOwner", owner);
        model.addAttribute("isOwner", isOwner);

        model.addAttribute("totalProducts", productService.countProducts(owner));
        model.addAttribute("totalSuppliers", supplierService.countSuppliers());
        model.addAttribute("totalOrders", orderService.countOrders());
        model.addAttribute("totalRevenue", orderService.totalRevenue());
        model.addAttribute("totalImports", stockImportService.countImports());

        model.addAttribute("pendingOrders", orderService.countByStatus(OrderService.STATUS_PENDING));
        model.addAttribute("shippingOrders", orderService.countByStatus(OrderService.STATUS_SHIPPING));
        model.addAttribute(
                "completedOrders",
                orderService.countByStatus(OrderService.STATUS_COMPLETED)
                        + orderService.countByStatus(OrderService.STATUS_DELIVERED)
        );
        model.addAttribute("cancelledOrders", orderService.countByStatus(OrderService.STATUS_CANCELLED));

        model.addAttribute("lowStockProducts", productService.getLowStockProducts(owner));
        model.addAttribute("expiringProducts", productService.getExpiringProducts(owner));
        model.addAttribute("bestSellingProducts", orderService.getBestSellingProducts());

        return "index";
    }
}