package controller;

import dto.MobileApiResponse;
import dto.MobileLoginRequest;
import dto.MobileRevenueSummaryDTO;
import dto.MobileUserInfoDTO;
import entity.AppUser;
import entity.Order;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import repository.OrderRepository;
import service.AuthService;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/mobile")
public class MobilePermissionApiController {

    private final AuthService authService;
    private final OrderRepository orderRepository;

    public MobilePermissionApiController(AuthService authService,
                                         OrderRepository orderRepository) {
        this.authService = authService;
        this.orderRepository = orderRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<MobileApiResponse<MobileUserInfoDTO>> login(@RequestBody MobileLoginRequest request) {
        try {
            authService.login(request.getUsername(), request.getPassword());

            AppUser user = authService.getCurrentUser();

            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(MobileApiResponse.fail("Đăng nhập thất bại"));
            }

            MobileUserInfoDTO data = MobileUserInfoDTO.fromUser(user);

            return ResponseEntity.ok(
                    MobileApiResponse.ok("Đăng nhập thành công", data)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MobileApiResponse.fail(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<MobileApiResponse<Void>> logout(HttpServletRequest request,
                                                          HttpServletResponse response) {
        authService.logout(request, response);

        return ResponseEntity.ok(
                MobileApiResponse.ok("Đã đăng xuất", null)
        );
    }

    @GetMapping("/me")
    public ResponseEntity<MobileApiResponse<MobileUserInfoDTO>> me() {
        try {
            AppUser user = authService.getCurrentUser();

            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(MobileApiResponse.fail("Bạn cần đăng nhập"));
            }

            return ResponseEntity.ok(
                    MobileApiResponse.ok("Thông tin người dùng", MobileUserInfoDTO.fromUser(user))
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MobileApiResponse.fail("Bạn cần đăng nhập"));
        }
    }

    @GetMapping("/permission-summary")
    public ResponseEntity<MobileApiResponse<MobileUserInfoDTO>> permissionSummary() {
        return me();
    }

    /*
     * API báo cáo doanh thu chỉ Owner được xem.
     * Nếu STAFF / SALE cố gọi API này, backend trả 403.
     */
    @GetMapping("/reports/revenue-summary")
    public ResponseEntity<MobileApiResponse<MobileRevenueSummaryDTO>> revenueSummary() {
        try {
            AppUser currentUser = authService.getCurrentUser();

            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(MobileApiResponse.fail("Bạn cần đăng nhập"));
            }

            String role = normalizeRole(currentUser.getRole());

            if (!AppUser.ROLE_OWNER.equals(role)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(MobileApiResponse.fail("Bạn không có quyền xem báo cáo doanh thu. Chức năng này chỉ dành cho Owner."));
            }

            AppUser owner = currentUser.getWorkspaceOwner();

            long totalOrders = orderRepository.countByUser(owner);
            long pendingOrders = orderRepository.countByUserAndStatus(owner, Order.STATUS_PENDING);
            long deliveredOrders = orderRepository.countByUserAndStatus(owner, Order.STATUS_DELIVERED);
            long completedOrders = orderRepository.countByUserAndStatus(owner, Order.STATUS_COMPLETED);
            BigDecimal totalRevenue = orderRepository.sumRevenueByUser(owner);

            MobileRevenueSummaryDTO data = new MobileRevenueSummaryDTO(
                    totalOrders,
                    pendingOrders,
                    deliveredOrders,
                    completedOrders,
                    totalRevenue
            );

            return ResponseEntity.ok(
                    MobileApiResponse.ok("Báo cáo doanh thu", data)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(MobileApiResponse.fail("Không thể tải báo cáo: " + e.getMessage()));
        }
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "";
        }

        String value = role.trim().toUpperCase();

        if (value.startsWith("ROLE_")) {
            value = value.substring(5);
        }

        return value;
    }
}