package controller;

import entity.ShopProfile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import service.AuthService;
import service.ShopProfileService;

@Controller
@RequestMapping("/shop-profile")
public class ShopProfileController {

    private final ShopProfileService shopProfileService;
    private final AuthService authService;

    public ShopProfileController(ShopProfileService shopProfileService, AuthService authService) {
        this.shopProfileService = shopProfileService;
        this.authService = authService;
    }

    @GetMapping
    public String form(Model model) {
        authService.requireRole("OWNER");
        model.addAttribute("profile", shopProfileService.getCurrentProfile());
        return "shop-profile/form";
    }

    @PostMapping
    public String update(@ModelAttribute ShopProfile profile) {
        authService.requireRole("OWNER");
        shopProfileService.update(profile);
        return "redirect:/shop-profile";
    }
}
