package com.monitor.health.utility;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;

import com.monitor.health.ApiClient;
import com.monitor.health.Constant;
import com.monitor.health.dao.FileResponseDto;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ApkDownloadManager {
    private static final String TAG = "ApkDownloadManager";
    private static final int DOWNLOAD_TIMEOUT_MS = 30000; // 30 seconds
    private static final int CONNECTION_TIMEOUT_MS = 10000; // 10 seconds
    private static final int READ_TIMEOUT_MS = 15000; // 15 seconds
    private static final long MIN_DOWNLOAD_SPEED_BYTES_PER_SEC = 1024; // 1 KB/s minimum

    // SharedPreferences keys
    private static final String PREF_DOWNLOAD_PERCENT = "download_percent";
    private static final String PREF_IS_DOWNLOADING = "is_downloading";
    private static final String PREF_DOWNLOAD_START_TIME = "download_start_time";

    private Context context;
    private ProgressCallback progressCallback;
    private InstallationCallback installationCallback;
    private static volatile boolean isDownloading = false;

    public interface ProgressCallback {
        void onProgress(int progress, long downloadedBytes, long totalBytes, long speed);
        void onSlowConnection(long currentSpeed);
    }

    public interface InstallationCallback {
        void onInstallationStarted();
        void onInstallationSuccess();
        void onInstallationFailed(String error);
        void onInstallationCancelled();
    }

    public ApkDownloadManager(Context context) {
        this.context = context;
    }

    public void setProgressCallback(ProgressCallback callback) {
        this.progressCallback = callback;
    }

    public void setInstallationCallback(InstallationCallback callback) {
        this.installationCallback = callback;
    }

    /**
     * Check if download is currently in progress
     */
    public boolean isDownloading() {
        SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        boolean downloading = prefs.getBoolean(PREF_IS_DOWNLOADING, false);

        // Double check with static variable
        if (downloading && !isDownloading) {
            // Clean up stale state (maybe app was killed during download)
            long startTime = prefs.getLong(PREF_DOWNLOAD_START_TIME, 0);
            long currentTime = System.currentTimeMillis();

            // If download flag is set for more than 10 minutes, assume it's stale
            if (currentTime - startTime > 600000) {
                Log.w(TAG, "Clearing stale download state");
                prefs.edit().putBoolean(PREF_IS_DOWNLOADING, false).apply();
                return false;
            }
        }

        return downloading || isDownloading;
    }

    /**
     * Check if file exists and download is complete
     */
    private boolean isFileDownloadComplete(String filename) {
        SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        int downloadPercent = prefs.getInt(PREF_DOWNLOAD_PERCENT + "_" + filename, 0);

        File apkFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), filename);
        boolean fileExists = apkFile.exists();

        Log.d(TAG, "File exists: " + fileExists + ", Download percent: " + downloadPercent + "%");

        return fileExists && downloadPercent == 100;
    }

    /**
     * Save download progress
     */
    private void saveDownloadProgress(String filename, int percent) {
        SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        prefs.edit().putInt(PREF_DOWNLOAD_PERCENT + "_" + filename, percent).apply();
    }

    /**
     * Mark download as started
     */
    private void markDownloadStarted() {
        SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(PREF_IS_DOWNLOADING, true)
                .putLong(PREF_DOWNLOAD_START_TIME, System.currentTimeMillis())
                .apply();
        isDownloading = true;
        Log.d(TAG, "Download marked as started");
    }

    /**
     * Mark download as completed
     */
    private void markDownloadCompleted() {
        SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(PREF_IS_DOWNLOADING, false)
                .remove(PREF_DOWNLOAD_START_TIME)
                .apply();
        isDownloading = false;
        Log.d(TAG, "Download marked as completed");
    }

    public void checkVersionDownload() {
        String testAndroidId = "doctorwatchserialtest";
        Log.d(TAG, "Android: " + testAndroidId);

        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);

        Log.d(TAG, "Month: " + month + ", Year: " + year);

        // Check if already downloading
        if (isDownloading()) {
            Log.w(TAG, "Download already in progress. Skipping...");
            return;
        }

        // Check if we have a pending installation
        SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String pendingInstall = prefs.getString("pending_install_filename", null);
        if (pendingInstall != null) {
            Log.d(TAG, "Pending installation detected: " + pendingInstall);
            checkApkInstallationStatus(pendingInstall);
        }

        Call<FileResponseDto> call = ApiClient.getUserService(Constant.BASE_URL_BGM, Constant.TOKEN_DR_WATCH_API, testAndroidId).getFileDownload();
        call.enqueue(new Callback<FileResponseDto>() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onResponse(Call<FileResponseDto> call, Response<FileResponseDto> response) {
                Log.d(TAG, "File download response: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    String filename = response.body().getData().getFilename();
                    String downloadUrl = response.body().getData().getDownload_url();

                    Log.d(TAG, "Server filename: " + filename);
                    Log.d(TAG, "Download URL: " + downloadUrl);

                    SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                    String savedFilename = prefs.getString("saved_filename", null);
                    String installedVersion = prefs.getString("installed_version", null);


                    Log.d(TAG, "Saved filename: " + savedFilename);
                    Log.d(TAG, "Installed version: " + installedVersion);

                    // Check download conditions
                    boolean fileDownloadComplete = isFileDownloadComplete(filename);
                    boolean isNewVersion = !filename.equals(savedFilename);
                    boolean alreadyDownloading = isDownloading();

                    Log.d(TAG, "Download conditions - File complete: " + fileDownloadComplete +
                            ", New version: " + isNewVersion +
                            ", Already downloading: " + alreadyDownloading);
                    // Extract like "1024"
                    String rawVersion = filename.replaceAll(".*_v(\\d+)\\.apk", "$1");

// Convert "1024" â†’ "v1.0.24"
                    String versionDisplay = "v0.0.0";  // fallback
                    if (rawVersion.length() == 4) {
                        String major = rawVersion.substring(0, 1);
                        String minor = rawVersion.substring(1, 2);
                        String patch = rawVersion.substring(2, 4);
                        versionDisplay = "v" + major + "." + minor + "." + patch;
                    }

                    String appVersionText = "Version: " + versionDisplay;

                    Log.d(TAG, "Server version: " + appVersionText + ", Current app: " + Constant.APP_VERSION);

// Only proceed if server version != current app version
                    if (!Constant.APP_VERSION.equals(appVersionText)) {

                        Log.d(TAG, "New version detected, checking download conditionsâ€¦");

                        // Download logic:
                        // 1. If file doesn't exist OR download percent != 100, download
                        // 2. If new version detected (different filename), download
                        // 3. Only download if NOT currently downloading

                        if (!alreadyDownloading && (!fileDownloadComplete || isNewVersion)) {
                            Log.d(TAG, "Starting download - Reason: " +
                                    (isNewVersion ? "New version" : "Incomplete download"));

                            // Save the new filename
                            prefs.edit().putString("saved_filename", filename).apply();

                            // Start download with connection monitoring
                            downloadAndInstallApkWithMonitoring(downloadUrl, filename);

                        } else if (alreadyDownloading) {
                            Log.w(TAG, "Download already in progress. Skipping...");

                        } else if (fileDownloadComplete && !isNewVersion) {
                            Log.d(TAG, "File already downloaded completely and up to date. No action needed.");
                        }

                    } else {
                        Log.d(TAG, "App is already on latest version, no download needed.");
                    }

                } else {
                    Log.e(TAG, "Response not successful: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<FileResponseDto> call, Throwable t) {
                Log.e(TAG, "API call failed: " + t.toString());
                handleApiFailure(t);
            }
        });
    }

    private void downloadAndInstallApkWithMonitoring(String downloadUrl, String filename) {
        // Mark download as started
        markDownloadStarted();

        new Thread(() -> {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;

            try {
                URL url = new URL(downloadUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new IOException("Server returned HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage());
                }

                int fileLength = connection.getContentLength();
                Log.d(TAG, "File size: " + fileLength + " bytes");

                // Reset download progress
                saveDownloadProgress(filename, 0);

                // Create APK file in internal storage
                File apkFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), filename);
                input = new BufferedInputStream(connection.getInputStream(), 8192);
                output = new FileOutputStream(apkFile);

                byte[] data = new byte[4096];
                final long[] total = {0};
                int count;
                long startTime = System.currentTimeMillis();
                long[] lastSpeedCheck = {startTime};
                long[] lastBytes = {0};

                while ((count = input.read(data)) != -1) {
                    total[0] += count;
                    output.write(data, 0, count);

                    // Calculate download speed every second
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastSpeedCheck[0] >= 1000) {
                        long timeDiff = currentTime - lastSpeedCheck[0];
                        long bytesDiff = total[0] - lastBytes[0];
                        final long currentSpeed = (bytesDiff * 1000) / timeDiff;

                        if (currentSpeed < MIN_DOWNLOAD_SPEED_BYTES_PER_SEC) {
                            Log.w(TAG, "Slow connection detected: " + currentSpeed + " bytes/sec");
                            if (progressCallback != null) {
                                new Handler(Looper.getMainLooper()).post(() ->
                                        progressCallback.onSlowConnection(currentSpeed));
                            }
                        }

                        // Update progress
                        if (fileLength > 0) {
                            final int progress = (int) (total[0] * 100 / fileLength);
                            final long downloadedBytes = total[0];
                            final long totalBytes = fileLength;

                            // Save progress to SharedPreferences
                            saveDownloadProgress(filename, progress);

                            if (progressCallback != null) {
                                new Handler(Looper.getMainLooper()).post(() ->
                                        progressCallback.onProgress(progress, downloadedBytes, totalBytes, currentSpeed));
                            }
                        }

                        lastSpeedCheck[0] = currentTime;
                        lastBytes[0] = total[0];

                        Log.d(TAG, "Download progress: " + (fileLength > 0 ? (total[0] * 100 / fileLength) : 0) +
                                "%, Speed: " + formatBytes(currentSpeed) + "/s");
                    }
                }

                Log.d(TAG, "Download completed successfully");

                // Mark download as 100% complete
                saveDownloadProgress(filename, 100);

                // Mark as pending installation
                SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                prefs.edit().putString("pending_install_filename", filename).apply();

                // Install APK
                new Handler(Looper.getMainLooper()).post(() -> installApk(apkFile, filename));

            } catch (Exception e) {
                Log.e(TAG, "Download failed: " + e.getMessage());
                e.printStackTrace();

                // Save incomplete download state
                SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                int currentPercent = prefs.getInt(PREF_DOWNLOAD_PERCENT + "_" + filename, 0);
                Log.e(TAG, "Download interrupted at " + currentPercent + "%");

                if (e instanceof SocketTimeoutException) {
                    Log.e(TAG, "Download timeout - connection too slow");
                } else if (e instanceof UnknownHostException) {
                    Log.e(TAG, "Network unavailable");
                } else if (e instanceof IOException) {
                    Log.e(TAG, "IO error during download");
                }

            } finally {
                // Mark download as completed (whether success or failure)
                markDownloadCompleted();

                try {
                    if (output != null) output.close();
                    if (input != null) input.close();
                } catch (IOException ignored) {}

                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private void installApk(File apkFile, String filename) {
        try {
            if (installationCallback != null) {
                installationCallback.onInstallationStarted();
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri apkUri;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                String authority = context.getPackageName() + ".fileprovider";
                apkUri = FileProvider.getUriForFile(context, authority, apkFile);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                apkUri = Uri.fromFile(apkFile);
            }

            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            registerInstallationReceiver(filename);

            context.startActivity(intent);
            Log.d(TAG, "Installation intent started");

        } catch (Exception e) {
            Log.e(TAG, "Failed to start APK installation: " + e.getMessage());
            if (installationCallback != null) {
                installationCallback.onInstallationFailed(e.getMessage());
            }
        }
    }

    private void registerInstallationReceiver(String filename) {
        BroadcastReceiver installReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(TAG, "Installation broadcast received: " + action);

                switch (action) {
                    case Intent.ACTION_PACKAGE_ADDED:
                    case Intent.ACTION_PACKAGE_REPLACED:
                        String packageName = intent.getData().getSchemeSpecificPart();
                        Log.d(TAG, "Package installed/updated: " + packageName);

                        SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                        prefs.edit()
                                .putString("installed_version", filename)
                                .remove("pending_install_filename")
                                .putLong("installation_time", System.currentTimeMillis())
                                .apply();

                        if (installationCallback != null) {
                            installationCallback.onInstallationSuccess();
                        }

                        context.unregisterReceiver(this);
                        break;

                    case Intent.ACTION_PACKAGE_REMOVED:
                        Log.d(TAG, "Package removed");
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");

        context.registerReceiver(installReceiver, filter);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                context.unregisterReceiver(installReceiver);
                Log.d(TAG, "Installation receiver unregistered due to timeout");
                checkApkInstallationStatus(filename);
            } catch (IllegalArgumentException e) {
                // Receiver already unregistered
            }
        }, 300000); // 5 minutes timeout
    }

    private void checkApkInstallationStatus(String filename) {
        SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String installedVersion = prefs.getString("installed_version", null);
        String pendingInstall = prefs.getString("pending_install_filename", null);

        Log.d(TAG, "Checking installation status - Filename: " + filename +
                ", Installed: " + installedVersion + ", Pending: " + pendingInstall);

        if (filename.equals(installedVersion)) {
            Log.d(TAG, "APK installation confirmed");
            if (installationCallback != null) {
                installationCallback.onInstallationSuccess();
            }
        } else if (filename.equals(pendingInstall)) {
            Log.d(TAG, "Installation still pending or failed");
            if (installationCallback != null) {
                installationCallback.onInstallationFailed("Installation timeout or user cancelled");
            }
            prefs.edit().remove("pending_install_filename").apply();
        }
    }

    private void handleApiFailure(Throwable t) {
        if (t instanceof SocketTimeoutException || t instanceof ConnectException) {
            Log.e(TAG, "Network connection issues detected");
        } else if (t instanceof UnknownHostException) {
            Log.e(TAG, "DNS resolution failed - check internet connection");
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Clear download state for a specific file (useful for retry or reset)
     */
    public void clearDownloadState(String filename) {
        SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        prefs.edit()
                .remove(PREF_DOWNLOAD_PERCENT + "_" + filename)
                .remove("pending_install_filename")
                .apply();
        Log.d(TAG, "Download state cleared for: " + filename);
    }

    public void setupCallbacks() {
        setProgressCallback(new ProgressCallback() {
            @Override
            public void onProgress(int progress, long downloadedBytes, long totalBytes, long speed) {
                Log.d(TAG, "Download: " + progress + "% (" + formatBytes(downloadedBytes) +
                        "/" + formatBytes(totalBytes) + ") at " + formatBytes(speed) + "/s");
            }

            @Override
            public void onSlowConnection(long currentSpeed) {
                Log.w(TAG, "Slow connection warning: " + formatBytes(currentSpeed) + "/s");
            }
        });

        setInstallationCallback(new InstallationCallback() {
            @Override
            public void onInstallationStarted() {
                Log.d(TAG, "APK installation started");
            }

            @Override
            public void onInstallationSuccess() {
                Log.d(TAG, "APK installation successful");
            }

            @Override
            public void onInstallationFailed(String error) {
                Log.e(TAG, "APK installation failed: " + error);
            }

            @Override
            public void onInstallationCancelled() {
                Log.d(TAG, "APK installation cancelled by user");
            }
        });
    }
}