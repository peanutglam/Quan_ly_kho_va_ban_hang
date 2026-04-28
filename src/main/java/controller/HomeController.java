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
    public String home(Model model) {
        AppUser currentUser;

        try {
            currentUser = authService.getCurrentUser();
        } catch (Exception e) {
            return "redirect:/login";
        }

        AppUser workspaceOwner = authService.getWorkspaceOwner(currentUser);

        boolean isOwner = "OWNER".equalsIgnoreCase(currentUser.getRole());
        boolean isEmployee = !isOwner;

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("workspaceOwner", workspaceOwner);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("isEmployee", isEmployee);

        productService.synchronizeProductStatistics(workspaceOwner);

        model.addAttribute("totalProducts", productService.countProducts(workspaceOwner));
        model.addAttribute("totalSuppliers", supplierService.getAllSuppliers().size());
        model.addAttribute("totalOrders", orderService.countOrders());
        model.addAttribute("totalRevenue", orderService.totalRevenue());
        model.addAttribute("totalImports", stockImportService.getAllImports().size());

        model.addAttribute("pendingOrders", orderService.countByStatus(OrderService.STATUS_PENDING));
        model.addAttribute("shippingOrders", orderService.countByStatus(OrderService.STATUS_SHIPPING));
        model.addAttribute("completedOrders", orderService.countByStatus(OrderService.STATUS_COMPLETED));
        model.addAttribute("cancelledOrders", orderService.countByStatus(OrderService.STATUS_CANCELLED));

        model.addAttribute("revenueByMonth", orderService.revenueByMonth());
        model.addAttribute("orderStatusStats", orderService.orderStatusStatistics());

        model.addAttribute("lowStockProducts", productService.getLowStockProducts(workspaceOwner));
        model.addAttribute("expiringProducts", productService.getExpiringProducts(workspaceOwner));

        model.addAttribute("bestSellingProducts", orderService.getBestSellingProducts());

        return "index";
    }
}