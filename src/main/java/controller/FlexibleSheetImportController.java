package controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import service.FlexibleSheetImportService;
import service.AuthService;

import java.util.List;

@Controller
@RequestMapping("/sheet")
public class FlexibleSheetImportController {

    private final FlexibleSheetImportService service;
    private final AuthService authService;

    public FlexibleSheetImportController(FlexibleSheetImportService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    @GetMapping("/flex-import")
    public String showImportPage() {
        authService.requireRole("OWNER", "STAFF");
        return "sheet/import";
    }

    @PostMapping("/read")
    public String readSheet(@RequestParam String sheetUrl,
                            @RequestParam String gid,
                            @RequestParam String type,
                            Model model) {
        authService.requireRole("OWNER", "STAFF");
        try {
            List<String> headers = service.readHeaders(sheetUrl, gid);

            model.addAttribute("sheetUrl", sheetUrl);
            model.addAttribute("gid", gid);
            model.addAttribute("type", type);
            model.addAttribute("headers", headers);

            if ("PRODUCT".equals(type)) {
                return "sheet/map-product";
            }

            return "sheet/map-order";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Không đọc được sheet: " + e.getMessage());
            return "sheet/import";
        }
    }

    @PostMapping("/import-product")
    public String importProduct(@RequestParam String sheetUrl,
                                @RequestParam String gid,
                                @RequestParam(required = false, defaultValue = "") String codeColumn,
                                @RequestParam String nameColumn,
                                @RequestParam(required = false, defaultValue = "") String quantityColumn,
                                @RequestParam(required = false, defaultValue = "") String totalQuantityColumn,
                                @RequestParam(required = false, defaultValue = "") String soldQuantityColumn,
                                @RequestParam(required = false, defaultValue = "") String importPriceColumn,
                                @RequestParam(required = false, defaultValue = "") String salePriceColumn,
                                @RequestParam(required = false, defaultValue = "") String supplierColumn,
                                @RequestParam(required = false, defaultValue = "") String expiryDateColumn,
                                Model model) {
        authService.requireRole("OWNER", "STAFF");
        try {
            int count = service.importProducts(
                    sheetUrl,
                    gid,
                    codeColumn,
                    nameColumn,
                    quantityColumn,
                    totalQuantityColumn,
                    soldQuantityColumn,
                    importPriceColumn,
                    salePriceColumn,
                    supplierColumn,
                    expiryDateColumn
            );

            model.addAttribute("successMessage", "Đã import " + count + " sản phẩm.");
            return "sheet/import";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Import sản phẩm thất bại: " + e.getMessage());
            return "sheet/import";
        }
    }

    @PostMapping("/import-order")
    public String importOrder(@RequestParam String sheetUrl,
                              @RequestParam String gid,
                              @RequestParam(required = false, defaultValue = "") String orderCodeColumn,
                              @RequestParam String customerNameColumn,
                              @RequestParam(required = false, defaultValue = "") String phoneColumn,
                              @RequestParam(required = false, defaultValue = "") String addressColumn,
                              @RequestParam String productNameColumn,
                              @RequestParam(required = false, defaultValue = "") String quantityColumn,
                              @RequestParam(required = false, defaultValue = "") String shippingFeeColumn,
                              @RequestParam(required = false, defaultValue = "") String totalBillColumn,
                              @RequestParam(required = false, defaultValue = "") String customerDepositColumn,
                              @RequestParam(required = false, defaultValue = "") String statusColumn,
                              Model model) {
        authService.requireRole("OWNER", "STAFF");
        try {
            int count = service.importOrders(
                    sheetUrl,
                    gid,
                    orderCodeColumn,
                    customerNameColumn,
                    phoneColumn,
                    addressColumn,
                    productNameColumn,
                    quantityColumn,
                    shippingFeeColumn,
                    totalBillColumn,
                    customerDepositColumn,
                    statusColumn
            );

            model.addAttribute("successMessage", "Đã import " + count + " đơn hàng.");
            return "sheet/import";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Import đơn hàng thất bại: " + e.getMessage());
            return "sheet/import";
        }
    }
}