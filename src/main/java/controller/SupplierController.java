package controller;

import entity.Supplier;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import service.AuthService;
import service.SupplierService;

@Controller
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierService supplierService;
    private final AuthService authService;

    public SupplierController(SupplierService supplierService, AuthService authService) {
        this.supplierService = supplierService;
        this.authService = authService;
    }

    @GetMapping
    public String listSuppliers(Model model) {
        model.addAttribute("suppliers", supplierService.getAllSuppliers());
        return "suppliers/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        authService.requireRole("OWNER", "STAFF");
        model.addAttribute("supplier", new Supplier());
        model.addAttribute("pageTitle", "Thêm nhà cung cấp");
        model.addAttribute("formAction", "/suppliers/create");
        return "suppliers/form";
    }

    @PostMapping("/create")
    public String createSupplier(@Valid @ModelAttribute("supplier") Supplier supplier,
                                 BindingResult bindingResult,
                                 Model model) {
        authService.requireRole("OWNER", "STAFF");
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Thêm nhà cung cấp");
            model.addAttribute("formAction", "/suppliers/create");
            return "suppliers/form";
        }
        supplierService.saveSupplier(supplier);
        return "redirect:/suppliers";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        authService.requireRole("OWNER", "STAFF");
        model.addAttribute("supplier", supplierService.getSupplierById(id));
        model.addAttribute("pageTitle", "Cập nhật nhà cung cấp");
        model.addAttribute("formAction", "/suppliers/edit/" + id);
        return "suppliers/form";
    }

    @PostMapping("/edit/{id}")
    public String updateSupplier(@PathVariable Long id,
                                 @Valid @ModelAttribute("supplier") Supplier supplier,
                                 BindingResult bindingResult,
                                 Model model) {
        authService.requireRole("OWNER", "STAFF");
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Cập nhật nhà cung cấp");
            model.addAttribute("formAction", "/suppliers/edit/" + id);
            return "suppliers/form";
        }
        Supplier existing = supplierService.getSupplierById(id);
        existing.setName(supplier.getName());
        existing.setPhone(supplier.getPhone());
        existing.setEmail(supplier.getEmail());
        existing.setAddress(supplier.getAddress());
        existing.setNote(supplier.getNote());
        supplierService.saveSupplier(existing);
        return "redirect:/suppliers";
    }

    @GetMapping("/delete/{id}")
    public String deleteSupplier(@PathVariable Long id) {
        authService.requireRole("OWNER", "STAFF");
        supplierService.deleteSupplier(id);
        return "redirect:/suppliers";
    }
}
