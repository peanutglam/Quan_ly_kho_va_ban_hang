package controller;

import entity.Product;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import service.ProductService;
import service.ShopProfileService;

import java.util.List;

@Controller
@RequestMapping("/shop")
public class ShopController {

    private final ProductService productService;
    private final ShopProfileService shopProfileService;

    public ShopController(ProductService productService,
                          ShopProfileService shopProfileService) {
        this.productService = productService;
        this.shopProfileService = shopProfileService;
    }

    @GetMapping({"", "/"})
    public String shop(@RequestParam(value = "q", required = false) String keyword,
                       @RequestParam(value = "category", required = false) String category,
                       Model model) {

        List<Product> products = productService.getPublicProducts(keyword);

        if (category != null && !category.isBlank()) {
            products = products.stream()
                    .filter(p -> category.equalsIgnoreCase(p.getCategory()))
                    .toList();
        }

        List<String> categories = productService.getPublicProducts(null)
                .stream()
                .map(Product::getCategory)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted()
                .toList();

        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategory", category);

        /*
         * Quan trọng:
         * Trang /shop là public nên phải dùng getPublicProfile(),
         * không dùng getCurrentProfile().
         */
        model.addAttribute("shopProfile", shopProfileService.getPublicProfile());

        return "shop/index";
    }

    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable Long id, Model model) {
        Product product = productService.getPublicProductById(id);

        model.addAttribute("product", product);
        model.addAttribute("shopProfile", shopProfileService.getPublicProfile());

        return "shop/detail";
    }
}