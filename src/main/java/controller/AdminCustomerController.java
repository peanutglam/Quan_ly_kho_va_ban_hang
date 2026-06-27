package controller;

import entity.AppUser;
import entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import repository.AppUserRepository;
import repository.OrderRepository;
import service.AuthService;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/customers")
public class AdminCustomerController {

    private final AppUserRepository appUserRepository;
    private final OrderRepository orderRepository;
    private final AuthService authService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminCustomerController(AppUserRepository appUserRepository,
                                   OrderRepository orderRepository,
                                   AuthService authService) {
        this.appUserRepository = appUserRepository;
        this.orderRepository = orderRepository;
        this.authService = authService;
    }

    @GetMapping
    public String customers(@RequestParam(value = "page", defaultValue = "0") int page,
                            Model model) {
        authService.requireRole(AppUser.ROLE_OWNER);

        AppUser owner = authService.getWorkspaceOwner();

        Page<AppUser> customerPage = appUserRepository.findByOwnerAndRoleOrderByIdDesc(
                owner,
                AppUser.ROLE_CUSTOMER,
                PageRequest.of(Math.max(page, 0), 20)
        );

        model.addAttribute("customers", customerPage.getContent());
        model.addAttribute("customerPage", customerPage);
        model.addAttribute("currentPage", customerPage.getNumber());
        model.addAttribute("totalPages", customerPage.getTotalPages());

        return "admin/customers";
    }

    @GetMapping("/{id}")
    public String customerDetail(@PathVariable Long id,
                                 @RequestParam(value = "page", defaultValue = "0") int page,
                                 Model model) {
        authService.requireRole(AppUser.ROLE_OWNER);

        AppUser owner = authService.getWorkspaceOwner();

        AppUser customer = appUserRepository.findByIdAndOwnerAndRole(id, owner, AppUser.ROLE_CUSTOMER)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng"));

        Page<Order> orderPage = orderRepository.findByUserAndCustomerAccountOrderByIdDesc(
                owner,
                customer,
                PageRequest.of(Math.max(page, 0), 10)
        );

        long orderCount = orderRepository.countByUserAndCustomerAccount(owner, customer);
        BigDecimal totalSpent = orderRepository.sumRevenueByUserAndCustomer(owner, customer);

        model.addAttribute("customer", customer);
        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("orderPage", orderPage);
        model.addAttribute("currentPage", orderPage.getNumber());
        model.addAttribute("totalPages", orderPage.getTotalPages());
        model.addAttribute("orderCount", orderCount);
        model.addAttribute("totalSpent", totalSpent == null ? BigDecimal.ZERO : totalSpent);

        return "admin/customer-detail";
    }

    @PostMapping("/{id}/reset-password")
    public String resetCustomerPassword(@PathVariable Long id,
                                        @RequestParam String newPassword,
                                        RedirectAttributes redirectAttributes) {
        authService.requireRole(AppUser.ROLE_OWNER);

        try {
            if (newPassword == null || newPassword.length() < 6) {
                throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự");
            }

            AppUser owner = authService.getWorkspaceOwner();

            AppUser customer = appUserRepository.findByIdAndOwnerAndRole(id, owner, AppUser.ROLE_CUSTOMER)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng"));

            customer.setPassword(passwordEncoder.encode(newPassword));
            appUserRepository.save(customer);

            redirectAttributes.addFlashAttribute("successMessage", "Đã đặt lại mật khẩu khách hàng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/customers/" + id;
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable Long id,
                               RedirectAttributes redirectAttributes) {
        authService.requireRole(AppUser.ROLE_OWNER);

        try {
            AppUser owner = authService.getWorkspaceOwner();

            AppUser customer = appUserRepository.findByIdAndOwnerAndRole(id, owner, AppUser.ROLE_CUSTOMER)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng"));

            customer.setActive(!customer.getActive());
            appUserRepository.save(customer);

            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái khách hàng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/customers/" + id;
    }
}