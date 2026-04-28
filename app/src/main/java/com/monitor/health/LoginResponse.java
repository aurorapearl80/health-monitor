package com.monitor.health;

public class LoginResponse {


    private String _id;
    private String username;
    private String email;
    private String token;

    public String get_id() {
        return _id;
    }

    public void set_id(String user_id) {
        this._id = user_id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String userName) {
        this.username = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}