package pl.proszczok.pixelspeecheq;

import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.view.WindowManager;

import java.util.List;

final class ClearAllController {
    interface Listener {
        void onClearSucceeded();

        void onClearFailed(boolean noTasks);
    }

    private static final String TAG = "PixelCenterRecents";
    private static final int GESTURE_FALLBACK_AFTER = 20;
    private static final long REVEAL_SETTLE_MS = 180;

    private final PixelateAccessibilityService service;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable stepRunnable = this::step;

    private boolean running;
    private boolean revealAttempted;
    private int attempts;
    private long startedAt;
    private long timeoutMs;

    ClearAllController(PixelateAccessibilityService service, Listener listener) {
        this.service = service;
        this.listener = listener;
    }

    void start() {
        if (running) {
            return;
        }
        running = true;
        attempts = 0;
        revealAttempted = false;
        startedAt = SystemClock.uptimeMillis();
        timeoutMs = PixelatePreferences.getSearchTimeoutSeconds(service) * 1000L;
        Log.d(TAG, "Start; timeout=" + timeoutMs + " ms");
        schedule(40);
    }

    boolean isRunning() {
        return running;
    }

    void cancel() {
        running = false;
        handler.removeCallbacks(stepRunnable);
    }

    private void step() {
        if (!running) {
            return;
        }

        List<AccessibilityWindowInfo> windows = service.getWindows();
        // Kod autora 1.4.2: warunki anulowania sprawdzamy przed wykonaniem
        // jakiejkolwiek akcji na świeżo odczytanym drzewie Ostatnich.
        if (!OverviewDetector.isOverviewVisible(windows, service.getPackageName(), service)) {
            finishFailure(false);
            return;
        }
        if (timeoutMs > 0 && SystemClock.uptimeMillis() - startedAt >= timeoutMs) {
            finishFailure(false);
            return;
        }
        AccessibilityNodeInfo clear = OverviewDetector.findClearAllButton(
                windows, service.getPackageName(), service);
        if (clear != null) {
            boolean clicked;
            try {
                clicked = OverviewDetector.performClickWithAncestor(clear);
            } finally {
                clear.recycle();
            }
            if (clicked) {
                running = false;
                handler.removeCallbacks(stepRunnable);
                listener.onClearSucceeded();
                Log.d(TAG, "Systemowy przycisk zamykania zostal nacisniety po "
                        + attempts + " krokach");
                return;
            }
        }

        if (attempts == 0 && !OverviewDetector.hasRecentTasks(
                windows, service.getPackageName(), service)) {
            finishFailure(true);
            return;
        }

        if (!revealAttempted) {
            revealAttempted = true;
            AccessibilityNodeInfo target = OverviewDetector.findClearAllButtonToReveal(
                    windows, service.getPackageName(), service);
            if (target != null) {
                boolean revealed;
                try {
                    revealed = target.performAction(
                            AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.getId());
                } finally {
                    target.recycle();
                }
                if (revealed) {
                    // Nie klikamy niewidocznego węzła. Po krótkiej stabilizacji
                    // ponownie pobieramy drzewo i dopiero wtedy klikamy element.
                    Log.d(TAG, "ACTION_SHOW_ON_SCREEN przyjete; ponowny odczyt drzewa");
                    schedule(REVEAL_SETTLE_MS);
                    return;
                }
            }
        }

        attempts++;

        boolean scrolled = false;
        if (attempts < GESTURE_FALLBACK_AFTER) {
            AccessibilityNodeInfo scroller = OverviewDetector.findOverviewScroller(
                    windows, service.getPackageName(), service);
            if (scroller != null) {
                try {
                    scrolled = scroller.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
                    if (!scrolled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        scrolled = scroller.performAction(
                                AccessibilityNodeInfo.AccessibilityAction.ACTION_PAGE_RIGHT.getId());
                    }
                    if (!scrolled) {
                        scrolled = scroller.performAction(
                                AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT.getId());
                    }
                } finally {
                    scroller.recycle();
                }
            }
        }

        if (scrolled) {
            Log.d(TAG, "Krok " + attempts + ": przewiniecie dostepnosci");
            // Android 9/10 Quickstep zatwierdza stronę dopiero po zakończeniu
            // animacji. Zbyt szybkie ponowienie uruchamiało tę samą stronę.
            schedule(Build.VERSION.SDK_INT < Build.VERSION_CODES.R ? 600 : 135);
        } else {
            Log.d(TAG, "Krok " + attempts + ": gest awaryjny");
            dispatchSwipeTowardClearAll();
            schedule(230);
        }
    }

    private void dispatchSwipeTowardClearAll() {
        WindowManager manager = (WindowManager) service.getSystemService(PixelateAccessibilityService.WINDOW_SERVICE);
        Display display = manager == null ? null : manager.getDefaultDisplay();
        if (display == null) {
            return;
        }
        Point size = new Point();
        display.getRealSize(size);
        float y = size.y * 0.46f;
        Path path = new Path();
        // Zachowanie autora 1.4.2: w karuzeli LTR gest prawo-lewo przesuwa
        // widok w stronę ostatniej strony z systemowym przyciskiem ClearAllButton.
        path.moveTo(size.x * 0.84f, y);
        path.lineTo(size.x * 0.18f, y);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 190))
                .build();
        service.dispatchGesture(gesture, null, handler);
    }

    private void finishFailure(boolean noTasks) {
        running = false;
        handler.removeCallbacks(stepRunnable);
        Log.w(TAG, noTasks ? "Brak kart do zamkniecia"
                : "Nie znaleziono systemowego przycisku przed limitem");
        listener.onClearFailed(noTasks);
    }

    private void schedule(long delayMs) {
        handler.removeCallbacks(stepRunnable);
        handler.postDelayed(stepRunnable, delayMs);
    }
}
