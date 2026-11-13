package pl.proszczok.pixelspeecheq;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.FrameLayout;

final class OverlayController {
    // Quickstep and screen readers both restore their own focus while the Recents
    // transition is settling. Request focus after that hand-off, otherwise TalkBack
    // can immediately move it back to the current task card.
    private static final long SCREEN_READER_FOCUS_DELAY_MS = 900;

    interface Listener {
        void onClearAllRequested();
    }

    private final PixelateAccessibilityService service;
    private final Listener listener;
    private final WindowManager windowManager;
    private final Runnable screenReaderFocusRunnable = this::focusForScreenReader;

    private FrameLayout root;
    private Button button;
    private WindowManager.LayoutParams params;
    private boolean shown;

    OverlayController(PixelateAccessibilityService service, Listener listener) {
        this.service = service;
        this.listener = listener;
        this.windowManager = (WindowManager) service.getSystemService(PixelateAccessibilityService.WINDOW_SERVICE);
        createView();
    }

    void show(boolean focusForScreenReader) {
        if (shown || windowManager == null) {
            return;
        }
        try {
            updateVerticalOffset();
            windowManager.addView(root, params);
            shown = true;
            if (focusForScreenReader) {
                button.removeCallbacks(screenReaderFocusRunnable);
                button.postDelayed(screenReaderFocusRunnable, SCREEN_READER_FOCUS_DELAY_MS);
            }
        } catch (RuntimeException ignored) {
            shown = false;
        }
    }

    void hide() {
        if (button != null) {
            button.removeCallbacks(screenReaderFocusRunnable);
        }
        if (!shown || windowManager == null) {
            return;
        }
        try {
            windowManager.removeView(root);
        } catch (RuntimeException ignored) {
            // The window may already have been removed by the system.
        } finally {
            shown = false;
        }
    }

    void setBusy(boolean busy) {
        button.setEnabled(!busy);
        button.setText(busy ? R.string.overlay_clearing : R.string.overlay_clear_all);
        button.setContentDescription(service.getString(
                busy ? R.string.overlay_clearing : R.string.overlay_clear_all));
        button.setAlpha(busy ? 0.82f : 1f);
        if (busy) {
            button.announceForAccessibility(service.getString(R.string.overlay_clearing));
        }
    }

    void announce(String message) {
        if (button != null) {
            button.announceForAccessibility(message);
        }
    }

    void onConfigurationChanged() {
        if (button != null && params != null) {
            String title = service.getString(R.string.overlay_window_title);
            button.setAccessibilityPaneTitle(title);
            params.setTitle(title);
        }
        if (!shown || windowManager == null) {
            return;
        }
        updateVerticalOffset();
        try {
            windowManager.updateViewLayout(root, params);
        } catch (RuntimeException ignored) {
            // A later accessibility event will recreate the overlay if necessary.
        }
    }

    void destroy() {
        hide();
        button = null;
        root = null;
    }

    @SuppressLint("AccessibilityFocus")
    private void focusForScreenReader() {
        if (!shown || button == null || !button.isShown()) {
            return;
        }
        button.performAccessibilityAction(
                AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null);
    }

    private void createView() {
        root = new FrameLayout(service);
        root.setPadding(dp(2), dp(2), dp(2), dp(2));
        root.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);

        button = new Button(service);
        button.setText(R.string.overlay_clear_all);
        button.setTextColor(buttonTextColor());
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setAllCaps(false);
        button.setSingleLine(true);
        button.setGravity(Gravity.CENTER);
        button.setMinWidth(dp(184));
        button.setMinHeight(dp(48));
        button.setPadding(dp(12), dp(4), dp(12), dp(4));
        button.setBackground(buttonBackground());
        button.setElevation(0);
        button.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_clear_all_apps, 0, 0, 0);
        button.setCompoundDrawablePadding(dp(8));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            button.setCompoundDrawableTintList(
                    ColorStateList.valueOf(buttonTextColor()));
        }
        button.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        button.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(Button.class.getName());
                info.setText(service.getString(host.isEnabled()
                        ? R.string.overlay_clear_all : R.string.overlay_clearing));
            }
        });

        button.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            listener.onClearAllRequested();
        });

        FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        root.addView(button, buttonParams);

        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                flags,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.setTitle(service.getString(R.string.overlay_window_title));
        updateVerticalOffset();
    }

    private Drawable buttonBackground() {
        GradientDrawable surface = new GradientDrawable();
        surface.setColor(Color.TRANSPARENT);
        surface.setCornerRadius(dp(24));
        int ripple = isNightMode()
                ? Color.argb(48, 255, 255, 255)
                : Color.argb(42, 0, 0, 0);
        return new RippleDrawable(ColorStateList.valueOf(ripple), surface, null);
    }

    private int buttonTextColor() {
        return isNightMode()
                ? systemColor("system_neutral1_50", Color.rgb(245, 245, 247))
                : systemColor("system_neutral1_900", Color.rgb(32, 33, 36));
    }

    private boolean isNightMode() {
        return (service.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    @SuppressLint("DiscouragedApi")
    private int systemColor(String name, int fallback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return fallback;
        int id = service.getResources().getIdentifier(name, "color", "android");
        return id == 0 ? fallback : service.getColor(id);
    }

    @SuppressLint({"DiscouragedApi", "InternalInsetResource"})
    private void updateVerticalOffset() {
        boolean landscape = service.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        int bottomInset = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && windowManager != null) {
            WindowMetrics metrics = windowManager.getCurrentWindowMetrics();
            Insets insets = metrics.getWindowInsets().getInsetsIgnoringVisibility(
                    WindowInsets.Type.navigationBars());
            bottomInset = insets.bottom;
        } else if (!landscape) {
            int id = service.getResources().getIdentifier(
                    "navigation_bar_height", "dimen", "android");
            if (id != 0) {
                bottomInset = service.getResources().getDimensionPixelSize(id);
            }
        }
        params.y = Math.max(bottomInset, landscape ? dp(8) : 0) + dp(20);
    }

    private int dp(int value) {
        return Math.round(value * service.getResources().getDisplayMetrics().density);
    }
}
