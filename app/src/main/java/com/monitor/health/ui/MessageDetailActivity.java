package com.monitor.health.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.VideoView;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import com.monitor.health.R;

public class MessageDetailActivity extends AppCompatActivity {
    private static final String TAG = "MessageDetailActivity";

    private TextView senderName;
    private TextView messageDate;
    private TextView messageBody;
    private VideoView videoView;
    private WebView webView;
    private ScrollView scrollView;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_detail);

        senderName = findViewById(R.id.sender_name);
        messageDate = findViewById(R.id.message_date);
        messageBody = findViewById(R.id.message_body);
        videoView = findViewById(R.id.video_view);
        webView = findViewById(R.id.web_view);
        scrollView = findViewById(R.id.scroll_view);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());

        Intent intent = getIntent();
        String sender = intent.getStringExtra("sender_name");
        String date = intent.getStringExtra("message_date");
        String body = intent.getStringExtra("message_body");

        senderName.setText(sender != null ? sender : "Unknown");
        messageDate.setText(date != null ? date : "");

        if (body != null && isVideoLink(body)) {
            Log.d(TAG, "Video link detected: " + body);
            displayVideo(body);
        } else {
            Log.d(TAG, "Text message");
            displayText(body);
        }
    }

    private boolean isVideoLink(String text) {
        if (text == null) return false;

        String lowerText = text.toLowerCase();

        return lowerText.contains("youtube.com") ||
                lowerText.contains("youtu.be") ||
                lowerText.contains(".mp4") ||
                lowerText.contains(".mkv") ||
                lowerText.contains(".webm") ||
                lowerText.contains(".mov") ||
                lowerText.contains(".avi") ||
                lowerText.contains(".m3u8") ||
                lowerText.contains("vimeo.com") ||
                lowerText.contains("dailymotion.com") ||
                lowerText.contains("drive.google.com");
    }

    private void displayVideo(String text) {
        String videoUrl = extractVideoUrl(text);

        if (videoUrl == null || videoUrl.isEmpty()) {
            showTextFallback(text);
            return;
        }

        if (isYouTubeLink(videoUrl)) {
            Log.d(TAG, "YouTube link detected: " + videoUrl);
            openYouTubeExternally(videoUrl);
        } else if (isDirectVideoLink(videoUrl)) {
            Log.d(TAG, "Direct video link detected: " + videoUrl);
            displayDirectVideo(videoUrl);
        } else {
            // Optional fallback for non-direct/non-youtube URLs
            displayInWebView(videoUrl);
        }
    }

    private boolean isYouTubeLink(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.contains("youtube.com") || lower.contains("youtu.be");
    }

    private boolean isDirectVideoLink(String url) {
        if (url == null) return false;
        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains(".mp4") ||
                lowerUrl.contains(".mkv") ||
                lowerUrl.contains(".webm") ||
                lowerUrl.contains(".mov") ||
                lowerUrl.contains(".avi") ||
                lowerUrl.contains(".m3u8");
    }

    /**
     * Best fix for YouTube: open in external app/browser
     */
    private void openYouTubeExternally(String youtubeUrl) {
        videoView.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);
        scrollView.setVisibility(View.VISIBLE);

        messageBody.setText("Opening YouTube video...");

        try {
            Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl));
            appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(appIntent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "No app found to open YouTube link", e);
            showTextFallback("Unable to open YouTube video.\n\n" + youtubeUrl);
        }
    }

    /**
     * Play direct video files
     */
    private void displayDirectVideo(String videoUrl) {
        videoView.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);
        scrollView.setVisibility(View.GONE);

        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        Uri videoUri = Uri.parse(videoUrl);
        videoView.setVideoURI(videoUri);

        videoView.setOnPreparedListener(mp -> {
            Log.d(TAG, "Video prepared, starting playback");
            videoView.start();
        });

        videoView.setOnCompletionListener(mp ->
                Log.d(TAG, "Video finished playing")
        );

        videoView.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "Video error: " + what + ", " + extra);
            showTextFallback("Failed to play video.\n\n" + videoUrl);
            return true;
        });

        videoView.requestFocus();
    }

    /**
     * Fallback for normal web links
     */
    private void displayInWebView(String url) {
        videoView.setVisibility(View.GONE);
        scrollView.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(url);
    }

    private String extractVideoUrl(String text) {
        if (text == null) return null;

        String[] parts = text.split("\\s+");
        for (String part : parts) {
            String cleaned = cleanUrl(part);
            if ((cleaned.startsWith("http://") || cleaned.startsWith("https://")) && isVideoLink(cleaned)) {
                return cleaned;
            }
        }

        String trimmed = cleanUrl(text.trim());
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }

        return null;
    }

    private String cleanUrl(String url) {
        if (url == null) return "";
        return url.replaceAll("^[\"'(<]+|[\"')>,.]+$", "");
    }

    private void displayText(String text) {
        videoView.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);
        scrollView.setVisibility(View.VISIBLE);
        messageBody.setText(text != null ? text : "No content");
    }

    private void showTextFallback(String text) {
        videoView.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);
        scrollView.setVisibility(View.VISIBLE);
        messageBody.setText(text != null ? text : "Failed to load video");
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}