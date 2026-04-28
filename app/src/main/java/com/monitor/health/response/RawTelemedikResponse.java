package com.monitor.health.response;

public class RawTelemedikResponse {
    private int errorCode;
    private String errorMessage;

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

    @Override
    public String toString() {
        return "RawTelemedikResponse{" +
                "errorCode=" + errorCode +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
