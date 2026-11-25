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

    // --- Auto-Reply (Away Mode) ---
    public static boolean isAutoReplyEnabled() { return getPrefs().getBoolean("ar_enabled", false); }
    public static void setAutoReplyEnabled(boolean v) { getPrefs().edit().putBoolean("ar_enabled", v).apply(); }

    public static boolean isAutoReplyBgOnly() { return getPrefs().getBoolean("ar_bg_only", true); }
    public static void setAutoReplyBgOnly(boolean v) { getPrefs().edit().putBoolean("ar_bg_only", v).apply(); }

    public static boolean isAutoReplyNonContactsOnly() { return getPrefs().getBoolean("ar_non_contacts", false); }
    public static void setAutoReplyNonContactsOnly(boolean v) { getPrefs().edit().putBoolean("ar_non_contacts", v).apply(); }

    public static String getAutoReplyText() {
        return getPrefs().getString("ar_text", "Hey! I'm currently offline/busy. I'll reply as soon as I'm back.");
    }
    public static void setAutoReplyText(String s) { getPrefs().edit().putString("ar_text", s).apply(); }

    public static int getAutoReplyCooldownMin() { return getPrefs().getInt("ar_cooldown_min", 30); }
    public static void setAutoReplyCooldownMin(int m) { getPrefs().edit().putInt("ar_cooldown_min", m).apply(); }

    // --- Smart Anti-Spam (3 Warnings -> Auto-Block) ---
    public static boolean isAntiSpamEnabled() { return getPrefs().getBoolean("as_enabled", false); }
    public static void setAntiSpamEnabled(boolean v) { getPrefs().edit().putBoolean("as_enabled", v).apply(); }

    public static boolean isAntiSpamIgnoreContacts() { return getPrefs().getBoolean("as_ignore_contacts", true); }
    public static void setAntiSpamIgnoreContacts(boolean v) { getPrefs().edit().putBoolean("as_ignore_contacts", v).apply(); }

    public static boolean isAntiSpamLinkDetect() { return getPrefs().getBoolean("as_link_detect", true); }
    public static void setAntiSpamLinkDetect(boolean v) { getPrefs().edit().putBoolean("as_link_detect", v).apply(); }

    public static boolean isAntiSpamRepeatDetect() { return getPrefs().getBoolean("as_repeat_detect", true); }
    public static void setAntiSpamRepeatDetect(boolean v) { getPrefs().edit().putBoolean("as_repeat_detect", v).apply(); }

    public static int getAntiSpamThreshold() { return getPrefs().getInt("as_threshold", 5); }
    public static void setAntiSpamThreshold(int n) { getPrefs().edit().putInt("as_threshold", n).apply(); }

    public static String getAntiSpamWarnMsg() {
        return getPrefs().getString("as_warn_msg", "⚠️ Warning {count}/3 ({reason}): Please stop spamming or you will be automatically blocked.");
    }
    public static void setAntiSpamWarnMsg(String s) { getPrefs().edit().putString("as_warn_msg", s).apply(); }

    public static String getAntiSpamBlockMsg() {
        return getPrefs().getString("as_block_msg", "🚫 Warning 3/3 ({reason}): Spam limit exceeded. You have been automatically blocked.");
    }
    public static void setAntiSpamBlockMsg(String s) { getPrefs().edit().putString("as_block_msg", s).apply(); }

    public static String getGuardWhitelist() { return getPrefs().getString("guard_whitelist", ""); }
    public static void setGuardWhitelist(String s) { getPrefs().edit().putString("guard_whitelist", s).apply(); }

    public static boolean isWhitelisted(long userId) {
        String raw = getGuardWhitelist();
        if (raw == null || raw.trim().isEmpty()) return false;
        String target = String.valueOf(userId);
        for (String p : raw.split("[,\\s]+")) {
            if (target.equals(p.trim())) return true;
        }
        return false;
    }

    private static final String LOG_SEP = "\n<<<DETOR_LOG>>>\n";

    public static java.util.List<String> getSpamLog() {
        String raw = getPrefs().getString("as_block_log", "");
        java.util.ArrayList<String> list = new java.util.ArrayList<>();
        if (raw != null && !raw.isEmpty()) {
            for (String p : raw.split(java.util.regex.Pattern.quote(LOG_SEP))) {
                if (!p.isEmpty()) list.add(p);
            }
        }
        return list;
    }

    public static void addSpamLog(String entry) {
        java.util.List<String> list = getSpamLog();
        list.add(0, entry);
        while (list.size() > 50) list.remove(list.size() - 1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(LOG_SEP);
            sb.append(list.get(i));
        }
        getPrefs().edit().putString("as_block_log", sb.toString()).apply();
    }

    public static void clearSpamLog() {
        getPrefs().edit().remove("as_block_log").apply();
    }

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
