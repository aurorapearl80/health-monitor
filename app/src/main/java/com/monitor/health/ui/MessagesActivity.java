package com.monitor.health.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.WearableRecyclerView;

import com.monitor.health.ApiClient;
import com.monitor.health.Constant;
import com.monitor.health.NetworkUtils;
import com.monitor.health.R;
import com.monitor.health.adapter.MessagesAdapter;
import com.monitor.health.dto.ApiResponseDTO;
import com.monitor.health.entity.MessageEntity;
import com.monitor.health.model.MessageThread;
import com.monitor.health.response.bledevice.DeviceResponseList;
import com.monitor.health.ui.service.MessageService;
import com.monitor.health.utility.DeviceUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessagesActivity extends AppCompatActivity {
    private static final String TAG = "MessagesActivity";
    private MessageService messageService;
    private MessagesAdapter adapter;
    private List<MessageThread> messageThreadList;
    private WearableRecyclerView rv;
    private ProgressBar progressBar;
    private TextView loadingText;
    private ImageButton btnCompose;
    private TextView tabRead;
    private TextView tabUnread;

    private int currentPage = 1; // For pagination
    private boolean isLoading = false; // Prevents duplicate API calls
    private boolean hasMore = true; // Whether there are more pages to load
    private boolean selectedIsRead = true; // Tab state: true = Read, false = Unread

    // Pagination disabled (rolled back)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        rv = findViewById(R.id.messages_recycler);
       // btnCompose = findViewById(R.id.btn_compose);
        progressBar = findViewById(R.id.progress_bar);
        loadingText = findViewById(R.id.loading_text);
        tabRead = findViewById(R.id.tab_read);
        tabUnread = findViewById(R.id.tab_unread);

        // Initialize service
        messageService = new MessageService(this);

        final androidx.wear.widget.WearableLinearLayoutManager wlm = new androidx.wear.widget.WearableLinearLayoutManager(this);
        rv.setLayoutManager(wlm);

        // Initialize empty list
        messageThreadList = new ArrayList<>();

        // Setup adapter
        adapter = new MessagesAdapter(messageThreadList, item -> {
            // Click handled in adapter now
        });

        rv.setAdapter(adapter);

       // btnCompose.setOnClickListener(v -> startActivity(new Intent(this, ComposeMessageActivity.class)));

        // Show loader initially
        showLoader("Loading messages...");

        // Load messages from database (filtered by current tab)
        loadMessagesFromDatabaseFiltered();

        if (NetworkUtils.isInternetConnected(getApplicationContext())){
            Log.d(TAG, "Network connection available, deleting all messages.");
            messageService.deleteAllMessages();
        } else {
            Log.d(TAG, "No network connection, skipping deleteAllMessages.");
        }
        // Fetch first page from API using current tab filter
        fetchInboxPage(1, 10, selectedIsRead, false);

        // Setup tabs behavior
        setupTabs();

        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy <= 0) return; // only trigger when scrolling down
                if (isLoading) return; // avoid duplicate loads

                RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
                if (adapter == null) return;

                int totalItemCount = adapter.getItemCount();
                if (totalItemCount == 0) return;

                // Compute last visible item position without assuming a specific LayoutManager type
                int childCount = recyclerView.getChildCount();
                int lastVisibleItemPosition = childCount > 0
                        ? recyclerView.getChildAdapterPosition(recyclerView.getChildAt(childCount - 1))
                        : RecyclerView.NO_POSITION;

                // Trigger when within threshold from the end to reduce sensitivity
                int threshold = 3; // load more when 3 items from bottom
                if (lastVisibleItemPosition != RecyclerView.NO_POSITION && lastVisibleItemPosition >= totalItemCount - 1 - threshold) {
                    onBottomReached();
                }
            }
        });
        

    }

    private void onBottomReached() {
        if (isLoading) {
            Log.d(TAG, "Already loading, skipping onBottomReached");
            return;
        }
        if (!hasMore) {
            Log.d(TAG, "No more pages to load");
            return;
        }

        Log.d(TAG, "Loading next page...");
        isLoading = true;
        // Show loading footer instead of full-screen loader; defer to next frame to avoid scroll-callback crash
        if (adapter != null && rv != null) {
            rv.post(() -> adapter.setLoading(true));
        }
        int nextPage = currentPage + 1;
        fetchInboxPage(nextPage, 10, selectedIsRead, true);
    }

    /**
     * Show loading indicator
     */
    private void showLoader(String message) {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.VISIBLE);
            loadingText.setVisibility(View.VISIBLE);
            loadingText.setText(message);
            rv.setVisibility(View.GONE);
        });
    }

    /**
     * Hide loading indicator
     */
    private void hideLoader() {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            loadingText.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
        });
    }

    /**
     * Set up Read/Unread tabs and their behavior
     */
    private void setupTabs() {
        if (tabRead == null || tabUnread == null) return;
        // Initial UI state
        updateTabUI();

        tabRead.setOnClickListener(v -> {
            if (!selectedIsRead) { // switch only if changed
                selectedIsRead = true;
                onTabChanged();
            }
        });
        tabUnread.setOnClickListener(v -> {
            if (selectedIsRead) {
                selectedIsRead = false;
                onTabChanged();
            }
        });
    }

    private void updateTabUI() {
        int selectedColor = ContextCompat.getColor(this, android.R.color.white);
        int unselectedColor = ContextCompat.getColor(this, android.R.color.darker_gray);
        if (selectedIsRead) {
            tabRead.setTextColor(selectedColor);
            tabUnread.setTextColor(unselectedColor);
        } else {
            tabRead.setTextColor(unselectedColor);
            tabUnread.setTextColor(selectedColor);
        }
    }

    private void onTabChanged() {
        // Update visual state
        updateTabUI();
        // Reset pagination state
        currentPage = 1;
        hasMore = true;
        isLoading = false;
        // Clear current list
        messageThreadList.clear();
        adapter.notifyDataSetChanged();
        // Show loader and fetch first page for selected tab
        showLoader("Loading messages...");
        fetchInboxPage(1, 10, selectedIsRead, false);
        // Also refresh from DB to show cached items matching the filter
        loadMessagesFromDatabaseFiltered();
    }

    /**
     * Load messages from local database and display them filtered by tab
     */
    private void loadMessagesFromDatabaseFiltered() {
        new Thread(() -> {
            try {
                List<MessageEntity> messages = messageService.getAllMessages();
                // Filter by isRead based on selected tab
                List<MessageEntity> filtered = new ArrayList<>();
                for (MessageEntity m : messages) {
                    if (m.isRead() == selectedIsRead) {
                        filtered.add(m);
                    }
                }
                List<MessageThread> threads = convertMessagesToThreads(filtered);

                runOnUiThread(() -> {
                    messageThreadList.clear();
                    messageThreadList.addAll(threads);
                    adapter.notifyDataSetChanged();

                    int unreadCount = messageService.getUnreadCount();
                    getSharedPreferences("msg_prefs", MODE_PRIVATE)
                            .edit()
                            .putInt("unread_count", unreadCount)
                            .apply();

                    Log.d(TAG, "âœ… Loaded " + threads.size() + " messages from database (" + (selectedIsRead ? "read" : "unread") + ")");

                    if (!threads.isEmpty()) {
                        hideLoader();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "âŒ Error loading messages from database", e);
            }
        }).start();
    }

    /**
     * Load messages from local database and display them
     */
    private void loadMessagesFromDatabase() {
        new Thread(() -> {
            try {
                // Get all messages from database
                List<MessageEntity> messages = messageService.getAllMessages();

                // Convert Message entities to MessageThread objects
                List<MessageThread> threads = convertMessagesToThreads(messages);

                // Update UI on main thread
                runOnUiThread(() -> {
                    messageThreadList.clear();
                    messageThreadList.addAll(threads);
                    adapter.notifyDataSetChanged();

                    // Update unread count
                    int unreadCount = messageService.getUnreadCount();
                    getSharedPreferences("msg_prefs", MODE_PRIVATE)
                            .edit()
                            .putInt("unread_count", unreadCount)
                            .apply();

                    Log.d(TAG, "âœ… Loaded " + threads.size() + " messages from database");

                    // Hide loader if we have messages
                    if (!threads.isEmpty()) {
                        hideLoader();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "âŒ Error loading messages from database", e);
            }
        }).start();
    }

    /**
     * Convert Message entities from database to MessageThread UI objects
     */
    private List<MessageThread> convertMessagesToThreads(List<MessageEntity> messages) {
        List<MessageThread> threads = new ArrayList<>();

        for (MessageEntity message : messages) {
            MessageThread thread = new MessageThread(
                    R.drawable.ic_profile,  // Default profile icon
                    message.getSubject(),
                    message.getBody(),
                    message.getMessageDate()
            );
            threads.add(thread);
        }

        return threads;
    }

    /**
     * Fetch messages from API and save to database
     */
    private void fetchInboxPage(int page, int perPage, Boolean isRead, boolean loadMore) {
        if (!loadMore) {
            showLoader("Syncing messages...");
        } else {
            // keep list visible; footer is already shown by onBottomReached
        }

        ApiClient.getUserService(
                Constant.BASE_URL_BGM,
                Constant.TOKEN_DR_WATCH_API,
                DeviceUtils.getIMEI(getApplicationContext())
        ).getInboxMessage(page, perPage, isRead).enqueue(new Callback<ApiResponseDTO>() {
            @Override
            public void onResponse(Call<ApiResponseDTO> call, Response<ApiResponseDTO> response) {
                Log.d(TAG, "âœ… API Response received for page: " + page + ", perPage: " + perPage + ", isRead: " + isRead);
                Log.d(TAG, "Response Data: " + response.body());

                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "API Response OK");

                    // Save to database in background thread
                    new Thread(() -> {
                        if (!loadMore) {
                            showLoader("Saving messages...");
                        }
                        ApiResponseDTO body = response.body();
                        messageService.saveApiMessages(body);
                        Log.d(TAG, "âœ… Messages saved to database");

                        // Determine hasMore using metadata or data size with safe fallbacks
                        boolean moreAvailable;
                        try {
                            if (body.getMeta() != null) {
                                int lastPage = body.getMeta().getLastPage();
                                int current = body.getMeta().getCurrentPage();
                                if (lastPage > 0 && current > 0) {
                                    moreAvailable = current < lastPage;
                                } else {
                                    // Fallback if meta fields are not populated correctly
                                    moreAvailable = body.getData() != null && body.getData().size() >= perPage;
                                }
                                Log.d(TAG, "[Paging] meta: current=" + current + ", last=" + lastPage + ", computed hasMore=" + moreAvailable);
                            } else {
                                moreAvailable = body.getData() != null && body.getData().size() >= perPage;
                                Log.d(TAG, "[Paging] no meta, dataSize=" + (body.getData() == null ? 0 : body.getData().size()) + ", perPage=" + perPage + ", hasMore=" + moreAvailable);
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "[Paging] meta evaluation failed, defaulting heuristic", e);
                            moreAvailable = body.getData() != null && body.getData().size() >= perPage;
                        }
                        boolean finalMoreAvailable = moreAvailable;

                        // Reload messages from database to display updated data (respect current tab)
                        loadMessagesFromDatabaseFiltered();

                        runOnUiThread(() -> {
                            if (!loadMore) {
                                hideLoader();
                            } else if (adapter != null) {
                                adapter.setLoading(false);
                            }
                            // advance currentPage only after successful load
                            currentPage = page;
                            hasMore = finalMoreAvailable;
                            isLoading = false; // Reset loading state
                        });
                    }).start();
                } else {
                    Log.e(TAG, "âŒ Server error: " + response.code() + " - " + response.message());
                    runOnUiThread(() -> {
                        if (!loadMore) {
                            hideLoader();
                            showLoader("Failed to fetch messages");
                        } else if (adapter != null) {
                            adapter.setLoading(false);
                        }
                        isLoading = false; // Reset loading state
                    });
                }
            }

            @Override
            public void onFailure(Call<ApiResponseDTO> call, Throwable t) {
                Log.e(TAG, "âŒ API Sync failed", t);
                runOnUiThread(() -> {
                    if (!loadMore) {
                        showLoader("Connection error. Loading cached messages...");
                    } else if (adapter != null) {
                        adapter.setLoading(false);
                    }
                });
                // Load cached messages from database if API fails
                loadMessagesFromDatabase();
                isLoading = false; // Reset loading state
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh messages when activity resumes (respect current tab filter)
        loadMessagesFromDatabaseFiltered();
    }
}