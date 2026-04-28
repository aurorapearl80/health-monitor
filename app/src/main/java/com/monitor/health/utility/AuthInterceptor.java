package com.monitor.health.utility;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    private String authToken;
    private String watchSerial;

    public AuthInterceptor(String authToken, String watchSerial) {
        this.authToken = authToken;
        this.watchSerial = watchSerial;

    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request modifiedRequest = originalRequest.newBuilder()
                .header("api-key",this.authToken)
                .header("watch-serial",this.watchSerial)
                .header("Firmware-Version", Build.DISPLAY)
                .header("App-Version", Build.VERSION.RELEASE)
                .header("Android-Version", String.valueOf(Build.VERSION.SDK_INT))
                .header("Content-Type","application/json")
                .header("Accept", "application/json")
                .build();
        return chain.proceed(modifiedRequest);
    }
}
