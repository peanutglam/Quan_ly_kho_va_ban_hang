package config;

import entity.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import service.CustomerAccountService;

@ControllerAdvice
public class PublicCustomerModelAttribute {

    private final CustomerAccountService customerAccountService;

    public PublicCustomerModelAttribute(CustomerAccountService customerAccountService) {
        this.customerAccountService = customerAccountService;
    }

    @ModelAttribute("currentCustomer")
    public AppUser currentCustomer(HttpServletRequest request) {
        try {
            return customerAccountService.getCurrentCustomerOrNull(request);
        } catch (Exception e) {
            return null;
        }
    }

    @ModelAttribute("customerLoggedIn")
    public boolean customerLoggedIn(HttpServletRequest request) {
        try {
            return customerAccountService.getCurrentCustomerOrNull(request) != null;
        } catch (Exception e) {
            return false;
        }
    }
}