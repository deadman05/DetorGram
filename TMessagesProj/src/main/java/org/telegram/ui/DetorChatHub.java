package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DetorConfig;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class DetorChatHub {

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final WeakHashMap<ChatActivity, TextView> fabMap = new WeakHashMap<>();
    private static final HashMap<Long, ActiveJob> activeJobs = new HashMap<>();

    private static class ActiveJob {
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        Runnable pendingRunnable;
        int sent;
        int total;
        String label;

        void cancel() {
            cancelled.set(true);
            if (pendingRunnable != null) {
                mainHandler.removeCallbacks(pendingRunnable);
                pendingRunnable = null;
            }
        }
    }

    public static void attach(final ChatActivity chat, final ViewGroup parent) {
        if (chat == null || parent == null || chat.isInPreviewMode() || chat.isInsideContainer) {
            return;
        }
        Context context = chat.getParentActivity();
        if (context == null) {
            return;
        }

        final TextView fab = new TextView(context);
        fab.setText("⚡");
        fab.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        fab.setTypeface(Typeface.DEFAULT_BOLD);
        fab.setTextColor(Color.WHITE);
        fab.setGravity(Gravity.CENTER);
        fab.setElevation(AndroidUtilities.dp(10));
        applyFabStyle(fab, false, "⚡");

        FrameLayout.LayoutParams lp = LayoutHelper.createFrame(
                48, 48, Gravity.RIGHT | Gravity.BOTTOM, 0, 0, 14, 130
        );
        parent.addView(fab, lp);
        fab.setVisibility(DetorConfig.isShowChatFab() ? View.VISIBLE : View.GONE);
        fabMap.put(chat, fab);
        parent.post(fab::bringToFront);

        final int touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        fab.setOnTouchListener(new View.OnTouchListener() {
            float downRawX, downRawY, startX, startY;
            boolean dragging;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downRawX = event.getRawX();
                        downRawY = event.getRawY();
                        startX = v.getX();
                        startY = v.getY();
                        dragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - downRawX;
                        float dy = event.getRawY() - downRawY;
                        if (!dragging && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                            dragging = true;
                        }
                        if (dragging) {
                            float maxX = Math.max(0, parent.getWidth() - v.getWidth());
                            float maxY = Math.max(0, parent.getHeight() - v.getHeight());
                            v.setX(Math.max(0, Math.min(maxX, startX + dx)));
                            v.setY(Math.max(0, Math.min(maxY, startY + dy)));
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!dragging) {
                            showHubCategories(chat);
                        }
                        return true;
                }
                return false;
            }
        });

        updateFabBadge(chat);
    }

    public static void detach(ChatActivity chat) {
        if (chat == null) return;
        fabMap.remove(chat);
        ActiveJob job = activeJobs.remove(chat.getDialogId());
        if (job != null) {
            job.cancel();
        }
    }

    public static void refreshVisibility(ChatActivity chat) {
        if (chat == null) return;
        TextView fab = fabMap.get(chat);
        if (fab != null) {
            fab.setVisibility(DetorConfig.isShowChatFab() ? View.VISIBLE : View.GONE);
            fab.bringToFront();
        }
    }

    private static void applyFabStyle(TextView fab, boolean running, String text) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(running ? 0xFFE53935 : 0xFF2AABEE);
        bg.setStroke(AndroidUtilities.dp(1.5f), 0x66FFFFFF);
        fab.setBackground(bg);
        fab.setText(text);
        fab.setTextSize(TypedValue.COMPLEX_UNIT_DIP, running ? 11 : 18);
    }

    private static void updateFabBadge(ChatActivity chat) {
        if (chat == null) return;
        TextView fab = fabMap.get(chat);
        if (fab == null) return;
        ActiveJob job = activeJobs.get(chat.getDialogId());
        if (job != null && !job.cancelled.get()) {
            applyFabStyle(fab, true, job.sent + "/" + job.total);
        } else {
            applyFabStyle(fab, false, "⚡");
        }
    }

    // ==================== MAIN HUB (5 CATEGORIES) ====================

    public static void showHubCategories(final ChatActivity chat) {
        Activity activity = chat.getParentActivity();
        if (activity == null) return;

        ScrollView scroll = new ScrollView(activity);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setBackgroundColor(Theme.getColor(Theme.key_dialogBackground));

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(20));
        scroll.addView(root, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        TextView header = new TextView(activity);
        header.setText("DetorGram Chat Hub");
        header.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        header.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), AndroidUtilities.dp(12));
        root.addView(header);

        final BottomSheet[] sheetHolder = new BottomSheet[1];

        ActiveJob running = activeJobs.get(chat.getDialogId());
        if (running != null && !running.cancelled.get()) {
            root.addView(makeCategoryCard(activity, "STOP", 0xFFE53935,
                    "Stop Active Task (" + running.label + ")",
                    "Progress: " + running.sent + " / " + running.total + " — Tap to cancel immediately",
                    v -> {
                        running.cancel();
                        activeJobs.remove(chat.getDialogId());
                        updateFabBadge(chat);
                        toast(activity, "Task stopped");
                        if (sheetHolder[0] != null) sheetHolder[0].dismiss();
                    }));
        }

        root.addView(makeCategoryCard(activity, "SPAM", 0xFFF45255,
                "Auto Sender (Spam & Mix)",
                "Count, flood-safe delay, single & mixed texts",
                v -> {
                    if (sheetHolder[0] != null) sheetHolder[0].dismiss();
                    showAutoSenderDialog(chat);
                }));

        root.addView(makeCategoryCard(activity, "POST", 0xFF9C27B0,
                "Multi-Post & Stories",
                "Broadcast to multiple chats, schedule & multi-story unlock",
                v -> {
                    if (sheetHolder[0] != null) sheetHolder[0].dismiss();
                    showMultiPostMenu(chat);
                }));

        root.addView(makeCategoryCard(activity, "TEXT", 0xFFFF9800,
                "Text Tools & Templates",
                "SmallCaps, stylish fonts, blank msg & quick replies",
                v -> {
                    if (sheetHolder[0] != null) sheetHolder[0].dismiss();
                    showTextToolsMenu(chat);
                }));

        root.addView(makeCategoryCard(activity, "UTIL", 0xFF2196F3,
                "Chat Utilities",
                "Jump to 1st msg, purge my msgs, mention active & copy ID",
                v -> {
                    if (sheetHolder[0] != null) sheetHolder[0].dismiss();
                    showUtilitiesMenu(chat);
                }));

        root.addView(makeCategoryCard(activity, "GHOST", 0xFF009688,
                "Quick Ghost Mode",
                "Instant privacy switches without leaving this chat",
                v -> {
                    if (sheetHolder[0] != null) sheetHolder[0].dismiss();
                    showQuickGhostSheet(chat);
                }));

        BottomSheet.Builder builder = new BottomSheet.Builder(activity);
        builder.setCustomView(scroll);
        sheetHolder[0] = builder.show();
    }

    private static View makeCategoryCard(Context ctx, String badge, int color, String title, String sub, View.OnClickListener onClick) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(14));
        bg.setColor(Theme.isCurrentThemeDark() ? 0x18FFFFFF : 0x0D000000);
        card.setBackground(bg);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardLp.bottomMargin = AndroidUtilities.dp(10);
        card.setLayoutParams(cardLp);

        TextView icon = new TextView(ctx);
        icon.setText(badge);
        icon.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
        icon.setTypeface(Typeface.DEFAULT_BOLD);
        icon.setTextColor(Color.WHITE);
        icon.setGravity(Gravity.CENTER);
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setCornerRadius(AndroidUtilities.dp(10));
        badgeBg.setColor(color);
        icon.setBackground(badgeBg);
        card.addView(icon, LayoutHelper.createLinear(46, 38, Gravity.CENTER_VERTICAL, 0, 0, 12, 0));

        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);

        TextView t = new TextView(ctx);
        t.setText(title);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        col.addView(t);

        TextView s = new TextView(ctx);
        s.setText(sub);
        s.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        s.setTextColor(Theme.getColor(Theme.key_dialogTextGray3));
        col.addView(s);

        card.addView(col, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));
        card.setOnClickListener(onClick);
        return card;
    }

    // ==================== CATEGORY 1: AUTO SENDER (SPAM & MIX) ====================

    private static void showAutoSenderDialog(final ChatActivity chat) {
        Activity activity = chat.getParentActivity();
        if (activity == null) return;

        ScrollView scroll = new ScrollView(activity);
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(10), AndroidUtilities.dp(20), AndroidUtilities.dp(10));
        scroll.addView(layout);

        EditText msgInput = makeEditText(activity, "Message text (for Mixed mode: separate by comma or new line)\ne.g. Hello, Hii, Kya kar rhe", true);
        if (chat.chatActivityEnterView != null && !TextUtils.isEmpty(chat.chatActivityEnterView.getFieldText())) {
            msgInput.setText(chat.chatActivityEnterView.getFieldText().toString());
        }
        layout.addView(msgInput);

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        EditText countInput = makeEditText(activity, "Count (e.g. 10)", false);
        countInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        countInput.setText("10");
        EditText delayInput = makeEditText(activity, "Delay sec (1.5 safe)", false);
        delayInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        delayInput.setText("1.5");
        row.addView(countInput, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 0, 0, 8, 0));
        row.addView(delayInput, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 8, 0, 0, 0));
        layout.addView(row);

        CheckBox mixCheck = makeCheckBox(activity, "Mixed Text Mode (split by comma or new line)", true);
        CheckBox randomCheck = makeCheckBox(activity, "Randomize mix order (unchecked = sequential)", true);
        CheckBox silentCheck = makeCheckBox(activity, "Send silently (no notification sound)", false);
        layout.addView(mixCheck);
        layout.addView(randomCheck);
        layout.addView(silentCheck);

        TextView note = new TextView(activity);
        note.setText("Default 1.5s delay is Telegram flood-safe. Keep >= 1.0s for large counts.");
        note.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        note.setTextColor(Theme.getColor(Theme.key_dialogTextGray3));
        note.setPadding(0, AndroidUtilities.dp(6), 0, 0);
        layout.addView(note);

        new AlertDialog.Builder(activity)
                .setTitle("Auto Sender (Spam & Mix)")
                .setView(scroll)
                .setPositiveButton("Start", (d, w) -> {
                    String raw = msgInput.getText().toString().trim();
                    if (raw.isEmpty()) {
                        toast(activity, "Enter at least one message");
                        return;
                    }
                    int count = parseIntSafe(countInput.getText().toString(), 5);
                    if (count < 1) count = 1;
                    if (count > 500) count = 500;

                    double delaySec = parseDoubleSafe(delayInput.getText().toString(), 1.5);
                    if (delaySec < 0.3) delaySec = 0.3;
                    long delayMs = (long) (delaySec * 1000L);

                    List<String> pool = parseMessagePool(raw, mixCheck.isChecked());
                    if (pool.isEmpty()) {
                        toast(activity, "No valid text found");
                        return;
                    }
                    startAutoSendJob(chat, pool, count, delayMs, randomCheck.isChecked(), !silentCheck.isChecked());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static List<String> parseMessagePool(String raw, boolean mixed) {
        List<String> pool = new ArrayList<>();
        if (!mixed) {
            pool.add(raw);
            return pool;
        }
        String[] parts = raw.split("[,\\n]+");
        for (String p : parts) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                pool.add(trimmed);
            }
        }
        if (pool.isEmpty()) {
            pool.add(raw);
        }
        return pool;
    }

    private static void startAutoSendJob(ChatActivity chat, List<String> pool, int totalCount, long delayMs, boolean randomOrder, boolean notify) {
        final long dialogId = chat.getDialogId();
        final int account = chat.getCurrentAccount();

        ActiveJob old = activeJobs.remove(dialogId);
        if (old != null) old.cancel();

        final ActiveJob job = new ActiveJob();
        job.total = totalCount;
        job.label = "Auto Sender";
        activeJobs.put(dialogId, job);
        updateFabBadge(chat);

        final Random rnd = new Random();
        final Runnable[] step = new Runnable[1];
        step[0] = () -> {
            if (job.cancelled.get() || job.sent >= totalCount) {
                activeJobs.remove(dialogId);
                updateFabBadge(chat);
                if (!job.cancelled.get() && chat.getParentActivity() != null) {
                    toast(chat.getParentActivity(), "Sent " + job.sent + " messages");
                }
                return;
            }
            String text = randomOrder
                    ? pool.get(rnd.nextInt(pool.size()))
                    : pool.get(job.sent % pool.size());

            SendMessagesHelper.getInstance(account).sendMessage(
                    SendMessagesHelper.SendMessageParams.of(text, dialogId, null, null, null, false, null, null, null, notify, 0, 0, null, false)
            );
            job.sent++;
            updateFabBadge(chat);

            if (job.sent < totalCount && !job.cancelled.get()) {
                job.pendingRunnable = step[0];
                mainHandler.postDelayed(step[0], delayMs);
            } else {
                activeJobs.remove(dialogId);
                updateFabBadge(chat);
                if (chat.getParentActivity() != null) {
                    toast(chat.getParentActivity(), "Completed: " + job.sent + " sent");
                }
            }
        };
        mainHandler.post(step[0]);
    }

    // ==================== CATEGORY 2: MULTI-POST & STORIES ====================

    private static void showMultiPostMenu(final ChatActivity chat) {
        Activity activity = chat.getParentActivity();
        if (activity == null) return;

        ScrollView scroll = new ScrollView(activity);
        scroll.setBackgroundColor(Theme.getColor(Theme.key_dialogBackground));
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(16));
        scroll.addView(box);

        HeaderCell hdr = new HeaderCell(activity);
        hdr.setText("Multi-Post & Stories");
        box.addView(hdr);

        final BottomSheet[] sheetHolder = new BottomSheet[1];

        TextCheckCell storyUnlock = new TextCheckCell(activity);
        storyUnlock.setTextAndCheck("Unlimited Stories (Bypass Client Limits)", DetorConfig.isMultiStoryUnlock(), true);
        storyUnlock.setOnClickListener(v -> {
            boolean nv = !storyUnlock.isChecked();
            storyUnlock.setChecked(nv);
            DetorConfig.setMultiStoryUnlock(nv);
            toast(activity, nv ? "Multi-story client limit unlocked" : "Default story limit restored");
        });
        box.addView(storyUnlock);

        box.addView(makeActionRow(activity, "Multi-Chat Broadcaster", "Send single or mixed post to multiple chats/groups/channels", v -> {
            if (sheetHolder[0] != null) sheetHolder[0].dismiss();
            showMultiChatBroadcaster(chat);
        }));

        box.addView(makeActionRow(activity, "Schedule Multi-Post (Current Chat)", "Queue multiple scheduled posts at fixed intervals", v -> {
            if (sheetHolder[0] != null) sheetHolder[0].dismiss();
            showScheduledMultiPostDialog(chat);
        }));

        BottomSheet.Builder b = new BottomSheet.Builder(activity);
        b.setCustomView(scroll);
        sheetHolder[0] = b.show();
    }

    private static void showMultiChatBroadcaster(final ChatActivity chat) {
        Activity activity = chat.getParentActivity();
        if (activity == null) return;

        final int account = chat.getCurrentAccount();
        MessagesController mc = MessagesController.getInstance(account);
        ArrayList<TLRPC.Dialog> allDialogs = new ArrayList<>(mc.getAllDialogs());

        final ArrayList<Long> dialogIds = new ArrayList<>();
        final ArrayList<String> dialogNames = new ArrayList<>();
        for (int i = 0; i < allDialogs.size() && dialogIds.size() < 60; i++) {
            TLRPC.Dialog d = allDialogs.get(i);
            if (d == null || d.id == 0) continue;
            String name = null;
            if (DialogObject.isUserDialog(d.id)) {
                TLRPC.User u = mc.getUser(d.id);
                if (u != null && !u.deleted) name = "[User] " + UserObject.getUserName(u);
            } else if (DialogObject.isChatDialog(d.id)) {
                TLRPC.Chat c = mc.getChat(-d.id);
                if (c != null && !c.left && !c.deactivated) {
                    name = (ChatObject.isChannel(c) && !c.megagroup ? "[Ch] " : "[Grp] ") + c.title;
                }
            }
            if (!TextUtils.isEmpty(name)) {
                dialogIds.add(d.id);
                dialogNames.add(name);
            }
        }

        if (dialogIds.isEmpty()) {
            toast(activity, "No chats loaded yet");
            return;
        }

        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(8), AndroidUtilities.dp(18), AndroidUtilities.dp(10));
        scroll.addView(root);

        EditText postInput = makeEditText(activity, "Post text (separate by --- for rotating mixed posts)", true);
        if (chat.chatActivityEnterView != null && !TextUtils.isEmpty(chat.chatActivityEnterView.getFieldText())) {
            postInput.setText(chat.chatActivityEnterView.getFieldText().toString());
        }
        root.addView(postInput);

        EditText delayInput = makeEditText(activity, "Delay between chats in sec (2.0 recommended)", false);
        delayInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        delayInput.setText("2.0");
        root.addView(delayInput);

        TextView pickLabel = new TextView(activity);
        pickLabel.setText("Select target chats (" + dialogIds.size() + " recent):");
        pickLabel.setTypeface(Typeface.DEFAULT_BOLD);
        pickLabel.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        pickLabel.setPadding(0, AndroidUtilities.dp(10), 0, AndroidUtilities.dp(6));
        root.addView(pickLabel);

        final ArrayList<CheckBox> checks = new ArrayList<>();
        for (int i = 0; i < dialogIds.size(); i++) {
            CheckBox cb = makeCheckBox(activity, dialogNames.get(i), dialogIds.get(i) == chat.getDialogId());
            checks.add(cb);
            root.addView(cb);
        }

        new AlertDialog.Builder(activity)
                .setTitle("Multi-Chat Broadcaster")
                .setView(scroll)
                .setPositiveButton("Broadcast", (d, w) -> {
                    String raw = postInput.getText().toString().trim();
                    if (raw.isEmpty()) {
                        toast(activity, "Enter post text first");
                        return;
                    }
                    ArrayList<Long> targets = new ArrayList<>();
                    for (int i = 0; i < checks.size(); i++) {
                        if (checks.get(i).isChecked()) {
                            targets.add(dialogIds.get(i));
                        }
                    }
                    if (targets.isEmpty()) {
                        toast(activity, "Select at least 1 chat");
                        return;
                    }
                    List<String> variants = new ArrayList<>();
                    for (String part : raw.split("---")) {
                        if (!part.trim().isEmpty()) variants.add(part.trim());
                    }
                    if (variants.isEmpty()) variants.add(raw);

                    long delayMs = (long) (Math.max(1.0, parseDoubleSafe(delayInput.getText().toString(), 2.0)) * 1000L);
                    startBroadcastJob(chat, targets, variants, delayMs);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void startBroadcastJob(ChatActivity chat, ArrayList<Long> targets, List<String> variants, long delayMs) {
        final long hostDialogId = chat.getDialogId();
        final int account = chat.getCurrentAccount();

        ActiveJob old = activeJobs.remove(hostDialogId);
        if (old != null) old.cancel();

        final ActiveJob job = new ActiveJob();
        job.total = targets.size();
        job.label = "Multi-Post";
        activeJobs.put(hostDialogId, job);
        updateFabBadge(chat);

        final Runnable[] step = new Runnable[1];
        step[0] = () -> {
            if (job.cancelled.get() || job.sent >= targets.size()) {
                activeJobs.remove(hostDialogId);
                updateFabBadge(chat);
                if (!job.cancelled.get() && chat.getParentActivity() != null) {
                    toast(chat.getParentActivity(), "Broadcast done: " + job.sent + " chats");
                }
                return;
            }
            long targetDid = targets.get(job.sent);
            String text = variants.get(job.sent % variants.size());
            SendMessagesHelper.getInstance(account).sendMessage(
                    SendMessagesHelper.SendMessageParams.of(text, targetDid, null, null, null, false, null, null, null, true, 0, 0, null, false)
            );
            job.sent++;
            updateFabBadge(chat);

            if (job.sent < targets.size() && !job.cancelled.get()) {
                job.pendingRunnable = step[0];
                mainHandler.postDelayed(step[0], delayMs);
            } else {
                activeJobs.remove(hostDialogId);
                updateFabBadge(chat);
                if (chat.getParentActivity() != null) {
                    toast(chat.getParentActivity(), "Broadcast done: " + job.sent + " chats");
                }
            }
        };
        mainHandler.post(step[0]);
    }

    private static void showScheduledMultiPostDialog(final ChatActivity chat) {
        Activity activity = chat.getParentActivity();
        if (activity == null) return;

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(10), AndroidUtilities.dp(20), AndroidUtilities.dp(10));

        EditText postsInput = makeEditText(activity, "Enter multiple posts (1 per line)", true);
        EditText gapMinInput = makeEditText(activity, "Gap between posts in minutes (e.g. 15)", false);
        gapMinInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        gapMinInput.setText("15");
        layout.addView(postsInput);
        layout.addView(gapMinInput);

        new AlertDialog.Builder(activity)
                .setTitle("Schedule Multi-Post")
                .setView(layout)
                .setPositiveButton("Schedule All", (d, w) -> {
                    String[] lines = postsInput.getText().toString().split("\\n");
                    List<String> valid = new ArrayList<>();
                    for (String s : lines) {
                        if (!s.trim().isEmpty()) valid.add(s.trim());
                    }
                    if (valid.isEmpty()) {
                        toast(activity, "Enter at least 1 post");
                        return;
                    }
                    int gapMin = Math.max(1, parseIntSafe(gapMinInput.getText().toString(), 15));
                    int account = chat.getCurrentAccount();
                    long dialogId = chat.getDialogId();
                    int baseTime = ConnectionsManager.getInstance(account).getCurrentTime() + 60;
                    for (int i = 0; i < valid.size(); i++) {
                        int sched = baseTime + (i * gapMin * 60);
                        SendMessagesHelper.getInstance(account).sendMessage(
                                SendMessagesHelper.SendMessageParams.of(valid.get(i), dialogId, null, null, null, false, null, null, null, true, sched, 0, null, false)
                        );
                    }
                    toast(activity, "Scheduled " + valid.size() + " posts!");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ==================== CATEGORY 3: TEXT TOOLS & TEMPLATES ====================

    private static void showTextToolsMenu(final ChatActivity chat) {
        Activity activity = chat.getParentActivity();
        if (activity == null) return;

        ScrollView scroll = new ScrollView(activity);
        scroll.setBackgroundColor(Theme.getColor(Theme.key_dialogBackground));
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(16));
        scroll.addView(box);

        HeaderCell hdr = new HeaderCell(activity);
        hdr.setText("Text Tools & Templates");
        box.addView(hdr);

        final BottomSheet[] sheetHolder = new BottomSheet[1];

        box.addView(makeActionRow(activity, "Convert Input -> ꜱᴍᴀʟʟᴄᴀᴘꜱ", "Transforms text typed in input box", v -> {
            transformInputText(chat, DetorChatHub::toSmallCaps);
            if (sheetHolder[0] != null) sheetHolder[0].dismiss();
        }));
        box.addView(makeActionRow(activity, "Convert Input -> 𝐁𝐨𝐥𝐝 𝐒𝐭𝐲𝐥𝐢𝐬𝐡", "Unicode bold mathematical font", v -> {
            transformInputText(chat, DetorChatHub::toBoldFont);
            if (sheetHolder[0] != null) sheetHolder[0].dismiss();
        }));
        box.addView(makeActionRow(activity, "Convert Input -> 𝙼𝚘𝚗𝚘𝚜𝚙𝚊𝚌𝚎", "Unicode monospace font", v -> {
            transformInputText(chat, DetorChatHub::toMonoFont);
            if (sheetHolder[0] != null) sheetHolder[0].dismiss();
        }));
        box.addView(makeActionRow(activity, "Reverse Input Text", "Flips text backwards", v -> {
            transformInputText(chat, s -> new StringBuilder(s).reverse().toString());
            if (sheetHolder[0] != null) sheetHolder[0].dismiss();
        }));
        box.addView(makeActionRow(activity, "Send Invisible Blank Message", "Sends an empty-looking message", v -> {
            SendMessagesHelper.getInstance(chat.getCurrentAccount()).sendMessage(
                    SendMessagesHelper.SendMessageParams.of("\u2800", chat.getDialogId(), null, null, null, false, null, null, null, true, 0, 0, null, false)
            );
            if (sheetHolder[0] != null) sheetHolder[0].dismiss();
        }));
        box.addView(makeActionRow(activity, "Quick Saved Templates", "Save & 1-tap send frequent replies", v -> {
            if (sheetHolder[0] != null) sheetHolder[0].dismiss();
            showTemplatesDialog(chat);
        }));

        BottomSheet.Builder b = new BottomSheet.Builder(activity);
        b.setCustomView(scroll);
        sheetHolder[0] = b.show();
    }

    private interface TextTransform {
        String apply(String in);
    }

    private static void transformInputText(ChatActivity chat, TextTransform fn) {
        Activity activity = chat.getParentActivity();
        if (chat.chatActivityEnterView == null) return;
        CharSequence cur = chat.chatActivityEnterView.getFieldText();
        if (TextUtils.isEmpty(cur)) {
            toast(activity, "Type some text in the chat box first");
            return;
        }
        chat.chatActivityEnterView.setFieldText(fn.apply(cur.toString()));
    }

    private static void showTemplatesDialog(final ChatActivity chat) {
        Activity activity = chat.getParentActivity();
        if (activity == null) return;

        List<String> templates = DetorConfig.getTemplates();
        String[] items = new String[templates.size() + 1];
        items[0] = "+ Add New Template";
        for (int i = 0; i < templates.size(); i++) {
            items[i + 1] = templates.get(i);
        }

        new AlertDialog.Builder(activity)
                .setTitle("Quick Templates (Tap to Insert)")
                .setItems(items, (d, which) -> {
                    if (which == 0) {
                        EditText input = makeEditText(activity, "Enter template text", true);
                        LinearLayout pad = new LinearLayout(activity);
                        pad.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(10), AndroidUtilities.dp(20), 0);
                        pad.addView(input, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
                        new AlertDialog.Builder(activity)
                                .setTitle("New Quick Template")
                                .setView(pad)
                                .setPositiveButton("Save", (d2, w2) -> {
                                    String t = input.getText().toString().trim();
                                    if (!t.isEmpty()) {
                                        DetorConfig.addTemplate(t);
                                        toast(activity, "Template saved");
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    } else {
                        final String selected = templates.get(which - 1);
                        new AlertDialog.Builder(activity)
                                .setTitle("Template Action")
                                .setMessage(selected)
                                .setPositiveButton("Send Now", (d2, w2) -> SendMessagesHelper.getInstance(chat.getCurrentAccount()).sendMessage(
                                        SendMessagesHelper.SendMessageParams.of(selected, chat.getDialogId(), null, null, null, false, null, null, null, true, 0, 0, null, false)
                                ))
                                .setNeutralButton("Insert in Box", (d2, w2) -> {
                                    if (chat.chatActivityEnterView != null) {
                                        chat.chatActivityEnterView.setFieldText(selected);
                                    }
                                })
                                .setNegativeButton("Delete", (d2, w2) -> {
                                    DetorConfig.removeTemplate(selected);
                                    toast(activity, "Deleted");
                                })
                                .show();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    // ==================== CATEGORY 4: CHAT UTILITIES ====================

    private static void showUtilitiesMenu(final ChatActivity chat) {
        Activity activity = chat.getParentActivity();
        if (activity == null) return;

        ScrollView scroll = new ScrollView(activity);
        scroll.setBackgroundColor(Theme.getColor(Theme.key_dialogBackground));
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(16));
        scroll.addView(box);

        HeaderCell hdr = new HeaderCell(activity);
        hdr.setText("Chat Utilities");
        box.addView(hdr);

        final BottomSheet[] sheetHolder = new BottomSheet[1];

        box.addView(makeActionRow(activity, "Jump to First Message", "Scroll directly to message #1", v -> {
            if (sheetHolder[0] != null) sheetHolder[0].dismiss();
            chat.scrollToMessageId(1, 0, true, 0, true, 0);
        }));

        box.addView(makeActionRow(activity, "Purge My Recent Messages", "Delete your last N messages in this chat", v -> {
            if (sheetHolder[0] != null) sheetHolder[0].dismiss();
            showPurgeDialog(chat);
        }));

        box.addView(makeActionRow(activity, "Mention Active Members", "Insert @usernames of recent chatters into box", v -> {
            if (sheetHolder[0] != null) sheetHolder[0].dismiss();
            mentionRecentMembers(chat);
        }));

        box.addView(makeActionRow(activity, "Copy Chat / User ID", "Current ID: " + chat.getDialogId(), v -> {
            AndroidUtilities.addToClipboard(String.valueOf(chat.getDialogId()));
            toast(activity, "Copied ID: " + chat.getDialogId());
            if (sheetHolder[0] != null) sheetHolder[0].dismiss();
        }));

        BottomSheet.Builder b = new BottomSheet.Builder(activity);
        b.setCustomView(scroll);
        sheetHolder[0] = b.show();
    }

    private static void showPurgeDialog(final ChatActivity chat) {
        Activity activity = chat.getParentActivity();
        if (activity == null) return;

        final String[] labels = {"Last 5 of my messages", "Last 15 of my messages", "Last 30 of my messages", "Last 50 of my messages"};
        final int[] limits = {5, 15, 30, 50};

        new AlertDialog.Builder(activity)
                .setTitle("Purge My Messages")
                .setItems(labels, (d, which) -> {
                    int limit = limits[which];
                    ArrayList<Integer> ids = new ArrayList<>();
                    if (chat.messages != null) {
                        for (int i = 0; i < chat.messages.size() && ids.size() < limit; i++) {
                            MessageObject m = chat.messages.get(i);
                            if (m != null && m.isOutOwner() && m.getId() > 0) {
                                ids.add(m.getId());
                            }
                        }
                    }
                    if (ids.isEmpty()) {
                        toast(activity, "No loaded outgoing messages found");
                        return;
                    }
                    long dialogId = chat.getDialogId();
                    int account = chat.getCurrentAccount();
                    if (dialogId < 0 && ChatObject.isChannel(MessagesController.getInstance(account).getChat(-dialogId))) {
                        TLRPC.TL_channels_deleteMessages req = new TLRPC.TL_channels_deleteMessages();
                        req.channel = MessagesController.getInstance(account).getInputChannel(-dialogId);
                        req.id.addAll(ids);
                        ConnectionsManager.getInstance(account).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() ->
                                toast(activity, err == null ? "Purged " + ids.size() + " messages" : "Error: " + err.text)
                        ));
                    } else {
                        TLRPC.TL_messages_deleteMessages req = new TLRPC.TL_messages_deleteMessages();
                        req.revoke = true;
                        req.id.addAll(ids);
                        ConnectionsManager.getInstance(account).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() ->
                                toast(activity, err == null ? "Purged " + ids.size() + " messages" : "Error: " + err.text)
                        ));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void mentionRecentMembers(final ChatActivity chat) {
        Activity activity = chat.getParentActivity();
        if (activity == null || chat.chatActivityEnterView == null || chat.messages == null) return;

        MessagesController mc = MessagesController.getInstance(chat.getCurrentAccount());
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        for (int i = 0; i < chat.messages.size() && tags.size() < 20; i++) {
            MessageObject m = chat.messages.get(i);
            if (m == null || m.isOutOwner()) continue;
            long fromId = m.getFromChatId();
            if (fromId > 0) {
                TLRPC.User u = mc.getUser(fromId);
                if (u != null && !u.bot && !u.deleted) {
                    String uname = UserObject.getPublicUsername(u);
                    if (!TextUtils.isEmpty(uname)) {
                        tags.add("@" + uname);
                    }
                }
            }
        }
        if (tags.isEmpty()) {
            toast(activity, "No recent @usernames found in loaded chat");
            return;
        }
        String joined = TextUtils.join(" ", tags);
        CharSequence existing = chat.chatActivityEnterView.getFieldText();
        String finalTxt = TextUtils.isEmpty(existing) ? joined : existing + " " + joined;
        chat.chatActivityEnterView.setFieldText(finalTxt);
        toast(activity, "Inserted " + tags.size() + " mentions");
    }

    // ==================== CATEGORY 5: QUICK GHOST ====================

    private static void showQuickGhostSheet(final ChatActivity chat) {
        Activity activity = chat.getParentActivity();
        if (activity == null) return;

        ScrollView scroll = new ScrollView(activity);
        scroll.setBackgroundColor(Theme.getColor(Theme.key_dialogBackground));
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(16));
        scroll.addView(box);

        HeaderCell hdr = new HeaderCell(activity);
        hdr.setText("Quick Ghost Controls");
        box.addView(hdr);

        addQuickToggle(activity, box, "Hide Seen (Ghost Read)", DetorConfig.isHideSeen(), DetorConfig::setHideSeen);
        addQuickToggle(activity, box, "Hide Typing / Recording", DetorConfig.isHideTyping(), DetorConfig::setHideTyping);
        addQuickToggle(activity, box, "Always Offline", DetorConfig.isAlwaysOffline(), DetorConfig::setAlwaysOffline);
        addQuickToggle(activity, box, "Anonymous Story View", DetorConfig.isAnonStoryView(), DetorConfig::setAnonStoryView);
        addQuickToggle(activity, box, "Anti-Delete + Deleted Tag", DetorConfig.isAntiDelete(), DetorConfig::setAntiDelete);

        final BottomSheet[] sheetHolder = new BottomSheet[1];
        box.addView(makeActionRow(activity, "Open Full DetorGram Settings", "All 14 features & toggles", v -> {
            if (sheetHolder[0] != null) sheetHolder[0].dismiss();
            DetorSettingsSheet.show(activity, () -> refreshVisibility(chat));
        }));

        BottomSheet.Builder b = new BottomSheet.Builder(activity);
        b.setCustomView(scroll);
        sheetHolder[0] = b.show();
    }

    private static void addQuickToggle(Context ctx, LinearLayout parent, String label, boolean init, DetorSettingsSheet.BoolSetter setter) {
        TextCheckCell cell = new TextCheckCell(ctx);
        cell.setTextAndCheck(label, init, true);
        cell.setOnClickListener(v -> {
            boolean nv = !cell.isChecked();
            cell.setChecked(nv);
            setter.set(nv);
        });
        parent.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    // ==================== HELPERS & FONT CONVERTERS ====================

    private static View makeActionRow(Context ctx, String title, String sub, View.OnClickListener click) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(12), AndroidUtilities.dp(20), AndroidUtilities.dp(12));
        row.setBackground(Theme.getSelectorDrawable(false));

        TextView t = new TextView(ctx);
        t.setText(title);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        row.addView(t);

        if (!TextUtils.isEmpty(sub)) {
            TextView s = new TextView(ctx);
            s.setText(sub);
            s.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            s.setTextColor(Theme.getColor(Theme.key_dialogTextGray3));
            row.addView(s);
        }
        row.setOnClickListener(click);
        return row;
    }

    private static EditText makeEditText(Context ctx, String hint, boolean multiline) {
        EditText et = new EditText(ctx);
        et.setHint(hint);
        et.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        et.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        et.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        et.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        if (multiline) {
            et.setMinLines(3);
            et.setMaxLines(6);
            et.setGravity(Gravity.TOP | Gravity.LEFT);
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        }
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(10));
        bg.setStroke(AndroidUtilities.dp(1), 0x44888888);
        et.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = AndroidUtilities.dp(10);
        et.setLayoutParams(lp);
        return et;
    }

    private static CheckBox makeCheckBox(Context ctx, String text, boolean checked) {
        CheckBox cb = new CheckBox(ctx);
        cb.setText(text);
        cb.setChecked(checked);
        cb.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        cb.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        return cb;
    }

    private static int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private static double parseDoubleSafe(String s, double def) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return def; }
    }

    private static void toast(Context ctx, String msg) {
        if (ctx != null) {
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
        }
    }

    private static String toSmallCaps(String input) {
        String normal = "abcdefghijklmnopqrstuvwxyz";
        String small = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            int idx = normal.indexOf(Character.toLowerCase(c));
            if (idx >= 0) {
                sb.append(small.charAt(idx));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String toBoldFont(String input) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                sb.append(Character.toChars(0x1D400 + (c - 'A')));
            } else if (c >= 'a' && c <= 'z') {
                sb.append(Character.toChars(0x1D41A + (c - 'a')));
            } else if (c >= '0' && c <= '9') {
                sb.append(Character.toChars(0x1D7CE + (c - '0')));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String toMonoFont(String input) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                sb.append(Character.toChars(0x1D670 + (c - 'A')));
            } else if (c >= 'a' && c <= 'z') {
                sb.append(Character.toChars(0x1D68A + (c - 'a')));
            } else if (c >= '0' && c <= '9') {
                sb.append(Character.toChars(0x1D7F6 + (c - '0')));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
