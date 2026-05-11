package com.monitor.health;

import com.monitor.health.utility.AuthInterceptor;

import org.conscrypt.Conscrypt;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.util.Arrays;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    // Shared OkHttpClient (connection pool, thread pool) â€” never recreate this
    private static OkHttpClient sharedHttpClient;

    private static synchronized OkHttpClient getSharedHttpClient() {
        if (sharedHttpClient == null) {
            HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
            httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);

            ConnectionSpec tlsSpec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)
                    .build();

            sharedHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .addInterceptor(httpLoggingInterceptor)
                    .sslSocketFactory(getSSLSocketFactory(), getTrustManager())
                    .connectionSpecs(Arrays.asList(tlsSpec, ConnectionSpec.CLEARTEXT))
                    .build();
        }
        return sharedHttpClient;
    }

    private static Retrofit getRetrofit(String baseUrl, String authToken, String watchSerial) {
        // Add per-call interceptor without rebuilding the whole client
        OkHttpClient client = getSharedHttpClient().newBuilder()
                .addInterceptor(new AuthInterceptor(authToken, "35647854073153455"))
                .build();

        return new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(baseUrl)
                .client(client)
                .build();
    }

    public static UserService getUserService(String baseUrl, String authToken, String watchSerial) {
        return getRetrofit(baseUrl, authToken, "35647854073153455").create(UserService.class);
    }

    private static SSLSocketFactory getSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS", Conscrypt.newProvider());
            sslContext.init(null, new TrustManager[]{getTrustManager()}, null);
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create SSL socket factory", e);
        }
    }

    private static X509TrustManager getTrustManager() {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }
}
