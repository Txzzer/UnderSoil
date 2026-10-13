package network;

public class AuthResponse {

    private String token;
    private String userId;
    private String name;
    private String expiresAt;

    public String getToken() {
        return token;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getExpiresAt() {
        return expiresAt;
    }
}
