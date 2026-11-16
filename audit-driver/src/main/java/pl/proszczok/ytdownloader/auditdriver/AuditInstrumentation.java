package pl.proszczok.ytdownloader.auditdriver;

import android.app.Instrumentation;
import android.app.UiAutomation;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Development tool only; never part of the shipped app. Keeps TalkBack connected. */
public class AuditInstrumentation extends Instrumentation {
    private Bundle args;
    private UiAutomation ui;
    private final List<JSONObject> events = Collections.synchronizedList(new ArrayList<>());

    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); args = arguments; start(); }

    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            ui = getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
            AccessibilityServiceInfo info = ui.getServiceInfo();
            info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
            ui.setServiceInfo(info);
            ui.setOnAccessibilityEventListener(event -> {
                String pkg = String.valueOf(event.getPackageName());
                if (!pkg.equals("pl.proszczok.pixelspeecheq") && !pkg.equals("com.google.android.marvin.talkback")) return;
                try {
                    events.add(new JSONObject().put("type", AccessibilityEvent.eventTypeToString(event.getEventType()))
                        .put("package", pkg).put("text", event.getText().toString())
                        .put("description", String.valueOf(event.getContentDescription())));
                } catch (Exception ignored) { }
            });
            SystemClock.sleep(400);
            String action = args.getString("action", "scan");
            boolean performed = action.equals("scan");
            if (!performed) {
                AccessibilityNodeInfo node = find(args.getString("text", ""), args.getString("match", "exact"));
                if (node == null) throw new IllegalStateException("Node not found: " + args.getString("text"));
                if (action.equals("focus")) {
                    while (!node.isFocusable() && node.getParent() != null && !node.isHeading()) node = node.getParent();
                    performed = node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
                }
                else if (action.equals("click")) {
                    while (!node.isClickable() && node.getParent() != null) node = node.getParent();
                    performed = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                } else if (action.equals("setText")) {
                    while (!node.isEditable() && node.getParent() != null) node = node.getParent();
                    Bundle text = new Bundle();
                    text.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, args.getString("value", ""));
                    performed = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, text);
                } else if (action.equals("scroll")) {
                    while (!node.isScrollable() && node.getParent() != null) node = node.getParent();
                    performed = node.performAction(args.getString("direction", "forward").equals("backward")
                        ? AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD : AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                }
            }
            SystemClock.sleep(Long.parseLong(args.getString("wait", "1500")));
            JSONArray windows = new JSONArray();
            for (AccessibilityWindowInfo window : ui.getWindows()) {
                AccessibilityNodeInfo root = window.getRoot();
                if (root != null) windows.put(new JSONObject().put("title", String.valueOf(window.getTitle()))
                    .put("type", window.getType()).put("root", snapshot(root)));
            }
            JSONArray eventArray = new JSONArray();
            synchronized (events) { for (JSONObject event : events) eventArray.put(event); }
            JSONObject report = new JSONObject().put("action", action).put("performed", performed)
                .put("windows", windows).put("events", eventArray);
            File dir = getTargetContext().getExternalFilesDir(null);
            try (FileOutputStream out = new FileOutputStream(new File(dir, "result.json"))) {
                out.write(report.toString(2).getBytes(StandardCharsets.UTF_8));
            }
            Bitmap screenshot = ui.takeScreenshot();
            if (screenshot != null) {
                try (FileOutputStream out = new FileOutputStream(new File(dir, "screen.png"))) {
                    screenshot.compress(Bitmap.CompressFormat.PNG, 100, out);
                }
                screenshot.recycle();
            }
            result.putString("stream", "AUDIT_OK " + performed + "\n");
            finish(0, result);
        } catch (Throwable error) {
            result.putString("stream", "AUDIT_ERROR " + error + "\n");
            finish(1, result);
        }
    }

    private AccessibilityNodeInfo find(String text, String match) {
        for (AccessibilityWindowInfo window : ui.getWindows()) {
            AccessibilityNodeInfo found = findNode(window.getRoot(), text, match);
            if (found != null) return found;
        }
        return null;
    }
    private AccessibilityNodeInfo findNode(AccessibilityNodeInfo node, String text, String match) {
        if (node == null) return null;
        String label = String.valueOf(node.getText());
        String desc = String.valueOf(node.getContentDescription());
        String targetPackage = args.getString("targetPackage", "");
        boolean packageMatches = targetPackage.isEmpty() || targetPackage.contentEquals(String.valueOf(node.getPackageName()));
        if (packageMatches && (match.equals("contains") ? label.contains(text) || desc.contains(text) : label.equals(text) || desc.equals(text))) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findNode(node.getChild(i), text, match);
            if (found != null) return found;
        }
        return null;
    }
    private JSONObject snapshot(AccessibilityNodeInfo node) throws Exception {
        Rect bounds = new Rect(); node.getBoundsInScreen(bounds);
        JSONArray children = new JSONArray();
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) children.put(snapshot(child));
        }
        JSONArray actions = new JSONArray();
        for (AccessibilityNodeInfo.AccessibilityAction a : node.getActionList()) actions.put(a.toString());
        return new JSONObject().put("text", String.valueOf(node.getText()))
            .put("desc", String.valueOf(node.getContentDescription())).put("class", String.valueOf(node.getClassName()))
            .put("package", String.valueOf(node.getPackageName())).put("bounds", bounds.toShortString())
            .put("state", String.valueOf(node.getStateDescription())).put("heading", node.isHeading())
            .put("selected", node.isSelected()).put("checked", node.isChecked()).put("checkable", node.isCheckable())
            .put("focusable", node.isFocusable()).put("focused", node.isFocused()).put("a11yFocused", node.isAccessibilityFocused())
            .put("clickable", node.isClickable()).put("enabled", node.isEnabled()).put("visible", node.isVisibleToUser())
            .put("range", String.valueOf(node.getRangeInfo())).put("liveRegion", node.getLiveRegion())
            .put("actions", actions).put("children", children);
    }
}
