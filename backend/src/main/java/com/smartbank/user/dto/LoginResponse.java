package com.smartbank.user.dto;

public class LoginResponse {

    private String message;
    private String token;
    private String tokenType;

    public LoginResponse() {
    }

    public LoginResponse(String message, String token, String tokenType) {
        this.message = message;
        this.token = token;
        this.tokenType = tokenType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
}
