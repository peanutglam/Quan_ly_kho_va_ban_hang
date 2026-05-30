package controller.api;

import dto.ApiPageResponse;
import dto.ProductApiResponse;
import entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.ProductService;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductApiController {

    private final ProductService productService;

    public ProductApiController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiPageResponse<ProductApiResponse> products(@RequestParam(value = "q", required = false) String keyword,
                                                        @RequestParam(value = "category", required = false) String category,
                                                        @RequestParam(value = "sale", defaultValue = "false") boolean saleOnly,
                                                        @RequestParam(value = "page", defaultValue = "0") int page,
                                                        @RequestParam(value = "size", defaultValue = "24") int size) {
        Page<Product> productPage = productService.getPublicProductsPage(keyword, category, saleOnly, page, size);

        List<ProductApiResponse> content = productPage.getContent()
                .stream()
                .map(ProductApiResponse::from)
                .toList();

        return ApiPageResponse.from(productPage, content);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ProductApiResponse.from(productService.getPublicProductById(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(java.util.Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return productService.getPublicCategories();
    }
}
