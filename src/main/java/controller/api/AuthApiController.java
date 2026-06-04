package controller.api;

import dto.LoginApiRequest;
import dto.LoginApiResponse;
import entity.AppUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.AuthService;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthService authService;

    public AuthApiController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginApiRequest request) {
        try {
            authService.login(request.getUsername(), request.getPassword());
            AppUser currentUser = authService.getCurrentUser();

            return ResponseEntity.ok(LoginApiResponse.success(currentUser));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(LoginApiResponse.fail(e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        try {
            AppUser user = authService.getCurrentUser();

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", true);
            body.put("userId", user.getId());
            body.put("username", user.getUsername());
            body.put("fullName", user.getFullName());
            body.put("role", user.getRole());

            return ResponseEntity.ok(body);
        } catch (Exception e) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", false);
            body.put("message", "Chưa đăng nhập");

            return ResponseEntity.status(401).body(body);
        }
    }
}