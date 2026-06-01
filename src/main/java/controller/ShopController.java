package controller;

import entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import service.ProductService;
import service.ShopProfileService;

@Controller
@RequestMapping("/shop")
public class ShopController {

    private static final int DEFAULT_SHOP_PAGE_SIZE = 20;

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
                       @RequestParam(value = "sale", defaultValue = "false") boolean saleOnly,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       Model model) {

        Page<Product> productPage = productService.getPublicProductsPage(
                keyword,
                category,
                saleOnly,
                page,
                DEFAULT_SHOP_PAGE_SIZE
        );

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("productPage", productPage);
        model.addAttribute("categories", productService.getPublicCategories());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("saleOnly", saleOnly);
        model.addAttribute("currentPage", productPage.getNumber());
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalElements", productPage.getTotalElements());
        model.addAttribute("pageSize", productPage.getSize());

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