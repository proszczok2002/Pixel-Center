package pl.proszczok.pixelspeecheq;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Rect;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class OverviewDetector {
    private static final int NODE_BUDGET = 1400;
    private static final String[] OVERVIEW_TITLE_RESOURCE_NAMES = {
            "accessibility_recent_apps",
            "recent_apps_label",
            "accessibility_overview",
            "overview_title"
    };
    private static final Map<String, List<String>> overviewTitleCache = new HashMap<>();

    private OverviewDetector() {
    }

    static boolean mayBeOverviewPending(
            List<AccessibilityWindowInfo> windows, String ownPackage) {
        if (windows == null || windows.isEmpty()) {
            return true;
        }
        for (AccessibilityWindowInfo window : windows) {
            if (!isInspectableWindow(window) || (!window.isActive() && !window.isFocused())) {
                continue;
            }
            AccessibilityNodeInfo root = window.getRoot();
            if (root == null) {
                return true;
            }
            try {
                String pkg = string(root.getPackageName());
                if (TextUtils.equals(ownPackage, pkg)) {
                    return false;
                }
                return isLauncherPackage(pkg);
            } finally {
                root.recycle();
            }
        }
        return false;
    }

    static boolean isOverviewVisible(
            List<AccessibilityWindowInfo> windows, String ownPackage, Context context) {
        if (windows == null) {
            return false;
        }
        for (AccessibilityWindowInfo window : windows) {
            if (!isInspectableWindow(window)) {
                continue;
            }
            AccessibilityNodeInfo root = window.getRoot();
            if (root == null) {
                continue;
            }
            try {
                if (!TextUtils.equals(ownPackage, string(root.getPackageName()))
                        && looksLikeOverviewWindow(window, root, context)) {
                    return true;
                }
            } finally {
                root.recycle();
            }
        }
        return false;
    }

    static AccessibilityNodeInfo findClearAllButton(
            List<AccessibilityWindowInfo> windows, String ownPackage, Context context) {
        return findClearAllButton(windows, ownPackage, context, false);
    }

    static AccessibilityNodeInfo findClearAllButtonToReveal(
            List<AccessibilityWindowInfo> windows, String ownPackage, Context context) {
        return findClearAllButton(windows, ownPackage, context, true);
    }

    private static AccessibilityNodeInfo findClearAllButton(
            List<AccessibilityWindowInfo> windows, String ownPackage, Context context,
            boolean revealOnly) {
        if (windows == null) {
            return null;
        }
        for (AccessibilityWindowInfo window : windows) {
            if (!isInspectableWindow(window)) {
                continue;
            }
            AccessibilityNodeInfo root = window.getRoot();
            if (root == null) {
                continue;
            }
            try {
                String pkg = string(root.getPackageName());
                if (TextUtils.equals(ownPackage, pkg)
                        || !looksLikeOverviewWindow(window, root, context)) {
                    continue;
                }
                AccessibilityNodeInfo match = findClearNode(
                        root, new int[]{NODE_BUDGET}, revealOnly);
                if (match != null) {
                    return match;
                }
            } finally {
                root.recycle();
            }
        }
        return null;
    }

    static boolean hasRecentTasks(
            List<AccessibilityWindowInfo> windows, String ownPackage, Context context) {
        if (windows == null) {
            return false;
        }
        for (AccessibilityWindowInfo window : windows) {
            if (!isInspectableWindow(window)) {
                continue;
            }
            AccessibilityNodeInfo root = window.getRoot();
            if (root == null) {
                continue;
            }
            try {
                String pkg = string(root.getPackageName());
                if (!TextUtils.equals(ownPackage, pkg)
                        && looksLikeOverviewWindow(window, root, context)
                        && containsTaskCard(root, new int[]{NODE_BUDGET})) {
                    return true;
                }
            } finally {
                root.recycle();
            }
        }
        return false;
    }

    static AccessibilityNodeInfo findOverviewScroller(
            List<AccessibilityWindowInfo> windows, String ownPackage, Context context) {
        if (windows == null) {
            return null;
        }
        AccessibilityNodeInfo largestScrollable = null;
        long largestArea = -1;
        for (AccessibilityWindowInfo window : windows) {
            if (!isInspectableWindow(window)) {
                continue;
            }
            AccessibilityNodeInfo root = window.getRoot();
            if (root == null) {
                continue;
            }
            try {
                String pkg = string(root.getPackageName());
                if (TextUtils.equals(ownPackage, pkg)
                        || !looksLikeOverviewWindow(window, root, context)) {
                    continue;
                }
                ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
                queue.add(AccessibilityNodeInfo.obtain(root));
                int remaining = NODE_BUDGET;
                while (!queue.isEmpty() && remaining-- > 0) {
                    AccessibilityNodeInfo node = queue.removeFirst();
                    String id = lower(node.getViewIdResourceName());
                    if (node.isVisibleToUser()
                            && (isResourceId(id, "overview_panel")
                            || id.contains("recents_view"))) {
                        AccessibilityNodeInfo result = AccessibilityNodeInfo.obtain(node);
                        node.recycle();
                        recycleQueue(queue);
                        if (largestScrollable != null) {
                            largestScrollable.recycle();
                        }
                        return result;
                    }
                    if (node.isVisibleToUser() && (node.isScrollable() || hasScrollAction(node))) {
                        Rect bounds = new Rect();
                        node.getBoundsInScreen(bounds);
                        long area = Math.max(0, bounds.width()) * (long) Math.max(0, bounds.height());
                        if (area > largestArea) {
                            if (largestScrollable != null) {
                                largestScrollable.recycle();
                            }
                            largestScrollable = AccessibilityNodeInfo.obtain(node);
                            largestArea = area;
                        }
                    }
                    for (int i = 0; i < node.getChildCount(); i++) {
                        AccessibilityNodeInfo child = node.getChild(i);
                        if (child != null) {
                            queue.addLast(child);
                        }
                    }
                    node.recycle();
                }
                recycleQueue(queue);
            } finally {
                root.recycle();
            }
        }
        return largestScrollable;
    }

    static boolean performClickWithAncestor(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = AccessibilityNodeInfo.obtain(node);
        try {
            for (int depth = 0; depth < 5 && current != null; depth++) {
                if (current.isVisibleToUser() && current.isEnabled() && current.isClickable()
                        && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true;
                }
                AccessibilityNodeInfo parent = current.getParent();
                current.recycle();
                current = parent;
            }
            return false;
        } finally {
            if (current != null) {
                current.recycle();
            }
        }
    }

    private static boolean looksLikeOverviewRoot(AccessibilityNodeInfo root) {
        String pkg = lower(root.getPackageName());
        if (!isLauncherPackage(pkg)) {
            return false;
        }
        int score = scoreTree(root, new int[]{NODE_BUDGET});
        return score >= 3;
    }

    private static boolean isLauncherPackage(String packageName) {
        String pkg = lower(packageName);
        return pkg.contains("launcher") || pkg.contains("quickstep")
                || pkg.equals("com.google.android.apps.nexuslauncher")
                || pkg.equals("com.android.systemui")
                || pkg.equals("com.android.wm.shell");
    }

    private static boolean looksLikeOverviewWindow(
            AccessibilityWindowInfo window, AccessibilityNodeInfo root, Context context) {
        if (!window.isActive() && !window.isFocused()) {
            return false;
        }
        if (!looksLikeOverviewRoot(root)) {
            return false;
        }
        String title = normalize(string(window.getTitle()));
        if (isKnownOverviewTitle(title)) {
            return true;
        }
        String packageName = string(root.getPackageName());
        for (String candidate : getLocalizedOverviewTitles(context, packageName)) {
            if (title.equals(candidate)) {
                return true;
            }
        }
        // Starszy Quickstep zachowuje w Ostatnich ogólny tytuł launchera.
        // Akceptujemy jawny, widoczny kontener Ostatnich, nigdy sam pakiet launchera.
        return hasVisibleOverviewContainer(root, new int[]{NODE_BUDGET});
    }

    private static boolean hasVisibleOverviewContainer(
            AccessibilityNodeInfo node, int[] remaining) {
        if (node == null || remaining[0]-- <= 0) {
            return false;
        }
        String id = lower(node.getViewIdResourceName());
        if (node.isVisibleToUser()
                && (isResourceId(id, "overview_panel") || isResourceId(id, "recents_view"))) {
            return true;
        }
        for (int i = 0; i < node.getChildCount() && remaining[0] > 0; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) {
                continue;
            }
            try {
                if (hasVisibleOverviewContainer(child, remaining)) {
                    return true;
                }
            } finally {
                child.recycle();
            }
        }
        return false;
    }

    private static boolean isKnownOverviewTitle(String title) {
        return title.equals(normalize("Recent apps"))
                || title.equals(normalize("Recent applications"))
                || title.equals(normalize("Recents"))
                || title.equals(normalize("Overview"))
                || title.equals(normalize("Ostatnie aplikacje"))
                || title.equals(normalize("Ostatnio otwarte aplikacje"));
    }

    @SuppressLint("DiscouragedApi")
    private static List<String> getLocalizedOverviewTitles(Context context, String packageName) {
        if (context == null || TextUtils.isEmpty(packageName)) {
            return java.util.Collections.emptyList();
        }
        String locales = context.getResources().getConfiguration().getLocales().toLanguageTags();
        String key = packageName + "|" + locales;
        synchronized (overviewTitleCache) {
            List<String> cached = overviewTitleCache.get(key);
            if (cached != null) {
                return cached;
            }
        }

        ArrayList<String> titles = new ArrayList<>();
        try {
            Resources resources = context.getPackageManager()
                    .getResourcesForApplication(packageName);
            for (String resourceName : OVERVIEW_TITLE_RESOURCE_NAMES) {
                int id = resources.getIdentifier(resourceName, "string", packageName);
                if (id != 0) {
                    String value = normalize(resources.getText(id).toString());
                    if (!TextUtils.isEmpty(value) && !titles.contains(value)) {
                        titles.add(value);
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException | Resources.NotFoundException ignored) {
            // The explicit Polish and English labels above still provide a safe fallback.
        }

        synchronized (overviewTitleCache) {
            overviewTitleCache.put(key, titles);
        }
        return titles;
    }

    private static int scoreTree(AccessibilityNodeInfo node, int[] remaining) {
        if (node == null || remaining[0]-- <= 0) {
            return 0;
        }
        int score = scoreNode(node);
        if (score >= 5) {
            return score;
        }
        for (int i = 0; i < node.getChildCount() && remaining[0] > 0; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) {
                continue;
            }
            try {
                score += scoreTree(child, remaining);
                if (score >= 5) {
                    return score;
                }
            } finally {
                child.recycle();
            }
        }
        return score;
    }

    private static int scoreNode(AccessibilityNodeInfo node) {
        if (!node.isVisibleToUser()) {
            return 0;
        }
        String id = lower(node.getViewIdResourceName());
        int score = 0;
        if (isResourceId(id, "overview_panel") || id.contains("overview_panel_container")) {
            score += 4;
        }
        if (id.contains("overview_actions") || id.contains("recents_view")) {
            score += 3;
        }
        if (id.contains("task_view") || isResourceId(id, "snapshot")) {
            score += 2;
        }
        if (id.contains("clear_all") || id.contains("dismiss_all")) {
            score += 4;
        }
        String label = normalize(string(node.getText()) + " " + string(node.getContentDescription()));
        if (isClearLabel(label)) {
            score += 4;
        } else if (label.equals(normalize("Screenshot"))
                || label.equals(normalize("Zrzut ekranu"))
                || label.equals(normalize("Select"))
                || label.equals(normalize("Wybierz"))) {
            score += 1;
        }
        return score;
    }

    private static AccessibilityNodeInfo findClearNode(
            AccessibilityNodeInfo node, int[] remaining, boolean revealOnly) {
        if (node == null || remaining[0]-- <= 0) {
            return null;
        }
        String id = lower(node.getViewIdResourceName());
        String label = normalize(string(node.getText()) + " " + string(node.getContentDescription()));
        boolean matches = revealOnly
                ? isResourceId(id, "clear_all")
                    && node.getActionList().contains(
                            AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN)
                : node.isVisibleToUser()
                    && (id.contains("clear_all") || id.contains("dismiss_all")
                    || isClearLabel(label));
        if (node.isEnabled() && matches) {
            return AccessibilityNodeInfo.obtain(node);
        }
        for (int i = 0; i < node.getChildCount() && remaining[0] > 0; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) {
                continue;
            }
            try {
                AccessibilityNodeInfo result = findClearNode(child, remaining, revealOnly);
                if (result != null) {
                    return result;
                }
            } finally {
                child.recycle();
            }
        }
        return null;
    }

    private static boolean containsTaskCard(AccessibilityNodeInfo node, int[] remaining) {
        if (node == null || remaining[0]-- <= 0) {
            return false;
        }
        if (node.isVisibleToUser()) {
            String id = lower(node.getViewIdResourceName());
            if (isResourceId(id, "task") || id.contains("task_view")
                    || isResourceId(id, "snapshot")) {
                return true;
            }
            if (isResourceId(id, "overview_panel")) {
                AccessibilityNodeInfo.CollectionInfo collection = node.getCollectionInfo();
                if (collection != null
                        && (collection.getColumnCount() > 0 || collection.getRowCount() > 1)) {
                    return true;
                }
            }
        }
        for (int i = 0; i < node.getChildCount() && remaining[0] > 0; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) {
                continue;
            }
            try {
                if (containsTaskCard(child, remaining)) {
                    return true;
                }
            } finally {
                child.recycle();
            }
        }
        return false;
    }

    private static boolean isResourceId(String value, String entryName) {
        return value.endsWith(":id/" + entryName) || value.endsWith("/id/" + entryName);
    }

    private static boolean isClearLabel(String normalized) {
        if (TextUtils.isEmpty(normalized)) {
            return false;
        }
        return normalized.equals(normalize("Wyczyść wszystko"))
                || normalized.equals(normalize("Wyczyść wszystkie"))
                || normalized.equals(normalize("Zamknij wszystkie"))
                || normalized.equals(normalize("Clear all"))
                || normalized.equals(normalize("Close all"))
                || normalized.equals(normalize("Dismiss all"));
    }

    private static boolean hasScrollAction(AccessibilityNodeInfo node) {
        for (AccessibilityNodeInfo.AccessibilityAction action : node.getActionList()) {
            int id = action.getId();
            if (id == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                    || id == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                    || id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_LEFT.getId()
                    || id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT.getId()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInspectableWindow(AccessibilityWindowInfo window) {
        return window != null && window.getType() != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY;
    }

    private static void recycleQueue(ArrayDeque<AccessibilityNodeInfo> queue) {
        while (!queue.isEmpty()) {
            queue.removeFirst().recycle();
        }
    }

    private static String normalize(String value) {
        String decomposed = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        return decomposed.replaceAll("[^\\p{L}\\p{Nd}]", "");
    }

    private static String lower(CharSequence value) {
        return string(value).toLowerCase(Locale.ROOT);
    }

    private static String string(CharSequence value) {
        return value == null ? "" : value.toString();
    }
}
