package controller;

import dto.DailyReportDTO;
import entity.AppUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import service.AuthService;
import service.OrderService;
import dto.DailyTrendDTO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

/**
 * Nhiệm vụ 3: Báo cáo cuối ngày.
 * Chỉ Owner/Admin truy cập được.
 */
@Controller
@RequestMapping("/reports")
public class ReportController {

    private final OrderService orderService;
    private final AuthService authService;

    public ReportController(OrderService orderService, AuthService authService) {
        this.orderService = orderService;
        this.authService = authService;
    }

    @GetMapping("/daily")
    public String dailyReport(@RequestParam(value = "date", required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                              Model model) {
        authService.requireRole("OWNER", "STAFF");

        if (date == null) date = LocalDate.now();

        DailyReportDTO report = orderService.getDailyReport(date);
        List<DailyTrendDTO> trendData = orderService.getLast31DaysTrend(date);

        List<String> trendLabels = new ArrayList<>();
        List<Long> trendOrderCounts = new ArrayList<>();
        List<BigDecimal> trendRevenues = new ArrayList<>();

        for (DailyTrendDTO item : trendData) {
            trendLabels.add(item.getDate().getDayOfMonth() + "/" + item.getDate().getMonthValue());
            trendOrderCounts.add(item.getOrderCount());
            trendRevenues.add(item.getRevenue());
        }

        model.addAttribute("trendLabels", trendLabels);
        model.addAttribute("trendOrderCounts", trendOrderCounts);
        model.addAttribute("trendRevenues", trendRevenues);
        model.addAttribute("report", report);
        model.addAttribute("selectedDate", date);
        model.addAttribute("today", LocalDate.now());
        return "reports/daily";
    }
}
