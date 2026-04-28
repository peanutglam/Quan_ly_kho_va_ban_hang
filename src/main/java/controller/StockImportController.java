package controller;

import org.springframework.stereotype.Controller;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import service.AuthService;
import service.ProductService;
import service.StockImportService;
import service.SupplierService;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/imports")
public class StockImportController {

    private final StockImportService stockImportService;
    private final ProductService productService;
    private final SupplierService supplierService;
    private final AuthService authService;

    public StockImportController(StockImportService stockImportService,
                                 ProductService productService,
                                 SupplierService supplierService,
                                 AuthService authService) {
        this.stockImportService = stockImportService;
        this.productService = productService;
        this.supplierService = supplierService;
        this.authService = authService;
    }

    @GetMapping
    public String listImports(Model model) {
        model.addAttribute("imports", stockImportService.getAllImports());
        return "imports/list";
    }

    @GetMapping("/create")
    public String showImportForm(Model model) {
        authService.requireRole("OWNER", "STAFF");
        model.addAttribute("products", productService.getAllProducts(null, authService.getCurrentUser()));
        model.addAttribute("suppliers", supplierService.getAllSuppliers());
        return "imports/form";
    }

    @PostMapping("/create")
    public String createImport(@RequestParam Long productId,
                               @RequestParam Long supplierId,
                               @RequestParam Integer quantity,
                               @RequestParam BigDecimal importPrice,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
                               @RequestParam(required = false) String note,
                               Model model) {
        authService.requireRole("OWNER", "STAFF");
        try {
            stockImportService.createImport(productId, supplierId, quantity, importPrice, expiryDate, note);
            return "redirect:/imports";
        } catch (IllegalArgumentException e) {
            model.addAttribute("products", productService.getAllProducts(null, authService.getCurrentUser()));
            model.addAttribute("suppliers", supplierService.getAllSuppliers());
            model.addAttribute("errorMessage", e.getMessage());
            return "imports/form";
        }
    }
}
