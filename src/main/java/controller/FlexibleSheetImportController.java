package controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import service.FlexibleSheetImportService;

import java.util.List;

@Controller
@RequestMapping("/sheet")
public class FlexibleSheetImportController {

    private final FlexibleSheetImportService service;

    public FlexibleSheetImportController(FlexibleSheetImportService service) {
        this.service = service;
    }

    @GetMapping("/flex-import")
    public String showImportPage() {
        return "sheet/import";
    }

    @PostMapping("/read")
    public String readSheet(@RequestParam String sheetUrl,
                            @RequestParam String gid,
                            @RequestParam String type,
                            Model model) {
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
                                @RequestParam String codeColumn,
                                @RequestParam String nameColumn,
                                @RequestParam String quantityColumn,
                                @RequestParam String importPriceColumn,
                                @RequestParam String salePriceColumn,
                                @RequestParam String supplierColumn,
                                @RequestParam String expiryDateColumn,
                                Model model) {
        try {
            int count = service.importProducts(
                    sheetUrl,
                    gid,
                    codeColumn,
                    nameColumn,
                    quantityColumn,
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
                              @RequestParam String orderCodeColumn,
                              @RequestParam String customerNameColumn,
                              @RequestParam String phoneColumn,
                              @RequestParam String addressColumn,
                              @RequestParam String productNameColumn,
                              @RequestParam String quantityColumn,
                              @RequestParam String statusColumn,
                              Model model) {
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