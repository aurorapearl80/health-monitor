package com.monitor.health.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.monitor.health.R;

public class WatchStateOverlayView extends FrameLayout {

    public enum State { HIDDEN, NOT_WORN }

    private View root;
    private TextView title;
    private TextView message;

    public WatchStateOverlayView(Context context) {
        super(context);
        init();
    }

    public WatchStateOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WatchStateOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Inflate our internal layout INTO THIS view
        root = LayoutInflater.from(getContext()).inflate(R.layout.view_watch_state_overlay, this, true);
        setClickable(true);
        setFocusable(true);

        title = root.findViewById(R.id.tvTitle);
        message = root.findViewById(R.id.tvMessage);

        // default hidden
        setVisibility(GONE);
    }

    public void setState(State state) {
        if (state == State.HIDDEN) {
            setVisibility(GONE);
        } else {
            setVisibility(VISIBLE);
            // Make it super obvious
            setForeground(null);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) root.getLayoutParams();
            if (lp != null) lp.gravity = Gravity.CENTER;
            if (title != null) title.setText("Please wear the device");
            if (message != null) message.setText("During measurement please stay still");
        }
    }
}
