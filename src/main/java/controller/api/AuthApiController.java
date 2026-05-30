package controller.api;

import dto.LoginApiRequest;
import dto.LoginApiResponse;
import entity.AppUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthService authService;

    public AuthApiController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginApiResponse> login(@RequestBody LoginApiRequest request) {
        try {
            AppUser user = authService.login(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(LoginApiResponse.success(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(LoginApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<LoginApiResponse> me() {
        try {
            AppUser user = authService.getCurrentUser();
            return ResponseEntity.ok(LoginApiResponse.success(user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(LoginApiResponse.error("Bạn cần đăng nhập"));
        }
    }
}
