package com.monitor.health.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.WearableRecyclerView;

import com.google.gson.Gson;
import com.monitor.health.ApiClient;
import com.monitor.health.Constant;
import com.monitor.health.R;
import com.monitor.health.adapter.MessagesAdapter;
import com.monitor.health.chat.ChatSocketManager;
import com.monitor.health.chat.dto.ChatMessageDTO;
import com.monitor.health.chat.dto.ChatMessagePageDTO;
import com.monitor.health.entity.MessageEntity;
import com.monitor.health.model.MessageThread;
import com.monitor.health.ui.service.MessageService;
import com.monitor.health.utility.DeviceUtils;
import com.monitor.health.utility.PreferenceHelper;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessagesActivity extends AppCompatActivity {
    private static final String TAG = "MessagesActivity";
    private static final int PER_PAGE = 50;

    private MessageService messageService;
    private MessagesAdapter adapter;
    private List<MessageThread> messageThreadList;
    private WearableRecyclerView rv;
    private ProgressBar progressBar;
    private TextView loadingText;
    private Button btnCompose;
    private TextView tabRead;
    private TextView tabUnread;

    private int currentPage = 1; // For pagination
    private boolean isLoading = false; // Prevents duplicate API calls
    private boolean hasMore = true; // Whether there are more pages to load
    private boolean selectedIsRead = true; // Tab state: true = Read, false = Unread

    private long myUserId = -1;
    private String watchSerial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        rv = findViewById(R.id.messages_recycler);
        btnCompose = findViewById(R.id.btn_compose);
        progressBar = findViewById(R.id.progress_bar);
        loadingText = findViewById(R.id.loading_text);
        tabRead = findViewById(R.id.tab_read);
        tabUnread = findViewById(R.id.tab_unread);

        messageService = new MessageService(this);
        watchSerial = DeviceUtils.resolveWatchSerial(this);

        // Optional: only used to register for live push (see onStart) — chat itself works
        // purely off watchSerial regardless of whether this device has ever logged in.
        String userIdStr = PreferenceHelper.getInstance(this).getString(Constant.USER_ID, null);
        if (userIdStr != null) {
            try {
                myUserId = Long.parseLong(userIdStr);
            } catch (NumberFormatException ignored) {
                Log.w(TAG, "Stored USER_ID is not numeric: " + userIdStr);
            }
        }

        final androidx.wear.widget.WearableLinearLayoutManager wlm = new androidx.wear.widget.WearableLinearLayoutManager(this);
        rv.setLayoutManager(wlm);

        messageThreadList = new ArrayList<>();
        adapter = new MessagesAdapter(messageThreadList, item -> {
            // Click handled in adapter now
        });
        rv.setAdapter(adapter);

        btnCompose.setOnClickListener(v -> startActivity(new Intent(this, ComposeMessageActivity.class)));

        ChatSocketManager.getInstance().setListener(this::onIncomingMessage);

        showLoader("Loading messages...");
        loadMessagesFromDatabaseFiltered();
        fetchMessages(1, PER_PAGE, false);

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

                int childCount = recyclerView.getChildCount();
                int lastVisibleItemPosition = childCount > 0
                        ? recyclerView.getChildAdapterPosition(recyclerView.getChildAt(childCount - 1))
                        : RecyclerView.NO_POSITION;

                int threshold = 3; // load more when 3 items from bottom
                if (lastVisibleItemPosition != RecyclerView.NO_POSITION && lastVisibleItemPosition >= totalItemCount - 1 - threshold) {
                    onBottomReached();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Connect only while this screen is visible — avoids an always-on socket
        // draining the wearable's battery.
        if (myUserId > 0) {
            ChatSocketManager.getInstance().connect(myUserId);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        ChatSocketManager.getInstance().disconnect();
    }

    private void onIncomingMessage(JSONObject messageJson) {
        try {
            ChatMessageDTO dto = new Gson().fromJson(messageJson.toString(), ChatMessageDTO.class);
            new Thread(() -> {
                messageService.saveMessage(dto);
                runOnUiThread(this::loadMessagesFromDatabaseFiltered);
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle incoming private_message", e);
        }
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
        fetchMessages(nextPage, PER_PAGE, true);
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
        // Both tabs are filtered client-side from the same local cache — unlike the old
        // DrWatch inbox endpoint, the real chat API has no per-request read/unread filter.
        updateTabUI();
        showLoader("Loading messages...");
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
                    hideLoader();

                    Log.d(TAG, "Loaded " + threads.size() + " messages from database (" + (selectedIsRead ? "read" : "unread") + ")");
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading messages from database", e);
            }
        }).start();
    }

    /**
     * Convert Message entities from database to MessageThread UI objects
     */
    private List<MessageThread> convertMessagesToThreads(List<MessageEntity> messages) {
        List<MessageThread> threads = new ArrayList<>();

        for (MessageEntity message : messages) {
            String name = message.isMine()
                    ? "You"
                    : (message.getSenderName() != null ? message.getSenderName() : "Care Team");
            String preview = message.getBody() != null
                    ? message.getBody()
                    : (message.getAttachmentName() != null ? "📎 " + message.getAttachmentName() : "");

            // Always the OTHER party's photo — the care team contact's for messages I sent,
            // the sender's for messages I received.
            String avatarUrl = message.isMine()
                    ? message.getRecipientProfileImageUrl()
                    : message.getSenderProfileImageUrl();

            threads.add(new MessageThread(
                    R.drawable.ic_profile,
                    name,
                    preview,
                    message.getCreatedAt(),
                    message.getApiId(),
                    message.isMine(),
                    message.isRead(),
                    avatarUrl
            ));
        }

        return threads;
    }

    /**
     * Fetch a page of the patient's chat thread and upsert it into the local cache.
     */
    private void fetchMessages(int page, int perPage, boolean loadMore) {
        if (!loadMore) {
            showLoader("Syncing messages...");
        }

        ApiClient.getChatService().getMessages(watchSerial, page, perPage).enqueue(new Callback<ChatMessagePageDTO>() {
            @Override
            public void onResponse(Call<ChatMessagePageDTO> call, Response<ChatMessagePageDTO> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ChatMessagePageDTO body = response.body();

                    new Thread(() -> {
                        messageService.saveMessages(body.getData());

                        boolean moreAvailable = body.getMeta() != null
                                && body.getMeta().getLastPage() > 0
                                && body.getMeta().getCurrentPage() < body.getMeta().getLastPage();

                        loadMessagesFromDatabaseFiltered();

                        runOnUiThread(() -> {
                            if (!loadMore) {
                                hideLoader();
                            } else if (adapter != null) {
                                adapter.setLoading(false);
                            }
                            currentPage = page;
                            hasMore = moreAvailable;
                            isLoading = false;
                        });
                    }).start();
                } else if (response.code() == 422) {
                    // Serial isn't linked to a patient account yet — not an error worth alarming over.
                    Log.w(TAG, "This watch's serial isn't linked to a patient account yet.");
                    runOnUiThread(() -> {
                        if (!loadMore) {
                            hideLoader();
                        } else if (adapter != null) {
                            adapter.setLoading(false);
                        }
                        isLoading = false;
                        hasMore = false;
                    });
                } else {
                    Log.e(TAG, "Server error: " + response.code() + " - " + response.message());
                    runOnUiThread(() -> {
                        if (!loadMore) {
                            hideLoader();
                        } else if (adapter != null) {
                            adapter.setLoading(false);
                        }
                        isLoading = false;
                    });
                }
            }

            @Override
            public void onFailure(Call<ChatMessagePageDTO> call, Throwable t) {
                Log.e(TAG, "Chat sync failed", t);
                runOnUiThread(() -> {
                    if (!loadMore) {
                        hideLoader();
                    } else if (adapter != null) {
                        adapter.setLoading(false);
                    }
                    isLoading = false;
                });
                // Fall back to whatever is already cached
                loadMessagesFromDatabaseFiltered();
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
