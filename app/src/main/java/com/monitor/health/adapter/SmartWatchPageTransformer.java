package com.monitor.health.adapter;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

// SmartWatchPageTransformer.java - Custom page transformer for smooth animations
public class SmartWatchPageTransformer implements ViewPager2.PageTransformer {
    private static final float MIN_SCALE = 0.85f; // how small side pages look
    private static final float MIN_ALPHA = 0.6f;  // fade amount for side pages

    @Override
    public void transformPage(@NonNull View page, float position) {
        // pick a target to scale: the page's first child if present, else the page itself
        View target = page;
        if (page instanceof ViewGroup && ((ViewGroup) page).getChildCount() > 0) {
            target = ((ViewGroup) page).getChildAt(0);
        }

        float absPos = Math.abs(position);

        // zoom: center â†’ 1f, sides â†’ MIN_SCALE
        float scale = MIN_SCALE + (1f - MIN_SCALE) * (1f - absPos);
        target.setScaleX(scale);
        target.setScaleY(scale);
        target.setPivotX(target.getWidth() * 0.5f);
        target.setPivotY(target.getHeight() * 0.5f);

        // subtle fade on sides
        float alpha = MIN_ALPHA + (1f - MIN_ALPHA) * (1f - absPos);
        target.setAlpha(alpha);

        // normal horizontal slide
        page.setTranslationX(-position * page.getWidth());

        // click handling: only the centered page should be interactive
        boolean isCenter = absPos < 0.0001f;
        page.setClickable(isCenter);
        page.setEnabled(isCenter);
        page.setTranslationZ(isCenter ? 1f : 0f);

        // transparent background so parent shows through during animation
        page.setBackgroundColor(Color.TRANSPARENT);
    }
}
