package com.monitor.health.utility;

public interface QuickActionsHandler {
    void setupQuickActions();
    void onSettingsClicked();
    void onHealthClicked();
    void onNotificationsClicked();
    void updateMessageBadge(int count);
}