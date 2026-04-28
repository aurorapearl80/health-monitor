package com.monitor.health.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.monitor.health.R;
import com.monitor.health.viewmodel.SharedDataViewModel;

public abstract class BaseFragment extends Fragment {   // â† must be abstract

    private FrameLayout contentContainer;
    protected WatchStateOverlayView watchOverlay;
    protected SharedDataViewModel sharedModel;

    @LayoutRes
    protected abstract int getContentLayoutResId();

    protected void onBaseViewCreated(@NonNull View root) { /* optional hook */ }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_base_with_overlay, container, false);
        contentContainer = root.findViewById(R.id.contentContainer);
        inflater.inflate(getContentLayoutResId(), contentContainer, true);
        watchOverlay = root.findViewById(R.id.watchOverlay);
        return root;
    }

    protected FrameLayout getContentContainer() { return contentContainer; }

    protected void setWatchState(WatchStateOverlayView.State state) {
        if (watchOverlay != null) watchOverlay.setState(state);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);

        // ViewModel shared across activity
        sharedModel = new ViewModelProvider(requireActivity()).get(SharedDataViewModel.class);

        // Observe wear-state and map to overlay
        sharedModel.wearState().observe(getViewLifecycleOwner(), state -> {
            if (watchOverlay == null || state == null) return;

            if (state == SharedDataViewModel.WearState.WORN) {
                watchOverlay.setState(WatchStateOverlayView.State.HIDDEN);
            } else if (state == SharedDataViewModel.WearState.NOT_WORN) {
                watchOverlay.setState(WatchStateOverlayView.State.NOT_WORN);
            } else if (state == SharedDataViewModel.WearState.DISCONNECTED) {
//                watchOverlay.setState(WatchStateOverlayView.State.CONNECTING);
            } else if (state == SharedDataViewModel.WearState.BLUETOOTH_OFF) {
               // watchOverlay.setState(WatchStateOverlayView.State.NO_BLUETOOTH);
            } else if (state == SharedDataViewModel.WearState.NO_PERMISSION) {
                //watchOverlay.setState(WatchStateOverlayView.State.NO_PERMISSION);
            } else { // UNKNOWN
                //watchOverlay.setState(WatchStateOverlayView.State.CONNECTING);
            }
        });

        onBaseViewCreated(root);
    }
}
