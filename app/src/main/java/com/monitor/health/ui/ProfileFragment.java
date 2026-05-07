package com.monitor.health.ui;

import static android.content.Context.BATTERY_SERVICE;
import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.monitor.health.ApiClient;
import com.monitor.health.Constant;
import com.monitor.health.LoginActivity;
import com.monitor.health.MainActivity;
import com.monitor.health.NetworkUtils;
import com.monitor.health.R;
import com.monitor.health.adapter.StatsAdapter;
import com.monitor.health.utility.PreferenceHelper;
import com.monitor.health.database.DatabaseClient;
import com.monitor.health.model.ReadingValue;
import com.monitor.health.model.healthscore.UserDrWatch;
import com.monitor.health.request.SendAlarmRequest;
import com.monitor.health.utility.DeviceUtils;
import com.monitor.health.utility.SmartWatchAlertDialog;
import com.monitor.health.viewmodel.SharedDataViewModel;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {
    private ImageView profileImage;
    private TextView userName;
    private TextView userStatus;
    private WearableRecyclerView statsRecyclerView;
    private LinearLayout quickActionsLayout;
    private View rootView;
    private List<String> userInfo;
    List<StatItem> stats;
    StatsAdapter adapter;
    SharedDataViewModel model;
    DatabaseClient databaseClient;

    int versionCode;
    SharedPreferences prefs;
    private static final String TAG = "ProfileFragment";
    String _model;                     // e.g., SM-G925I
    String _maker;              // e.g., Samsung
    String osVersion;       // e.g., 4.4, 12, 13
    String _country;
    String androidId;
    private static final int MAX_RETRY_COUNT = 3; // Set max retry attempts
    private int retryCount = 0;
    int batteryPercent = 0;

    private TextView messageBadge;
    private View messageButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_profile, container, false);


        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        versionCode = 0;
        databaseClient = DatabaseClient.getInstance(getActivity());
        userInfo = new ArrayList<>();

        androidId =  DeviceUtils.getIMEI(getActivity());

        _model = Build.MODEL;                     // e.g., SM-G925I
        _maker = Build.MANUFACTURER;              // e.g., Samsung
        osVersion = Build.VERSION.RELEASE;       // e.g., 4.4, 12, 13
        _country = Locale.getDefault().getCountry(); // e.g., PR empty for now

        BatteryManager batteryManager = (BatteryManager) getActivity().getSystemService(BATTERY_SERVICE);
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = getActivity().registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        batteryPercent = (level * 100) / scale;

        prefs = getActivity().getSharedPreferences("LocationPrefs", MODE_PRIVATE);

        model = new ViewModelProvider(requireActivity()).get(SharedDataViewModel.class);
        getUserInfo();
        initViews();
        setupRecyclerView();
        loadUserData();
        setupQuickActions();




    }

    private void getUserInfo() {
//        model.getUserInfo().observe(getViewLifecycleOwner(), temp -> {
//            //extView.setText(temp);
//            userInfo.addAll(temp);
//
//        });
    }

    private void initViews() {
        profileImage = rootView.findViewById(R.id.profile_image);
        userName = rootView.findViewById(R.id.user_name);
        userStatus = rootView.findViewById(R.id.user_status);
        statsRecyclerView = rootView.findViewById(R.id.stats_recycler_view);
        quickActionsLayout = rootView.findViewById(R.id.quick_actions_layout);
        messageButton = rootView.findViewById(R.id.action_notifications);
        messageBadge  = rootView.findViewById(R.id.message_badge);
    }

    private void setupRecyclerView() {
        WearableLinearLayoutManager layoutManager = new WearableLinearLayoutManager(getContext());
        statsRecyclerView.setLayoutManager(layoutManager);
        statsRecyclerView.setEdgeItemsCenteringEnabled(true);

        //List<StatItem> stats = createStatItems();
        stats = new ArrayList<>();
        adapter = new StatsAdapter(stats);
        statsRecyclerView.setAdapter(adapter);
    }

    private List<StatItem> createStatItems() {
        List<StatItem> stats = new ArrayList<>();
        stats.add(new StatItem("Steps Today", "8,247", R.drawable.ic_step));
        stats.add(new StatItem("Heart Rate", "72 BPM", R.drawable.ic_heart_rate));
        stats.add(new StatItem("Sleep", "7h 23m", R.drawable.ic_step));
        stats.add(new StatItem("Calories", "1,840", R.drawable.ic_calories));
        return stats;
    }

    // Method to update stats from API response
    @SuppressLint("NotifyDataSetChanged")
    public void updateStatsData(List<StatItem> newStats) {
        if (stats != null && adapter != null) {
            stats.clear();
            stats.addAll(newStats);
            adapter.notifyDataSetChanged();
        }
    }

    private void loadUserData() {

        try {
            PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            versionCode = pInfo.versionCode;
            String versionName = pInfo.versionName;

            Log.d("AppVersion", "Version Name: " + versionName + ", Version Code: " + versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        List<StatItem> newStats = new ArrayList<>();
        String IME = DeviceUtils.getIMEI(getActivity());
        List<UserDrWatch> list = databaseClient.getAppDatabase().userDrWatchDao().getAllDrWatch();
        if (list != null && !list.isEmpty() && list.get(0) != null) {
            newStats.add(new StatItem("Steps Today", "8,247", R.drawable.ic_step));
            newStats.add(new StatItem("Heart Rate", "72 BPM", R.drawable.ic_heart_rate));
            newStats.add(new StatItem("Sleep", "7h 23m", R.drawable.ic_step));
            newStats.add(new StatItem("Calories", "1,840", R.drawable.ic_calories));
            newStats.add(new StatItem("Email", list.get(0).getEmail(), R.drawable.ic_email));
            newStats.add(new StatItem("Phone", list.get(0).getPhone(), R.drawable.ic_phone));
            newStats.add(new StatItem("Health Score", list.get(0).getOverallHealthScore()+"", R.drawable.ic_health_score));
            newStats.add(new StatItem("VERSION", Constant.APP_VERSION, R.drawable.ic_health_score));
            newStats.add(new StatItem("IME", IME, R.drawable.ic_health_score));
            userName.setText(list.get(0).getUsername());
            // Update the adapter with the new data
            updateStatsData(newStats);
        } else {
            newStats.add(new StatItem("VERSION", Constant.APP_VERSION, R.drawable.ic_health_score));
            newStats.add(new StatItem("IME", IME, R.drawable.ic_health_score));
            updateStatsData(newStats);
        }

        userStatus.setText("Active");
        profileImage.setImageResource(R.drawable.ic_profile);
    }

    // Method to update individual stat item
    public void updateStatItem(String title, String newValue) {
        if (stats != null && adapter != null) {
            for (int i = 0; i < stats.size(); i++) {
                StatItem item = stats.get(i);
                if (item.getTitle().equals(title)) {
                    // Create new item with updated value
                    stats.set(i, new StatItem(item.getTitle(), newValue, item.getIconRes()));
                    adapter.notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    private void setupQuickActions() {
        setupQuickAction(R.id.action_settings, this::showLogoutConfirmation);

        // Handle health action
        // Navigation.findNavController(rootView).navigate(R.id.healthFragment);
        //showBasicDialog();
        setupQuickAction(R.id.action_health, this::showSettingUpBleDevice);

        setupQuickAction(R.id.action_notifications, () -> {
            // Handle notifications action
            // Navigation.findNavController(rootView).navigate(R.id.notificationsFragment);
            //showCustomDialog();
            //moveToVideoCall();
        });

        // OPEN MessagesActivity
        setupQuickAction(R.id.action_notifications, () -> {
            Intent intent = new Intent(getActivity(), MessagesActivity.class);
            startActivity(intent);
        });
    }

    private void showLogoutConfirmation() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_logout_confirmation);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = (int) (requireContext().getResources().getDisplayMetrics().widthPixels * 0.9);
            params.height = (int) (requireContext().getResources().getDisplayMetrics().heightPixels * 0.85);
            window.setAttributes(params);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.7f);
        }

        dialog.findViewById(R.id.btn_logout_confirm).setOnClickListener(v -> {
            dialog.dismiss();
            logout();
        });

        dialog.findViewById(R.id.btn_logout_cancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void logout() {
        PreferenceHelper.getInstance(requireContext()).remove(Constant.AUTH_TOKEN);
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showSettingUpBleDevice() {

        // Inside your Fragment's onClick or a specific logic block
        Intent intent = new Intent(getActivity(), BleDeviceSelectionActivity.class);
        startActivity(intent);
    }

    private void moveToVideoCall() {

        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.navigateToPage(7); // Navigate to page 1
        }
    }

    private void setupQuickAction(int viewId, Runnable action) {
        View view = rootView.findViewById(viewId);
        if (view != null) {
            view.setOnClickListener(v -> action.run());
        }
    }

    // Inner class for stat items
    public static class StatItem {
        private String title;
        private String value;
        private int iconRes;

        public StatItem(String title, String value, int iconRes) {
            this.title = title;
            this.value = value;
            this.iconRes = iconRes;
        }

        // Getters
        public String getTitle() { return title; }
        public String getValue() { return value; }
        public int getIconRes() { return iconRes; }
    }

    private void showBasicDialog() {
        new SmartWatchAlertDialog.Builder(getActivity())
                .setTitle("Delete Reading")
                .setMessage("Are you sure you want to delete this reading?")
                .setIcon(R.drawable.ic_delete)
                .setOkButtonText("Delete")
                .setCancelButtonText("Keep")
                .setDialogListener(new SmartWatchAlertDialog.DialogListener() {
                    @Override
                    public void onOkClicked() {
                        // Delete the reading
                        //deleteReading();
                    }

                    @Override
                    public void onCancelClicked() {
                        // Do nothing or show cancelled message
                    }
                })
                .show();
    }


    private void showCustomDialog() {
        SmartWatchAlertDialog dialog = new SmartWatchAlertDialog.Builder(getActivity())
                .setTitle("Custom Dialog")
                .setMessage("This is a custom dialog for smartwatch")
                .setIcon(R.drawable.ic_warning)
                .hideIcon() // Hide icon if needed
                .setDialogListener(new SmartWatchAlertDialog.DialogListener() {
                    @Override
                    public void onOkClicked() {
                        // Handle OK
                    }

                    @Override
                    public void onCancelClicked() {
                        // Handle Cancel
                    }
                })
                .create();

        dialog.show();
    }

    public void sendAlarm() {

        double latitude = Double.longBitsToDouble(prefs.getLong("latitude", Double.doubleToRawLongBits(0)));
        double longitude = Double.longBitsToDouble(prefs.getLong("longitude", Double.doubleToRawLongBits(0)));
//        SendAlarmRequest sendAlarmRequest = new SendAlarmRequest(latitude, longitude, androidId, 1, 1, batteryPercent, true,
//                _model, _maker,
//                "0", _country);
        // Location sample: lat: 148.752, Long: 87588.701
        SendAlarmRequest sendAlarmRequest = new SendAlarmRequest(latitude, longitude, androidId, 1, 1, batteryPercent, true,
                _model, _maker,
                "0", _country);
        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM, Constant.TOKEN_DR_WATCH_API, androidId)
                .sendAlarm(sendAlarmRequest);

        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Log.d(TAG, "SDK_003_CREATE_UPDATE_USER DRS_020_SEND_ALARM code " + response.code());
                Log.d(TAG, "SDK_003_CREATE_UPDATE_USER DRS_020_SEND_ALARM body " + response.body());
                Log.d(TAG, "SDK_003_CREATE_UPDATE_USER DRS_020_SEND_ALARM toString " + response.toString());
                Log.d(TAG, "SDK_003_CREATE_UPDATE_USER DRS_020_SEND_ALARM message " + response.message());
                // Hide progress
                if (response.code() == 200) {
                    Log.d(TAG, "Alarm sent successfully!");
                    retryCount = 0; // Reset retry count on success
                    playNotificationSound();
                } else {
                    handleRetry();
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                handleRetry();
            }
        });
    }
    @SuppressLint("LongLogTag")
    private void handleRetry() {
        if (retryCount < MAX_RETRY_COUNT) {
            retryCount++;
            Log.d(TAG, "Retrying sendAlarm... Attempt " + retryCount);
            sendAlarm();
        } else {
            Log.d(TAG, "Max retry attempts reached. Failed to send alarm.");
        }
    }

    private void playNotificationSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), notification);
            ringtone.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateConnectionIcon();
        // Demo: read unread count from SharedPreferences (replace with DB/API later)
//        SharedPreferences sp = requireContext().getSharedPreferences("msg_prefs", Context.MODE_PRIVATE);
//
//        // Demo: set unread to 3 if not existing yet (remove later)
//        if (!sp.contains("unread_count")) {
//            sp.edit().putInt("unread_count", 3).apply();
//        }
//
//        int unreadCount = sp.getInt("unread_count", 0);
        updateBadge(2);


    }

    private void updateConnectionIcon() {
        if (rootView == null) return;
        android.widget.ImageView iv = rootView.findViewById(R.id.ivConnectionStatus);
        if (iv == null) return;
        NetworkUtils.ConnectionQuality quality = NetworkUtils.getConnectionQuality(requireContext());
        if (quality == NetworkUtils.ConnectionQuality.STRONG) {
            iv.setImageResource(R.drawable.ic_signal_strong);
            iv.setVisibility(android.view.View.VISIBLE);
        } else if (quality == NetworkUtils.ConnectionQuality.WEAK) {
            iv.setImageResource(R.drawable.ic_signal_weak);
            iv.setVisibility(android.view.View.VISIBLE);
            NetworkUtils.showSlowConnectionToast(requireContext());
        } else {
            iv.setVisibility(android.view.View.INVISIBLE);
        }
    }

    private void updateBadge(int unreadCount) {
        if (messageBadge == null) return; // prevents crash

        if (unreadCount <= 0) {
            messageBadge.setVisibility(View.GONE);
        } else {
            messageBadge.setVisibility(View.VISIBLE);
            messageBadge.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
        }
    }
}