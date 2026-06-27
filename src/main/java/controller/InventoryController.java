package controller;

import entity.InventoryCheck;
import entity.InventoryLog;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import service.AuthService;
import service.InventoryCheckService;
import service.InventoryLogService;
import service.ProductService;

import java.util.List;

@Controller
public class InventoryController {

    private final InventoryCheckService inventoryCheckService;
    private final InventoryLogService inventoryLogService;
    private final ProductService productService;
    private final AuthService authService;

    public InventoryController(InventoryCheckService inventoryCheckService,
                               InventoryLogService inventoryLogService,
                               ProductService productService,
                               AuthService authService) {
        this.inventoryCheckService = inventoryCheckService;
        this.inventoryLogService = inventoryLogService;
        this.productService = productService;
        this.authService = authService;
    }

    @GetMapping("/inventory-checks")
    public String inventoryCheckPage(@RequestParam(value = "page", required = false, defaultValue = "0") int page,
                                     Model model) {
        authService.requireRole("OWNER", "STAFF");
        Page<InventoryCheck> checkPage = inventoryCheckService.getChecks(page, 20);

        model.addAttribute("products", productService.getAllProducts(null, authService.getCurrentUser()));
        model.addAttribute("checks", checkPage.getContent());
        model.addAttribute("checkPage", checkPage);
        model.addAttribute("currentPage", checkPage.getNumber());
        model.addAttribute("totalPages", checkPage.getTotalPages());

        return "inventory/checks";
    }

    @PostMapping("/inventory-checks")
    public String performInventoryCheck(@RequestParam(value = "productIds", required = false) List<Long> productIds,
                                        @RequestParam(value = "actualQuantities", required = false) List<Integer> actualQuantities,
                                        @RequestParam(value = "reasons", required = false) List<String> reasons,
                                        @RequestParam(value = "notes", required = false) List<String> notes,
                                        RedirectAttributes redirectAttributes) {
        authService.requireRole("OWNER", "STAFF");

        try {
            int count = inventoryCheckService.performInventoryCheck(productIds, actualQuantities, reasons, notes);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu kiểm kê " + count + " sản phẩm.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể lưu kiểm kê: " + e.getMessage());
        }

        return "redirect:/inventory-checks";
    }

    @GetMapping("/inventory-logs")
    public String inventoryLogs(@RequestParam(value = "page", required = false, defaultValue = "0") int page,
                                Model model) {
        authService.requireRole("OWNER", "STAFF", "SALE");
        Page<InventoryLog> logPage = inventoryLogService.getLogs(page, 30);

        model.addAttribute("logs", logPage.getContent());
        model.addAttribute("logPage", logPage);
        model.addAttribute("currentPage", logPage.getNumber());
        model.addAttribute("totalPages", logPage.getTotalPages());

        return "inventory/logs";
    }
}