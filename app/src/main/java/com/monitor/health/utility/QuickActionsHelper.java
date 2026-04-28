package com.monitor.health.utility;


import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.monitor.health.R;

public class QuickActionsHelper {

    private View rootView;
    private QuickActionsHandler handler;
    private TextView messageBadge;
    private ImageView actionSettings;
    private ImageView actionHealth;
    private ImageView actionNotifications;

    public QuickActionsHelper(View rootView, QuickActionsHandler handler) {
        this.rootView = rootView;
        this.handler = handler;
        initViews();
        setupClickListeners();
    }

    private void initViews() {
        actionSettings = rootView.findViewById(R.id.action_settings);
        actionHealth = rootView.findViewById(R.id.action_health);
        actionNotifications = rootView.findViewById(R.id.action_notifications);
        messageBadge = rootView.findViewById(R.id.message_badge);
    }

    private void setupClickListeners() {
        if (actionSettings != null) {
            actionSettings.setOnClickListener(v -> handler.onSettingsClicked());
        }

        if (actionHealth != null) {
            actionHealth.setOnClickListener(v -> handler.onHealthClicked());
        }

        if (actionNotifications != null) {
            actionNotifications.setOnClickListener(v -> handler.onNotificationsClicked());
        }
    }

    public void updateMessageBadge(int count) {
        if (messageBadge != null) {
            if (count > 0) {
                messageBadge.setVisibility(View.VISIBLE);
                messageBadge.setText(String.valueOf(count));
            } else {
                messageBadge.setVisibility(View.GONE);
            }
        }
    }
}

