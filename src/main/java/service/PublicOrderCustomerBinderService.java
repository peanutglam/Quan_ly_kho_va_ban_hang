package service;

import entity.AppUser;
import entity.Order;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.OrderRepository;

@Service
public class PublicOrderCustomerBinderService {

    private final CustomerAccountService customerAccountService;
    private final OrderRepository orderRepository;

    public PublicOrderCustomerBinderService(CustomerAccountService customerAccountService,
                                            OrderRepository orderRepository) {
        this.customerAccountService = customerAccountService;
        this.orderRepository = orderRepository;
    }

    /*
     * Gắn đơn hàng public với tài khoản khách đang đăng nhập.
     *
     * Nếu khách chưa đăng nhập:
     * - Không làm gì.
     * - Đơn vẫn được tạo bình thường.
     *
     * Nếu khách đã đăng nhập:
     * - order.customerAccount = customer
     * - Nếu tên / SĐT / địa chỉ trong đơn bị trống thì tự lấy từ tài khoản khách.
     */
    @Transactional
    public Order bindCustomerToOrder(HttpServletRequest request, Order order) {
        if (request == null || order == null) {
            return order;
        }

        AppUser customer = customerAccountService.getCurrentCustomerOrNull(request);

        if (customer == null) {
            return order;
        }

        order.setCustomerAccount(customer);

        if (isBlank(order.getCustomerName())) {
            order.setCustomerName(customer.getFullName());
        }

        if (isBlank(order.getCustomerPhone())) {
            order.setCustomerPhone(customer.getPhone());
        }

        if (isBlank(order.getCustomerAddress())) {
            order.setCustomerAddress(customer.getAddress());
        }

        return orderRepository.save(order);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}