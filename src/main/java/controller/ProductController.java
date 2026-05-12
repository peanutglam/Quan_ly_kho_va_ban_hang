package controller;

import entity.AppUser;
import entity.Product;
import entity.Supplier;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import service.AuthService;
import service.ProductService;
import service.SupplierService;
import org.springframework.data.domain.Page;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final SupplierService supplierService;
    private final AuthService authService;

    public ProductController(ProductService productService,
                             SupplierService supplierService,
                             AuthService authService) {
        this.productService = productService;
        this.supplierService = supplierService;
        this.authService = authService;
    }

    @GetMapping
    public String listProducts(@RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "stockStatus", required = false) String stockStatus,
                               @RequestParam(value = "expiryStatus", required = false) String expiryStatus,
                               @RequestParam(value = "page", required = false, defaultValue = "0") int page,
                               Model model) {
        AppUser user = authService.getCurrentUser();

        Page<Product> productPage = productService.filterProductsPage(
                user,
                keyword,
                stockStatus,
                expiryStatus,
                page,
                30
        );

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("productPage", productPage);
        model.addAttribute("currentPage", productPage.getNumber());
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalElements", productPage.getTotalElements());

        model.addAttribute("keyword", keyword);
        model.addAttribute("stockStatus", stockStatus);
        model.addAttribute("expiryStatus", expiryStatus);

        return "products/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        authService.requireRole("OWNER", "STAFF");

        model.addAttribute("product", new Product());
        model.addAttribute("suppliers", supplierService.getAllSuppliers());
        model.addAttribute("pageTitle", "Thêm sản phẩm");
        model.addAttribute("formAction", "/products/create");

        return "products/form";
    }

    @PostMapping("/create")
    public String createProduct(@Valid @ModelAttribute("product") Product product,
                                BindingResult bindingResult,
                                @RequestParam(value = "supplierId", required = false) Long supplierId,
                                Model model) {
        authService.requireRole("OWNER", "STAFF");

        AppUser user = authService.getCurrentUser();

        if (bindingResult.hasErrors()) {
            model.addAttribute("suppliers", supplierService.getAllSuppliers());
            model.addAttribute("pageTitle", "Thêm sản phẩm");
            model.addAttribute("formAction", "/products/create");
            return "products/form";
        }

        if (supplierId != null) {
            try {
                Supplier supplier = supplierService.getSupplierById(supplierId);
                product.setSupplier(supplier);
            } catch (Exception ignored) {
            }
        }

        try {
            productService.create(product, user);
            return "redirect:/products";
        } catch (IllegalArgumentException e) {
            model.addAttribute("suppliers", supplierService.getAllSuppliers());
            model.addAttribute("pageTitle", "Thêm sản phẩm");
            model.addAttribute("formAction", "/products/create");
            model.addAttribute("errorMessage", e.getMessage());
            return "products/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        authService.requireRole("OWNER", "STAFF");

        AppUser user = authService.getCurrentUser();

        model.addAttribute("product", productService.getById(id, user));
        model.addAttribute("suppliers", supplierService.getAllSuppliers());
        model.addAttribute("pageTitle", "Cập nhật sản phẩm");
        model.addAttribute("formAction", "/products/edit/" + id);

        return "products/form";
    }

    @PostMapping("/edit/{id}")
    public String updateProduct(@PathVariable Long id,
                                @Valid @ModelAttribute("product") Product product,
                                BindingResult bindingResult,
                                @RequestParam(value = "supplierId", required = false) Long supplierId,
                                Model model) {
        authService.requireRole("OWNER", "STAFF");

        AppUser user = authService.getCurrentUser();

        if (bindingResult.hasErrors()) {
            model.addAttribute("suppliers", supplierService.getAllSuppliers());
            model.addAttribute("pageTitle", "Cập nhật sản phẩm");
            model.addAttribute("formAction", "/products/edit/" + id);
            return "products/form";
        }

        if (supplierId != null) {
            try {
                Supplier supplier = supplierService.getSupplierById(supplierId);
                product.setSupplier(supplier);
            } catch (Exception ignored) {
            }
        }

        try {
            productService.update(id, product, user);
            return "redirect:/products";
        } catch (IllegalArgumentException e) {
            model.addAttribute("suppliers", supplierService.getAllSuppliers());
            model.addAttribute("pageTitle", "Cập nhật sản phẩm");
            model.addAttribute("formAction", "/products/edit/" + id);
            model.addAttribute("errorMessage", e.getMessage());
            return "products/form";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        authService.requireRole("OWNER", "STAFF");

        AppUser user = authService.getCurrentUser();

        try {
            String msg = productService.delete(id, user);
            redirectAttrs.addFlashAttribute("successMessage", msg);
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", "Không thể xóa: " + e.getMessage());
        }

        return "redirect:/products";
    }

    /*
     * Không cho xóa toàn bộ bằng GET nữa.
     */
    @GetMapping("/delete-all")
    public String deleteAllByGet(RedirectAttributes redirectAttrs) {
        redirectAttrs.addFlashAttribute(
                "errorMessage",
                "Vui lòng dùng nút Xóa toàn bộ và nhập mật khẩu Owner để xác nhận."
        );

        return "redirect:/products";
    }

    /*
     * Xóa toàn bộ sản phẩm phải là OWNER và phải nhập đúng mật khẩu OWNER.
     */
    @PostMapping("/delete-all")
    public String deleteAll(@RequestParam String ownerPassword,
                            RedirectAttributes redirectAttrs) {
        authService.requireRole("OWNER");

        AppUser user = authService.getCurrentUser();

        try {
            authService.verifyOwnerPassword(ownerPassword);
            productService.deleteAll(user);

            redirectAttrs.addFlashAttribute(
                    "successMessage",
                    "Đã xóa/ẩn toàn bộ sản phẩm. Dữ liệu lịch sử đơn hàng cũ vẫn được giữ an toàn."
            );
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", "Không thể xóa toàn bộ sản phẩm: " + e.getMessage());
        }

        return "redirect:/products";
    }
}