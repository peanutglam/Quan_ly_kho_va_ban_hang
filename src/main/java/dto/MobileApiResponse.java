package dto;

public class MobileApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    public MobileApiResponse() {
    }

    public MobileApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> MobileApiResponse<T> ok(String message, T data) {
        return new MobileApiResponse<>(true, message, data);
    }

    public static <T> MobileApiResponse<T> fail(String message) {
        return new MobileApiResponse<>(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean getSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message == null ? "" : message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}