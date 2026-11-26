package org.telegram.messenger;

import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public class DetorAutoGuard {

    private static class SpamTracker {
        final ArrayDeque<Long> recentTimes = new ArrayDeque<>();
        final ArrayDeque<Long> mediaTimes = new ArrayDeque<>();
        String lastText = "";
        int repeatCount = 0;
        int warnings = 0;
        long lastWarnMs = 0;
    }

    private static final ConcurrentHashMap<String, SpamTracker> spamMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> autoReplyLastMs = new ConcurrentHashMap<>();

    public static void clearSpamMemory(int account) {
        String prefix = account + ":";
        for (String k : new ArrayList<>(spamMap.keySet())) {
            if (k.startsWith(prefix)) spamMap.remove(k);
        }
        for (String k : new ArrayList<>(autoReplyLastMs.keySet())) {
            if (k.startsWith(prefix)) autoReplyLastMs.remove(k);
        }
    }

    public static void onNewMessages(int account, long dialogId, ArrayList<MessageObject> messages) {
        try {
            if (messages == null || messages.isEmpty()) return;
            if (!DetorConfig.isAutoReplyEnabled(account) && !DetorConfig.isAntiSpamEnabled(account)) return;
            if (!DialogObject.isUserDialog(dialogId) || dialogId <= 0) return;

            long myId = UserConfig.getInstance(account).getClientUserId();
            if (dialogId == myId || dialogId == 777000L) return;
            if (DetorConfig.isWhitelisted(account, dialogId)) return;

            MessagesController mc = MessagesController.getInstance(account);
            TLRPC.User user = mc.getUser(dialogId);
            if (user == null || user.bot || user.support || user.verified || user.self) return;

            boolean isContact = user.contact || user.mutual_contact;
            int nowSec = ConnectionsManager.getInstance(account).getCurrentTime();

            MessageObject latestFresh = null;
            for (int i = 0; i < messages.size(); i++) {
                MessageObject msg = messages.get(i);
                if (msg == null || msg.messageOwner == null || msg.isOutOwner()) continue;
                if (msg.messageOwner instanceof TLRPC.TL_messageService) continue;
                if (nowSec > 0 && msg.messageOwner.date > 0 && (nowSec - msg.messageOwner.date) > 90) continue;

                latestFresh = msg;
                if (DetorConfig.isAntiSpamEnabled(account)) {
                    if (checkAntiSpam(account, dialogId, user, isContact, msg)) {
                        return;
                    }
                }
            }

            if (latestFresh != null && DetorConfig.isAutoReplyEnabled(account)) {
                checkAutoReply(account, dialogId, isContact);
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean checkAntiSpam(int account, long dialogId, TLRPC.User user, boolean isContact, MessageObject msg) {
        if (DetorConfig.isAntiSpamIgnoreContacts(account) && isContact) {
            return false;
        }

        long now = System.currentTimeMillis();
        String key = account + ":" + dialogId;
        SpamTracker tracker = spamMap.get(key);
        if (tracker == null) {
            tracker = new SpamTracker();
            spamMap.put(key, tracker);
        }

        if (tracker.lastWarnMs > 0 && (now - tracker.lastWarnMs) > 12L * 60 * 60 * 1000) {
            tracker.warnings = 0;
        }

        // 1. Flood rate detection (window: 8 sec)
        tracker.recentTimes.addLast(now);
        while (!tracker.recentTimes.isEmpty() && (now - tracker.recentTimes.peekFirst()) > 8000L) {
            tracker.recentTimes.removeFirst();
        }
        int threshold = Math.max(3, DetorConfig.getAntiSpamThreshold(account));
        String reason = null;
        if (tracker.recentTimes.size() >= threshold) {
            reason = "Flood (" + tracker.recentTimes.size() + " msgs/8s)";
            tracker.recentTimes.clear();
        }

        // 2. Repeated identical text detection
        String text = msg.messageText != null ? msg.messageText.toString().trim() : "";
        if (reason == null && DetorConfig.isAntiSpamRepeatDetect(account) && text.length() >= 2) {
            if (text.equalsIgnoreCase(tracker.lastText)) {
                tracker.repeatCount++;
                if (tracker.repeatCount >= 3) {
                    reason = "Repeated text spam";
                    tracker.repeatCount = 0;
                }
            } else {
                tracker.lastText = text;
                tracker.repeatCount = 1;
            }
        }

        // 3. Sticker / GIF / Forward burst
        if (reason == null && (msg.isSticker() || msg.isAnimatedSticker() || msg.isGif() || msg.isForwarded())) {
            tracker.mediaTimes.addLast(now);
            while (!tracker.mediaTimes.isEmpty() && (now - tracker.mediaTimes.peekFirst()) > 8000L) {
                tracker.mediaTimes.removeFirst();
            }
            if (tracker.mediaTimes.size() >= Math.max(3, threshold - 1)) {
                reason = "Media/Forward flood";
                tracker.mediaTimes.clear();
            }
        }

        // 4. Promo / Link spam from non-contacts
        if (reason == null && DetorConfig.isAntiSpamLinkDetect(account) && !isContact && !text.isEmpty()) {
            String low = text.toLowerCase(Locale.US);
            if (low.contains("t.me/") || low.contains("http://") || low.contains("https://")
                    || low.contains("bit.ly/") || low.contains("wa.me/") || low.contains("joinchat")) {
                reason = "Unsolicited link spam";
            }
        }

        if (reason == null) {
            return false;
        }

        if (now - tracker.lastWarnMs < 4000L) {
            return true;
        }

        tracker.warnings++;
        tracker.lastWarnMs = now;
        final int strike = tracker.warnings;
        final String finalReason = reason;
        final String userName = UserObject.getUserName(user);

        if (strike < 3) {
            String warnText = DetorConfig.getAntiSpamWarnMsg(account)
                    .replace("{count}", String.valueOf(strike))
                    .replace("{reason}", finalReason);
            sendDelayedText(account, dialogId, warnText, 500L);
        } else {
            String blockText = DetorConfig.getAntiSpamBlockMsg(account)
                    .replace("{reason}", finalReason);
            sendDelayedText(account, dialogId, blockText, 400L);
            AndroidUtilities.runOnUIThread(() -> {
                try {
                    MessagesController.getInstance(account).blockPeer(dialogId);
                    String time = new SimpleDateFormat("dd MMM HH:mm", Locale.US).format(new Date());
                    DetorConfig.addSpamLog(account, time + " • " + userName + " (" + dialogId + ") — " + finalReason);
                    spamMap.remove(key);
                } catch (Throwable ignored) {
                }
            }, 1100L);
        }
        return true;
    }

    private static void checkAutoReply(int account, long dialogId, boolean isContact) {
        if (DetorConfig.isAutoReplyBgOnly(account) && !ApplicationLoader.mainInterfacePaused) {
            return;
        }
        if (DetorConfig.isAutoReplyNonContactsOnly(account) && isContact) {
            return;
        }

        long now = System.currentTimeMillis();
        long cooldownMs = Math.max(1, DetorConfig.getAutoReplyCooldownMin(account)) * 60_000L;
        String key = account + ":" + dialogId;
        Long last = autoReplyLastMs.get(key);
        if (last != null && (now - last) < cooldownMs) {
            return;
        }

        String replyText = DetorConfig.getAutoReplyText(account);
        if (TextUtils.isEmpty(replyText)) return;

        autoReplyLastMs.put(key, now);
        sendDelayedText(account, dialogId, replyText, 700L);
    }

    private static void sendDelayedText(int account, long dialogId, String text, long delayMs) {
        AndroidUtilities.runOnUIThread(() -> {
            try {
                SendMessagesHelper.getInstance(account).sendMessage(
                        SendMessagesHelper.SendMessageParams.of(
                                text, dialogId, null, null, null, false,
                                null, null, null, true, 0, 0, null, false
                        )
                );
            } catch (Throwable ignored) {
            }
        }, delayMs);
    }

    // ==================== RESTRICTED FORWARD AUTO RE-SEND ====================

    public static boolean isRawRestricted(int account, MessageObject msgObj) {
        if (msgObj == null || msgObj.messageOwner == null) return false;
        if (msgObj.messageOwner.noforwards) return true;
        long srcDid = msgObj.getDialogId();
        MessagesController mc = MessagesController.getInstance(account);
        if (srcDid < 0) {
            TLRPC.Chat c = mc.getChat(-srcDid);
            return c != null && c.noforwards;
        } else if (srcDid > 0) {
            TLRPC.UserFull uf = mc.getUserFull(srcDid);
            return uf != null && (uf.noforwards_peer_enabled || uf.noforwards_my_enabled);
        }
        return false;
    }

    public static void resendRestrictedMedia(int account, long peer, MessageObject msgObj, boolean hideCaption, boolean notify, int scheduleDate) {
        try {
            File file = FileLoader.getInstance(account).getPathToMessage(msgObj.messageOwner);
            if ((file == null || !file.exists() || file.length() == 0) && !TextUtils.isEmpty(msgObj.messageOwner.attachPath)) {
                file = new File(msgObj.messageOwner.attachPath);
            }
            if (file == null || !file.exists() || file.length() == 0) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (ApplicationLoader.applicationContext != null) {
                        Toast.makeText(ApplicationLoader.applicationContext,
                                "DetorGram: Pehle media/file download karein, fir Forward karein!",
                                Toast.LENGTH_LONG).show();
                    }
                });
                return;
            }

            final String path = file.getAbsolutePath();
            final String caption = (!hideCaption && msgObj.caption != null) ? msgObj.caption.toString() : null;
            final ArrayList<TLRPC.MessageEntity> entities = (!hideCaption && msgObj.messageOwner != null) ? msgObj.messageOwner.entities : null;
            final String mime = (msgObj.getDocument() != null && !TextUtils.isEmpty(msgObj.getDocument().mime_type))
                    ? msgObj.getDocument().mime_type
                    : (msgObj.isPhoto() ? "image/jpeg" : "application/octet-stream");

            AccountInstance ai = AccountInstance.getInstance(account);
            boolean invoked = false;

            if (msgObj.isPhoto()) {
                invoked = invokePreparePhoto(ai, path, peer, caption, entities, notify, scheduleDate);
            }
            if (!invoked) {
                invoked = invokePrepareDocument(ai, path, mime, peer, caption, entities, notify, scheduleDate);
            }
            if (!invoked) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (ApplicationLoader.applicationContext != null) {
                        Toast.makeText(ApplicationLoader.applicationContext,
                                "Saved locally: " + path, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    private static boolean invokePreparePhoto(AccountInstance ai, String path, long peer, String caption,
                                              ArrayList<TLRPC.MessageEntity> entities, boolean notify, int scheduleDate) {
        try {
            for (Method m : SendMessagesHelper.class.getDeclaredMethods()) {
                if (!m.getName().equals("prepareSendingPhoto")) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length >= 6 && p[0] == AccountInstance.class && p[1] == String.class && p[2] == Uri.class && p[3] == long.class) {
                    m.setAccessible(true);
                    Object[] args = new Object[p.length];
                    args[0] = ai;
                    args[1] = path;
                    args[2] = null;
                    args[3] = peer;
                    boolean captionSet = false;
                    boolean entitiesSet = false;
                    for (int i = 4; i < p.length; i++) {
                        if (p[i] == CharSequence.class || p[i] == String.class) {
                            args[i] = !captionSet ? caption : null;
                            captionSet = true;
                        } else if (p[i] == ArrayList.class && !entitiesSet) {
                            args[i] = entities;
                            entitiesSet = true;
                        } else if (p[i] == boolean.class) {
                            args[i] = (i == p.length - 4 || i == p.length - 3) ? notify : false;
                        } else if (p[i] == int.class) {
                            args[i] = (i == p.length - 2) ? scheduleDate : 0;
                        } else if (p[i] == long.class) {
                            args[i] = 0L;
                        } else {
                            args[i] = null;
                        }
                    }
                    m.invoke(null, args);
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean invokePrepareDocument(AccountInstance ai, String path, String mime, long peer, String caption,
                                                 ArrayList<TLRPC.MessageEntity> entities, boolean notify, int scheduleDate) {
        try {
            for (Method m : SendMessagesHelper.class.getDeclaredMethods()) {
                if (!m.getName().equals("prepareSendingDocument")) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length >= 7 && p[0] == AccountInstance.class && p[1] == String.class && p[2] == String.class
                        && p[3] == Uri.class && p[4] == String.class && p[5] == long.class) {
                    m.setAccessible(true);
                    Object[] args = new Object[p.length];
                    args[0] = ai;
                    args[1] = path;
                    args[2] = path;
                    args[3] = null;
                    args[4] = mime;
                    args[5] = peer;
                    boolean captionSet = false;
                    boolean entitiesSet = false;
                    for (int i = 6; i < p.length; i++) {
                        if ((p[i] == CharSequence.class || p[i] == String.class) && !captionSet) {
                            args[i] = caption;
                            captionSet = true;
                        } else if (p[i] == ArrayList.class && !entitiesSet) {
                            args[i] = entities;
                            entitiesSet = true;
                        } else if (p[i] == boolean.class) {
                            args[i] = notify;
                        } else if (p[i] == int.class) {
                            args[i] = scheduleDate;
                        } else if (p[i] == long.class) {
                            args[i] = 0L;
                        } else {
                            args[i] = null;
                        }
                    }
                    m.invoke(null, args);
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
