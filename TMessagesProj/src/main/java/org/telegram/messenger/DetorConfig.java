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

    private static String accKey(int account, String key) {
        long uid = UserConfig.getInstance(account).getClientUserId();
        return (uid != 0 ? ("u" + uid + "_") : ("a" + account + "_")) + key;
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

    public static boolean isHideStoriesBar() { return getPrefs().getBoolean("hide_stories_bar_v2", false); }
    public static void setHideStoriesBar(boolean v) {
        getPrefs().edit().putBoolean("hide_stories_bar_v2", v).apply();
        try {
            NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(NotificationCenter.storiesUpdated);
        } catch (Throwable ignored) {}
    }

    public static boolean isForwardNoQuote() { return getPrefs().getBoolean("forward_no_quote", false); }
    public static void setForwardNoQuote(boolean v) { getPrefs().edit().putBoolean("forward_no_quote", v).apply(); }

    public static boolean isShowChatFab() { return getPrefs().getBoolean("show_chat_fab", true); }
    public static void setShowChatFab(boolean v) { getPrefs().edit().putBoolean("show_chat_fab", v).apply(); }

    public static boolean isMultiStoryUnlock() { return getPrefs().getBoolean("multi_story_unlock", true); }
    public static void setMultiStoryUnlock(boolean v) { getPrefs().edit().putBoolean("multi_story_unlock", v).apply(); }

    // --- 4. Auto-Reply (Per Account) ---
    public static boolean isAutoReplyEnabled(int account) { return getPrefs().getBoolean(accKey(account, "ar_enabled"), false); }
    public static void setAutoReplyEnabled(int account, boolean v) { getPrefs().edit().putBoolean(accKey(account, "ar_enabled"), v).apply(); }

    public static boolean isAutoReplyBgOnly(int account) { return getPrefs().getBoolean(accKey(account, "ar_bg_only"), true); }
    public static void setAutoReplyBgOnly(int account, boolean v) { getPrefs().edit().putBoolean(accKey(account, "ar_bg_only"), v).apply(); }

    public static boolean isAutoReplyNonContactsOnly(int account) { return getPrefs().getBoolean(accKey(account, "ar_non_contacts"), false); }
    public static void setAutoReplyNonContactsOnly(int account, boolean v) { getPrefs().edit().putBoolean(accKey(account, "ar_non_contacts"), v).apply(); }

    public static String getAutoReplyText(int account) {
        return getPrefs().getString(accKey(account, "ar_text"), "Hey! I'm currently offline/busy. I'll reply as soon as I'm back.");
    }
    public static void setAutoReplyText(int account, String s) { getPrefs().edit().putString(accKey(account, "ar_text"), s).apply(); }

    public static int getAutoReplyCooldownMin(int account) { return getPrefs().getInt(accKey(account, "ar_cooldown_min"), 30); }
    public static void setAutoReplyCooldownMin(int account, int m) { getPrefs().edit().putInt(accKey(account, "ar_cooldown_min"), m).apply(); }

    // --- 5. Smart Anti-Spam (Per Account: 3 Warnings -> Auto-Block) ---
    public static boolean isAntiSpamEnabled(int account) { return getPrefs().getBoolean(accKey(account, "as_enabled"), false); }
    public static void setAntiSpamEnabled(int account, boolean v) { getPrefs().edit().putBoolean(accKey(account, "as_enabled"), v).apply(); }

    public static boolean isAntiSpamIgnoreContacts(int account) { return getPrefs().getBoolean(accKey(account, "as_ignore_contacts"), true); }
    public static void setAntiSpamIgnoreContacts(int account, boolean v) { getPrefs().edit().putBoolean(accKey(account, "as_ignore_contacts"), v).apply(); }

    public static boolean isAntiSpamLinkDetect(int account) { return getPrefs().getBoolean(accKey(account, "as_link_detect"), true); }
    public static void setAntiSpamLinkDetect(int account, boolean v) { getPrefs().edit().putBoolean(accKey(account, "as_link_detect"), v).apply(); }

    public static boolean isAntiSpamRepeatDetect(int account) { return getPrefs().getBoolean(accKey(account, "as_repeat_detect"), true); }
    public static void setAntiSpamRepeatDetect(int account, boolean v) { getPrefs().edit().putBoolean(accKey(account, "as_repeat_detect"), v).apply(); }

    public static int getAntiSpamThreshold(int account) { return getPrefs().getInt(accKey(account, "as_threshold"), 5); }
    public static void setAntiSpamThreshold(int account, int n) { getPrefs().edit().putInt(accKey(account, "as_threshold"), n).apply(); }

    public static String getAntiSpamWarnMsg(int account) {
        return getPrefs().getString(accKey(account, "as_warn_msg"), "⚠️ Warning {count}/3 ({reason}): Please stop spamming or you will be automatically blocked.");
    }
    public static void setAntiSpamWarnMsg(int account, String s) { getPrefs().edit().putString(accKey(account, "as_warn_msg"), s).apply(); }

    public static String getAntiSpamBlockMsg(int account) {
        return getPrefs().getString(accKey(account, "as_block_msg"), "🚫 Warning 3/3 ({reason}): Spam limit exceeded. You have been automatically blocked.");
    }
    public static void setAntiSpamBlockMsg(int account, String s) { getPrefs().edit().putString(accKey(account, "as_block_msg"), s).apply(); }

    public static String getGuardWhitelist(int account) { return getPrefs().getString(accKey(account, "guard_whitelist"), ""); }
    public static void setGuardWhitelist(int account, String s) { getPrefs().edit().putString(accKey(account, "guard_whitelist"), s).apply(); }

    public static boolean isWhitelisted(int account, long userId) {
        String raw = getGuardWhitelist(account);
        if (raw == null || raw.trim().isEmpty()) return false;
        String target = String.valueOf(userId);
        for (String p : raw.split("[,\\s]+")) {
            if (target.equals(p.trim())) return true;
        }
        return false;
    }

    private static final String LOG_SEP = "\n<<<DETOR_LOG>>>\n";

    public static java.util.List<String> getSpamLog(int account) {
        String raw = getPrefs().getString(accKey(account, "as_block_log"), "");
        java.util.ArrayList<String> list = new java.util.ArrayList<>();
        if (raw != null && !raw.isEmpty()) {
            for (String p : raw.split(java.util.regex.Pattern.quote(LOG_SEP))) {
                if (!p.isEmpty()) list.add(p);
            }
        }
        return list;
    }

    public static void addSpamLog(int account, String entry) {
        java.util.List<String> list = getSpamLog(account);
        list.add(0, entry);
        while (list.size() > 50) list.remove(list.size() - 1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(LOG_SEP);
            sb.append(list.get(i));
        }
        getPrefs().edit().putString(accKey(account, "as_block_log"), sb.toString()).apply();
    }

    public static void clearSpamLog(int account) {
        getPrefs().edit().remove(accKey(account, "as_block_log")).apply();
    }

    // Backward-compatible fallbacks (prevents compile errors if any class uses 0-arg calls)
    public static boolean isAutoReplyEnabled() { return isAutoReplyEnabled(UserConfig.selectedAccount); }
    public static void setAutoReplyEnabled(boolean v) { setAutoReplyEnabled(UserConfig.selectedAccount, v); }
    public static boolean isAutoReplyBgOnly() { return isAutoReplyBgOnly(UserConfig.selectedAccount); }
    public static void setAutoReplyBgOnly(boolean v) { setAutoReplyBgOnly(UserConfig.selectedAccount, v); }
    public static boolean isAutoReplyNonContactsOnly() { return isAutoReplyNonContactsOnly(UserConfig.selectedAccount); }
    public static void setAutoReplyNonContactsOnly(boolean v) { setAutoReplyNonContactsOnly(UserConfig.selectedAccount, v); }
    public static String getAutoReplyText() { return getAutoReplyText(UserConfig.selectedAccount); }
    public static void setAutoReplyText(String s) { setAutoReplyText(UserConfig.selectedAccount, s); }
    public static int getAutoReplyCooldownMin() { return getAutoReplyCooldownMin(UserConfig.selectedAccount); }
    public static void setAutoReplyCooldownMin(int m) { setAutoReplyCooldownMin(UserConfig.selectedAccount, m); }
    public static boolean isAntiSpamEnabled() { return isAntiSpamEnabled(UserConfig.selectedAccount); }
    public static void setAntiSpamEnabled(boolean v) { setAntiSpamEnabled(UserConfig.selectedAccount, v); }
    public static boolean isAntiSpamIgnoreContacts() { return isAntiSpamIgnoreContacts(UserConfig.selectedAccount); }
    public static void setAntiSpamIgnoreContacts(boolean v) { setAntiSpamIgnoreContacts(UserConfig.selectedAccount, v); }
    public static boolean isAntiSpamLinkDetect() { return isAntiSpamLinkDetect(UserConfig.selectedAccount); }
    public static void setAntiSpamLinkDetect(boolean v) { setAntiSpamLinkDetect(UserConfig.selectedAccount, v); }
    public static boolean isAntiSpamRepeatDetect() { return isAntiSpamRepeatDetect(UserConfig.selectedAccount); }
    public static void setAntiSpamRepeatDetect(boolean v) { setAntiSpamRepeatDetect(UserConfig.selectedAccount, v); }
    public static int getAntiSpamThreshold() { return getAntiSpamThreshold(UserConfig.selectedAccount); }
    public static void setAntiSpamThreshold(int n) { setAntiSpamThreshold(UserConfig.selectedAccount, n); }
    public static String getAntiSpamWarnMsg() { return getAntiSpamWarnMsg(UserConfig.selectedAccount); }
    public static void setAntiSpamWarnMsg(String s) { setAntiSpamWarnMsg(UserConfig.selectedAccount, s); }
    public static String getAntiSpamBlockMsg() { return getAntiSpamBlockMsg(UserConfig.selectedAccount); }
    public static void setAntiSpamBlockMsg(String s) { setAntiSpamBlockMsg(UserConfig.selectedAccount, s); }
    public static String getGuardWhitelist() { return getGuardWhitelist(UserConfig.selectedAccount); }
    public static void setGuardWhitelist(String s) { setGuardWhitelist(UserConfig.selectedAccount, s); }
    public static boolean isWhitelisted(long userId) { return isWhitelisted(UserConfig.selectedAccount, userId); }
    public static java.util.List<String> getSpamLog() { return getSpamLog(UserConfig.selectedAccount); }
    public static void addSpamLog(String entry) { addSpamLog(UserConfig.selectedAccount, entry); }
    public static void clearSpamLog() { clearSpamLog(UserConfig.selectedAccount); }

    // --- 6. Quick Saved Templates ---
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

    // --- 7. Deleted Messages Tracker ("deleted" tag ke liye) ---
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
