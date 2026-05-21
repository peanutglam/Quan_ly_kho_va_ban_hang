package controller;

import entity.ShopProfile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import service.AuthService;
import service.ShopProfileService;

@Controller
@RequestMapping("/shop-config")
public class ShopConfigController {

    private final ShopProfileService shopProfileService;
    private final AuthService authService;

    public ShopConfigController(ShopProfileService shopProfileService,
                                AuthService authService) {
        this.shopProfileService = shopProfileService;
        this.authService = authService;
    }

    @GetMapping
    public String form(Model model) {
        authService.requireRole("OWNER");

        ShopProfile profile = shopProfileService.getCurrentProfile();

        model.addAttribute("profile", profile);

        return "shop-config/form";
    }

    @PostMapping
    public String save(@ModelAttribute("profile") ShopProfile profile,
                       RedirectAttributes redirectAttributes) {
        authService.requireRole("OWNER");

        try {
            shopProfileService.updateShopConfig(profile);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu cấu hình cửa hàng thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lưu cấu hình cửa hàng thất bại: " + e.getMessage());
        }

        return "redirect:/shop-config";
    }
}