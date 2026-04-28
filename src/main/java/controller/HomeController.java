package controller;

import entity.AppUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import service.*;

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

        model.addAttribute("currentUser", currentUser);
        productService.synchronizeProductStatistics(currentUser);

        // Thống kê tổng quan
        model.addAttribute("totalProducts", productService.countProducts(currentUser));
        model.addAttribute("totalSuppliers", supplierService.getAllSuppliers().size());
        model.addAttribute("totalOrders", orderService.countOrders());
        model.addAttribute("totalRevenue", orderService.totalRevenue());
        model.addAttribute("totalImports", stockImportService.getAllImports().size());

        // Trạng thái đơn hàng
        model.addAttribute("pendingOrders", orderService.countByStatus(OrderService.STATUS_PENDING));
        model.addAttribute("shippingOrders", orderService.countByStatus(OrderService.STATUS_SHIPPING));
        model.addAttribute("completedOrders", orderService.countByStatus(OrderService.STATUS_COMPLETED));
        model.addAttribute("cancelledOrders", orderService.countByStatus(OrderService.STATUS_CANCELLED));

        // Biểu đồ
        model.addAttribute("revenueByMonth", orderService.revenueByMonth());
        model.addAttribute("orderStatusStats", orderService.orderStatusStatistics());

        // Cảnh báo kho hàng
        model.addAttribute("lowStockProducts", productService.getLowStockProducts(currentUser));
        model.addAttribute("expiringProducts", productService.getExpiringProducts(currentUser));

        // Top sản phẩm bán chạy
        model.addAttribute("bestSellingProducts", orderService.getBestSellingProducts());

        return "index";
    }
}
