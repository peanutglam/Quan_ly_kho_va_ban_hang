package controller;

import entity.AppUser;
import jakarta.servlet.http.HttpServletResponse;
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

    /*
     * Route "/" không render dashboard trực tiếp.
     * Luôn đi qua warmup trước.
     */
    @GetMapping("/")
    public String root(HttpServletResponse response) {
        disableCache(response);
        return "redirect:/dashboard-warmup?ts=" + System.currentTimeMillis();
    }

    /*
     * Request trung gian sau login.
     * Mục tiêu: ép session/owner/service đọc dữ liệu trước,
     * sau đó mới render dashboard thật ở request kế tiếp.
     */
    @GetMapping("/dashboard-warmup")
    public String dashboardWarmup(HttpServletResponse response) {
        disableCache(response);

        try {
            AppUser currentUser = authService.getCurrentUser();
            AppUser workspaceOwner = authService.getWorkspaceOwner(currentUser);

            /*
             * Gọi trước các service chính để tránh request dashboard đầu tiên bị trả 0.
             */
            productService.countProducts(workspaceOwner);
            supplierService.getAllSuppliers().size();
            orderService.countOrders();
            orderService.totalRevenue();
            stockImportService.getAllImports().size();
            productService.getLowStockProducts(workspaceOwner);
            productService.getExpiringProducts(workspaceOwner);
            orderService.getBestSellingProducts();

        } catch (Exception e) {
            return "redirect:/login?expired=" + System.currentTimeMillis();
        }

        return "redirect:/dashboard?freshLogin=1&ts=" + System.currentTimeMillis();
    }

    /*
     * Dashboard thật.
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model,
                            HttpServletResponse response) {
        disableCache(response);

        AppUser currentUser;

        try {
            currentUser = authService.getCurrentUser();
        } catch (Exception e) {
            return "redirect:/login";
        }

        AppUser workspaceOwner = authService.getWorkspaceOwner(currentUser);

        String role = currentUser.getRole() == null ? "" : currentUser.getRole().trim();
        boolean isOwner = "OWNER".equalsIgnoreCase(role);
        boolean isEmployee = !isOwner;

        long totalProducts = productService.countProducts(workspaceOwner);
        int totalSuppliers = supplierService.getAllSuppliers().size();
        long totalOrders = orderService.countOrders();
        int totalImports = stockImportService.getAllImports().size();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("workspaceOwner", workspaceOwner);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("isEmployee", isEmployee);

        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("totalSuppliers", totalSuppliers);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalRevenue", orderService.totalRevenue());
        model.addAttribute("totalImports", totalImports);

        model.addAttribute("pendingOrders", orderService.countByStatus(OrderService.STATUS_PENDING));
        model.addAttribute("shippingOrders", orderService.countByStatus(OrderService.STATUS_SHIPPING));
        model.addAttribute("completedOrders", orderService.countByStatus(OrderService.STATUS_COMPLETED));
        model.addAttribute("cancelledOrders", orderService.countByStatus(OrderService.STATUS_CANCELLED));

        model.addAttribute("revenueByMonth", orderService.revenueByMonth());
        model.addAttribute("orderStatusStats", orderService.orderStatusStatistics());

        model.addAttribute("lowStockProducts", productService.getLowStockProducts(workspaceOwner));
        model.addAttribute("expiringProducts", productService.getExpiringProducts(workspaceOwner));
        model.addAttribute("bestSellingProducts", orderService.getBestSellingProducts());

        /*
         * Dùng cho JS reload 1 lần sau login nếu trình duyệt vẫn giữ DOM cũ.
         */
        model.addAttribute("forceClientRefresh", true);

        return "index";
    }

    private void disableCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }
}