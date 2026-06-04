package dto;

import entity.AppUser;

public class LoginApiResponse {

    private boolean success;
    private String message;
    private Long userId;
    private String username;
    private String fullName;
    private String role;

    public LoginApiResponse() {
    }

    public static LoginApiResponse success(AppUser user) {
        LoginApiResponse response = new LoginApiResponse();

        response.success = true;
        response.message = "Đăng nhập thành công";

        if (user != null) {
            response.userId = user.getId();
            response.username = user.getUsername();
            response.fullName = user.getFullName();
            response.role = user.getRole();
        }

        return response;
    }

    public static LoginApiResponse fail(String message) {
        LoginApiResponse response = new LoginApiResponse();

        response.success = false;
        response.message = message == null || message.isBlank()
                ? "Đăng nhập thất bại"
                : message;

        return response;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }
}