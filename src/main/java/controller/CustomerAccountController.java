package controller;

import entity.AppUser;
import entity.Order;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import service.CustomerAccountService;
import service.ShopProfileService;

@Controller
@RequestMapping("/customer")
public class CustomerAccountController {

    private final CustomerAccountService customerAccountService;
    private final ShopProfileService shopProfileService;

    public CustomerAccountController(CustomerAccountService customerAccountService,
                                     ShopProfileService shopProfileService) {
        this.customerAccountService = customerAccountService;
        this.shopProfileService = shopProfileService;
    }

    /*
     * LƯU Ý:
     * Không khai báo /customer/login
     * Không khai báo /customer/register
     * Không khai báo /customer/logout
     *
     * Vì các route này project của bạn đã có trong AuthController.
     * Nếu khai báo lại ở đây sẽ bị lỗi Ambiguous mapping.
     */

    @GetMapping("/account")
    public String account(HttpServletRequest request,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        try {
            AppUser customer = customerAccountService.getCurrentCustomer(request);

            model.addAttribute("customer", customer);
            model.addAttribute("shopProfile", shopProfileService.getPublicProfile());

            return "customer/account";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn cần đăng nhập tài khoản khách hàng.");
            return "redirect:/customer/login";
        }
    }

    @GetMapping("/profile")
    public String profilePage(HttpServletRequest request,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        try {
            AppUser customer = customerAccountService.getCurrentCustomer(request);

            model.addAttribute("customer", customer);
            model.addAttribute("shopProfile", shopProfileService.getPublicProfile());

            return "customer/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn cần đăng nhập tài khoản khách hàng.");
            return "redirect:/customer/login";
        }
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam String fullName,
                                @RequestParam(required = false) String email,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String address,
                                @RequestParam(required = false) String currentPassword,
                                @RequestParam(required = false) String newPassword,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        try {
            customerAccountService.updateProfile(
                    request,
                    fullName,
                    email,
                    phone,
                    address,
                    currentPassword,
                    newPassword
            );

            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật thông tin cá nhân.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn cần đăng nhập tài khoản khách hàng.");
            return "redirect:/customer/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/customer/profile";
    }

    @GetMapping("/orders")
    public String myOrders(@RequestParam(value = "page", defaultValue = "0") int page,
                           HttpServletRequest request,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            Page<Order> orderPage = customerAccountService.getMyOrders(request, page, 10);

            model.addAttribute("orders", orderPage.getContent());
            model.addAttribute("orderPage", orderPage);
            model.addAttribute("currentPage", orderPage.getNumber());
            model.addAttribute("totalPages", orderPage.getTotalPages());
            model.addAttribute("shopProfile", shopProfileService.getPublicProfile());

            return "customer/orders";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn cần đăng nhập tài khoản khách hàng.");
            return "redirect:/customer/login";
        }
    }

    @GetMapping("/orders/{id}")
    public String myOrderDetail(@PathVariable Long id,
                                HttpServletRequest request,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        try {
            Order order = customerAccountService.getMyOrderDetail(request, id);

            model.addAttribute("order", order);
            model.addAttribute("shopProfile", shopProfileService.getPublicProfile());

            return "customer/order-detail";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn cần đăng nhập tài khoản khách hàng.");
            return "redirect:/customer/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/customer/orders";
        }
    }
}