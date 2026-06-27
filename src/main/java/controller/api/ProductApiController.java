package controller.api;

import dto.ProductApiResponse;
import entity.AppUser;
import entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.AuthService;
import service.ProductService;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductApiController {

    private final ProductService productService;
    private final AuthService authService;

    public ProductApiController(ProductService productService,
                                AuthService authService) {
        this.productService = productService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<?> listProducts(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          @RequestParam(required = false) String keyword) {
        try {
            AppUser currentUser = authService.getCurrentUser();
            authService.requireRole("OWNER", "STAFF", "SALE");
            AppUser owner = authService.getWorkspaceOwner(currentUser);

            Page<Product> productPage = productService.filterProductsPage(
                    owner,
                    keyword,
                    "",
                    "",
                    page,
                    size
            );

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("content", productPage.getContent().stream().map(ProductApiResponse::new).toList());
            body.put("currentPage", productPage.getNumber());
            body.put("totalPages", productPage.getTotalPages());
            body.put("totalElements", productPage.getTotalElements());
            body.put("size", productPage.getSize());

            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable Long id) {
        try {
            AppUser currentUser = authService.getCurrentUser();
            authService.requireRole("OWNER", "STAFF", "SALE");
            AppUser owner = authService.getWorkspaceOwner(currentUser);

            Product product = productService.getById(id, owner);

            return ResponseEntity.ok(new ProductApiResponse(product));
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