package com.monitor.health.utility;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;

import com.monitor.health.R;

public class SmartWatchAlertDialog extends Dialog {

    private static final String TAG = "SmartWatchAlertDialog";

    public interface DialogListener {
        void onOkClicked();
        void onCancelClicked();
    }

    private DialogListener listener;
    private String title;
    private String message;
    private int iconResource;
    private String okButtonText = "OK";
    private String cancelButtonText = "Cancel";
    private boolean showIcon = true;

    public SmartWatchAlertDialog(Context context) {
        super(context);
    }

    public SmartWatchAlertDialog(Context context, String title, String message, int iconResource) {
        super(context);
        this.title = title;
        this.message = message;
        this.iconResource = iconResource;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove default title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_smartwatch_alert);

        // Set dialog properties for smartwatch
        setupDialogWindow();

        // Initialize views
        initializeViews();
    }

    @Override
    public void show() {
        try {
            if (isContextValid()) {
                super.show();
            } else {
                Log.w(TAG, "Cannot show dialog: context is not valid");
            }
        } catch (WindowManager.BadTokenException e) {
            Log.w(TAG, "Cannot show dialog: BadTokenException", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while showing dialog", e);
        }
    }

    private boolean isContextValid() {
        Context context = getContext();
        if (context == null) {
            return false;
        }

        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            return !activity.isFinishing() && !activity.isDestroyed();
        }

        return true; // For non-Activity contexts (like Application context)
    }

    private void setupDialogWindow() {
        Window window = getWindow();
        if (window != null) {
            // Make dialog background transparent
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // Set dialog size for smartwatch (smaller screen)
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.9);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);

            // Add dim background
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.7f);
        }
    }

    private void initializeViews() {
        //ImageView iconView = findViewById(R.id.dialog_icon);
        //TextView titleView = findViewById(R.id.dialog_title);
        TextView messageView = findViewById(R.id.dialog_message);
//        Button okButton = findViewById(R.id.btn_ok);
//        Button cancelButton = findViewById(R.id.btn_cancel);
        LinearLayout okButton = findViewById(R.id.btn_ok);      // Emergency Call button
        LinearLayout cancelButton = findViewById(R.id.btn_cancel); // I'm OK button

        // Set icon
//        if (showIcon && iconResource != 0) {
//            iconView.setImageResource(iconResource);
//            iconView.setVisibility(View.VISIBLE);
//        } else {
//            iconView.setVisibility(View.GONE);
//        }

        // Set title
//        if (title != null && !title.isEmpty()) {
//            titleView.setText(title);
//            titleView.setVisibility(View.VISIBLE);
//        } else {
//            titleView.setVisibility(View.GONE);
//        }

        // Set message
        if (message != null && !message.isEmpty()) {
            messageView.setText(message);
            messageView.setVisibility(View.VISIBLE);
        } else {
            messageView.setVisibility(View.GONE);
        }

        // Set button texts
        //okButton.setText(okButtonText);
        //cancelButton.setText(cancelButtonText);

        // Set button click listeners
        okButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOkClicked();
            }
            dismiss();
        });

        cancelButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCancelClicked();
            }
            dismiss();
        });
    }

    // Builder pattern for easy dialog creation
    public static class Builder {
        private Context context;
        private String title;
        private String message;
        private int iconResource;
        private String okButtonText = "OK";
        private String cancelButtonText = "Cancel";
        private boolean showIcon = true;
        private DialogListener listener;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder setIcon(int iconResource) {
            this.iconResource = iconResource;
            this.showIcon = true;
            return this;
        }

        public Builder hideIcon() {
            this.showIcon = false;
            return this;
        }

        public Builder setOkButtonText(String text) {
            this.okButtonText = text;
            return this;
        }

        public Builder setCancelButtonText(String text) {
            this.cancelButtonText = text;
            return this;
        }

        public Builder setDialogListener(DialogListener listener) {
            this.listener = listener;
            return this;
        }

        public SmartWatchAlertDialog create() {
            SmartWatchAlertDialog dialog = new SmartWatchAlertDialog(context, title, message, iconResource);
            dialog.okButtonText = this.okButtonText;
            dialog.cancelButtonText = this.cancelButtonText;
            dialog.showIcon = this.showIcon;
            dialog.listener = this.listener;
            return dialog;
        }

        public void show() {
            if (isValidContext(context)) {
                SmartWatchAlertDialog dialog = create();
                dialog.show();
            } else {
                Log.w(TAG, "Cannot show dialog: context is not valid");
            }
        }
    }

    // Helper method to validate context
    private static boolean isValidContext(Context context) {
        if (context == null) {
            return false;
        }

        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            return !activity.isFinishing() && !activity.isDestroyed();
        }

        return true; // For non-Activity contexts
    }

    // Quick static methods for common dialogs
    public static void showConfirmDialog(Context context, String title, String message, DialogListener listener) {
        if (isValidContext(context)) {
            new Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setIcon(R.drawable.ic_warning)
                    .setDialogListener(listener)
                    .show();
        }
    }

    public static void showDeleteDialog(Context context, String message, DialogListener listener) {
        if (isValidContext(context)) {
            new Builder(context)
                    .setTitle("Delete Item")
                    .setMessage(message)
                    .setIcon(R.drawable.ic_delete)
                    .setOkButtonText("Delete")
                    .setCancelButtonText("Keep")
                    .setDialogListener(listener)
                    .show();
        }
    }

    public static void showSaveDialog(Context context, String message, DialogListener listener) {
        if (isValidContext(context)) {
            new Builder(context)
                    .setTitle("Save Changes")
                    .setMessage(message)
                    .setIcon(R.drawable.ic_save)
                    .setOkButtonText("Save")
                    .setCancelButtonText("Discard")
                    .setDialogListener(listener)
                    .show();
        }
    }
}