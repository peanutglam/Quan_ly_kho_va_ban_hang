package controller.api;

import dto.DailyReportDTO;
import entity.AppUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.AuthService;
import service.OrderService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportApiController {

    private final OrderService orderService;
    private final AuthService authService;

    public ReportApiController(OrderService orderService, AuthService authService) {
        this.orderService = orderService;
        this.authService = authService;
    }

    @GetMapping("/daily")
    public ResponseEntity<?> dailyReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        Map<String, Object> body = new LinkedHashMap<>();

        try {
            AppUser currentUser = authService.getCurrentUser();

            if (currentUser == null) {
                body.put("success", false);
                body.put("message", "Bạn cần đăng nhập");
                return ResponseEntity.status(401).body(body);
            }

            LocalDate reportDate = date == null
                    ? LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                    : date;

            DailyReportDTO report = orderService.getDailyReport(reportDate);

            return ResponseEntity.ok(report);
        } catch (Exception e) {
            body.put("success", false);
            body.put("message", e.getMessage() == null ? "Không tải được báo cáo ngày" : e.getMessage());
            return ResponseEntity.badRequest().body(body);
        }
    }
}