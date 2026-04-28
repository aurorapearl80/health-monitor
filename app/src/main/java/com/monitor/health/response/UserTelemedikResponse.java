package com.monitor.health.response;

public class UserTelemedikResponse {
    private String status;
    private String message;
    private RawTelemedikResponse rawTelemedikResponse;

    public UserTelemedikResponse() {
    }

    public UserTelemedikResponse(String status, String message, RawTelemedikResponse rawTelemedikResponse) {
        this.status = status;
        this.message = message;
        this.rawTelemedikResponse = rawTelemedikResponse;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public RawTelemedikResponse getRawTelemedikResponse() {
        return rawTelemedikResponse;
    }

    public void setRawTelemedikResponse(RawTelemedikResponse rawTelemedikResponse) {
        this.rawTelemedikResponse = rawTelemedikResponse;
    }

    @Override
    public String toString() {
        return "UserTelemedikResponse{" +
                "status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", rawTelemedikResponse=" + rawTelemedikResponse +
                '}';
    }
}
