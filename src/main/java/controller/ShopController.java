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
                       @RequestParam(value = "prefix", required = false) String prefix,
                       @RequestParam(value = "sale", defaultValue = "false") boolean saleOnly,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       Model model) {

        Page<Product> productPage = productService.getPublicProductsPage(
                keyword,
                category,
                prefix,
                saleOnly,
                page,
                DEFAULT_SHOP_PAGE_SIZE
        );

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("productPage", productPage);
        model.addAttribute("categories", productService.getPublicCategories());

        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedPrefix", prefix);
        model.addAttribute("saleOnly", saleOnly);

        model.addAttribute("currentPage", productPage.getNumber());
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalElements", productPage.getTotalElements());
        model.addAttribute("pageSize", productPage.getSize());
        int totalPages = productPage.getTotalPages();
        int currentPage = productPage.getNumber();

        int pageGroupSize = 3;
        int pageGroupStart = (currentPage / pageGroupSize) * pageGroupSize;
        int pageGroupEnd = Math.min(pageGroupStart + pageGroupSize - 1, totalPages - 1);

        int previousGroupPage = Math.max(pageGroupStart - pageGroupSize, 0);
        int nextGroupPage = Math.min(pageGroupStart + pageGroupSize, Math.max(totalPages - 1, 0));

        model.addAttribute("pageGroupStart", pageGroupStart);
        model.addAttribute("pageGroupEnd", pageGroupEnd);
        model.addAttribute("previousGroupPage", previousGroupPage);
        model.addAttribute("nextGroupPage", nextGroupPage);
        model.addAttribute("lastPage", Math.max(totalPages - 1, 0));
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