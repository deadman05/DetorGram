package org.telegram.messenger;

import android.app.Activity;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DetorConfig {

    private static SharedPreferences prefs;
    private static Set<String> deletedMessagesCache;

    private static SharedPreferences getPrefs() {
        if (prefs == null) {
            prefs = ApplicationLoader.applicationContext.getSharedPreferences("detor_config", Activity.MODE_PRIVATE);
        }
        return prefs;
    }

    // --- 1. Privacy & Ghost Mode ---
    public static boolean isHideSeen() { return getPrefs().getBoolean("hide_seen", false); }
    public static void setHideSeen(boolean v) { getPrefs().edit().putBoolean("hide_seen", v).apply(); }

    public static boolean isHideTyping() { return getPrefs().getBoolean("hide_typing", false); }
    public static void setHideTyping(boolean v) { getPrefs().edit().putBoolean("hide_typing", v).apply(); }

    public static boolean isAlwaysOffline() { return getPrefs().getBoolean("always_offline", false); }
    public static void setAlwaysOffline(boolean v) { getPrefs().edit().putBoolean("always_offline", v).apply(); }

    public static boolean isAnonStoryView() { return getPrefs().getBoolean("anon_story_view", false); }
    public static void setAnonStoryView(boolean v) { getPrefs().edit().putBoolean("anon_story_view", v).apply(); }

    // --- 2. Anti-Delete & Media ---
    public static boolean isAntiDelete() { return getPrefs().getBoolean("anti_delete", true); }
    public static void setAntiDelete(boolean v) { getPrefs().edit().putBoolean("anti_delete", v).apply(); }

    public static boolean isSaveRestricted() { return getPrefs().getBoolean("save_restricted", true); }
    public static void setSaveRestricted(boolean v) { getPrefs().edit().putBoolean("save_restricted", v).apply(); }

    public static boolean isViewOnceSave() { return getPrefs().getBoolean("view_once_save", true); }
    public static void setViewOnceSave(boolean v) { getPrefs().edit().putBoolean("view_once_save", v).apply(); }

    public static boolean isHideSponsored() { return getPrefs().getBoolean("hide_sponsored", true); }
    public static void setHideSponsored(boolean v) { getPrefs().edit().putBoolean("hide_sponsored", v).apply(); }

    // --- 3. Power-User & Utility ---
    public static boolean isShowIdDc() { return getPrefs().getBoolean("show_id_dc", true); }
    public static void setShowIdDc(boolean v) { getPrefs().edit().putBoolean("show_id_dc", v).apply(); }

    public static boolean isConfirmCall() { return getPrefs().getBoolean("confirm_call", true); }
    public static void setConfirmCall(boolean v) { getPrefs().edit().putBoolean("confirm_call", v).apply(); }

    public static boolean isHideStoriesBar() { return getPrefs().getBoolean("hide_stories_bar", false); }
    public static void setHideStoriesBar(boolean v) { getPrefs().edit().putBoolean("hide_stories_bar", v).apply(); }

    public static boolean isForwardNoQuote() { return getPrefs().getBoolean("forward_no_quote", false); }
    public static void setForwardNoQuote(boolean v) { getPrefs().edit().putBoolean("forward_no_quote", v).apply(); }

    public static boolean isShowChatFab() { return getPrefs().getBoolean("show_chat_fab", true); }
    public static void setShowChatFab(boolean v) { getPrefs().edit().putBoolean("show_chat_fab", v).apply(); }

    public static boolean isMultiStoryUnlock() { return getPrefs().getBoolean("multi_story_unlock", true); }
    public static void setMultiStoryUnlock(boolean v) { getPrefs().edit().putBoolean("multi_story_unlock", v).apply(); }

    // --- Quick Saved Templates ---
    private static final String TPL_SEP = "\n<<<DETOR_TPL>>>\n";

    public static java.util.List<String> getTemplates() {
        String raw = getPrefs().getString("quick_templates", "");
        java.util.ArrayList<String> list = new java.util.ArrayList<>();
        if (raw != null && !raw.isEmpty()) {
            String[] parts = raw.split(java.util.regex.Pattern.quote(TPL_SEP));
            for (String p : parts) {
                if (!p.isEmpty()) list.add(p);
            }
        }
        return list;
    }

    public static void addTemplate(String text) {
        java.util.List<String> list = getTemplates();
        if (!list.contains(text)) {
            list.add(0, text);
            saveTemplates(list);
        }
    }

    public static void removeTemplate(String text) {
        java.util.List<String> list = getTemplates();
        if (list.remove(text)) {
            saveTemplates(list);
        }
    }

    private static void saveTemplates(java.util.List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(TPL_SEP);
            sb.append(list.get(i));
        }
        getPrefs().edit().putString("quick_templates", sb.toString()).apply();
    }

    // --- Deleted Messages Tracker ("deleted" tag ke liye) ---
    private static synchronized void ensureDeletedCache() {
        if (deletedMessagesCache == null) {
            Set<String> saved = getPrefs().getStringSet("deleted_msgs", Collections.emptySet());
            deletedMessagesCache = new HashSet<>(saved);
        }
    }

    public static synchronized void markMessageDeleted(int account, long channelId, int msgId) {
        ensureDeletedCache();
        String key = account + "_" + channelId + "_" + msgId;
        if (deletedMessagesCache.size() > 5000) {
            deletedMessagesCache.clear();
        }
        if (deletedMessagesCache.add(key)) {
            getPrefs().edit().putStringSet("deleted_msgs", new HashSet<>(deletedMessagesCache)).apply();
        }
    }

    public static synchronized boolean isMessageDeleted(int account, long dialogId, int msgId) {
        ensureDeletedCache();
        long channelId = dialogId < 0 ? -dialogId : 0;
        if (deletedMessagesCache.contains(account + "_" + channelId + "_" + msgId)) {
            return true;
        }
        return deletedMessagesCache.contains(account + "_0_" + msgId);
    }
}
