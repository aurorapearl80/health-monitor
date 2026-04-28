package com.monitor.health.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.monitor.health.R;
import com.monitor.health.model.MessageThread;
import com.monitor.health.ui.MessageDetailActivity;
import com.monitor.health.utility.TimeUtils;

import java.util.List;

public class MessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_ITEM = 0;
    private static final int TYPE_FOOTER = 1;

    private List<MessageThread> items;
    private OnItemClickListener listener;
    private boolean showLoadingFooter = false;

    public boolean isLoadingFooterVisible() { return showLoadingFooter; }

    public interface OnItemClickListener {
        void onItemClick(MessageThread item);
    }

    public MessagesAdapter(List<MessageThread> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setLoading(boolean loading) {
        if (loading && !this.showLoadingFooter) {
            this.showLoadingFooter = true;
            // Insert footer at the end
            notifyItemInserted(items.size());
        } else if (!loading && this.showLoadingFooter) {
            // Remove footer from the end
            int footerPosition = items.size();
            this.showLoadingFooter = false;
            notifyItemRemoved(footerPosition);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (showLoadingFooter && position == items.size()) {
            return TYPE_FOOTER;
        }
        return TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_FOOTER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_loading_footer, parent, false);
            return new LoadingViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_thread, parent, false);
            return new MessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MessageViewHolder) {
            MessageThread thread = items.get(position);
            ((MessageViewHolder) holder).bind(thread, listener);
        }
        // Footer has no binding logic beyond showing the ProgressBar
    }

    @Override
    public int getItemCount() {
        return items.size() + (showLoadingFooter ? 1 : 0);
    }

    public void updateData(List<MessageThread> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private ImageView profileIcon;
        private TextView senderName;
        private TextView messagePreview;
        private TextView timestamp;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            profileIcon = itemView.findViewById(R.id.img_profile);
            senderName = itemView.findViewById(R.id.txt_name);
            messagePreview = itemView.findViewById(R.id.txt_last_message);
            timestamp = itemView.findViewById(R.id.txt_time);
        }

        public void bind(MessageThread thread, OnItemClickListener listener) {
            profileIcon.setImageResource(thread.iconResId);
            senderName.setText(thread.name);
            messagePreview.setText(thread.preview);
            timestamp.setText(TimeUtils.getRelativeTime(thread.timestamp));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    // Open MessageDetailActivity
                    Intent intent = new Intent(itemView.getContext(), MessageDetailActivity.class);
                    intent.putExtra("sender_name", thread.name);
                    intent.putExtra("message_body", thread.preview);
                    intent.putExtra("message_date", TimeUtils.formatFullDateTime(thread.timestamp));
                    itemView.getContext().startActivity(intent);
                }
            });
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        ProgressBar progressBar;
        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.loading_footer_progress);
        }
    }
}