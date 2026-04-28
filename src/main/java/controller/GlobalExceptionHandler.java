package controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String forbidden(SecurityException ex, Model model, HttpServletRequest request) {
        addErrorAttributes(model, request, 403, "Bạn không có quyền truy cập chức năng này", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String badRequest(IllegalArgumentException ex, Model model, HttpServletRequest request) {
        addErrorAttributes(model, request, 400, "Yêu cầu chưa hợp lệ", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String serverError(Exception ex, Model model, HttpServletRequest request) {
        addErrorAttributes(model, request, 500, "Hệ thống đang gặp lỗi", ex.getMessage());
        return "error";
    }

    private void addErrorAttributes(Model model, HttpServletRequest request, int status, String title, String message) {
        Object path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        model.addAttribute("status", status);
        model.addAttribute("errorTitle", title);
        model.addAttribute("message", message == null || message.isBlank() ? "Vui lòng quay lại và thử lại." : message);
        model.addAttribute("path", path == null ? request.getRequestURI() : path);
    }
}
