package controller;

import dto.DailyReportDTO;
import dto.DailyTrendDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import service.OrderService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Controller
public class ReportController {

    private final OrderService orderService;

    public ReportController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/reports/daily")
    public String dailyReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate chartStartDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate chartEndDate,

            Model model
    ) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));

        LocalDate selectedDate = date == null ? today : date;

        // Phần trên của báo cáo vẫn lấy theo 1 ngày.
        DailyReportDTO report = orderService.getDailyReport(selectedDate);

        // Phần biểu đồ mới lấy theo khoảng thời gian riêng.
        LocalDate safeChartEndDate = chartEndDate == null ? selectedDate : chartEndDate;
        LocalDate safeChartStartDate = chartStartDate == null ? safeChartEndDate.minusDays(30) : chartStartDate;

        if (safeChartStartDate.isAfter(safeChartEndDate)) {
            LocalDate temp = safeChartStartDate;
            safeChartStartDate = safeChartEndDate;
            safeChartEndDate = temp;
        }

        List<DailyTrendDTO> trendData = orderService.getTrendByRange(safeChartStartDate, safeChartEndDate);

        List<String> trendLabels = new ArrayList<>();
        List<Long> trendOrderCounts = new ArrayList<>();
        List<BigDecimal> trendRevenues = new ArrayList<>();

        for (DailyTrendDTO item : trendData) {
            trendLabels.add(item.getDate().getDayOfMonth() + "/" + item.getDate().getMonthValue());
            trendOrderCounts.add(item.getOrderCount());
            trendRevenues.add(item.getRevenue());
        }

        model.addAttribute("today", today);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("report", report);

        model.addAttribute("chartStartDate", safeChartStartDate);
        model.addAttribute("chartEndDate", safeChartEndDate);

        model.addAttribute("trendLabels", trendLabels);
        model.addAttribute("trendOrderCounts", trendOrderCounts);
        model.addAttribute("trendRevenues", trendRevenues);

        return "reports/daily";
    }
}