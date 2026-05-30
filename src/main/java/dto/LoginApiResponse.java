package dto;

import entity.AppUser;

public class LoginApiResponse {
    private boolean success;
    private String message;
    private Long userId;
    private String username;
    private String fullName;
    private String role;

    public static LoginApiResponse success(AppUser user) {
        LoginApiResponse response = new LoginApiResponse();
        response.setSuccess(true);
        response.setMessage("Đăng nhập thành công");
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole());
        return response;
    }

    public static LoginApiResponse error(String message) {
        LoginApiResponse response = new LoginApiResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
