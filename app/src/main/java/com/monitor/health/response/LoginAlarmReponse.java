package com.monitor.health.response;

public class LoginAlarmReponse {
    private int errorCode;
    private String errorMessage;
    private String sessionToken;

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    @Override
    public String toString() {
        return "LoginAlarmReponse{" +
                "errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", sessionToken='" + sessionToken + '\'' +
                '}';
    }
}
