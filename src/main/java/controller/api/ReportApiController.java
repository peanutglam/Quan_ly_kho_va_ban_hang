package controller.api;

import dto.DailyReportDTO;
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

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final OrderService orderService;
    private final AuthService authService;

    public ReportApiController(OrderService orderService,
                               AuthService authService) {
        this.orderService = orderService;
        this.authService = authService;
    }

    @GetMapping("/daily")
    public ResponseEntity<?> dailyReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        try {
            authService.getCurrentUser();

            LocalDate reportDate = date == null
                    ? LocalDate.now(VIETNAM_ZONE)
                    : date;

            DailyReportDTO report = orderService.getDailyReport(reportDate);

            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", message == null || message.isBlank() ? "Có lỗi xảy ra" : message);
        return body;
    }
}