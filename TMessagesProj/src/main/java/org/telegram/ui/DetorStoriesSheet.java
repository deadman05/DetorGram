package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Stories.StoriesController;
import org.telegram.ui.Stories.recorder.StoryRecorder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class DetorStoriesSheet {

    public static void show(final DialogsActivity dialogsActivity) {
        if (dialogsActivity == null) return;
        Activity activity = dialogsActivity.getParentActivity();
        if (activity == null) return;

        final int account = dialogsActivity.getCurrentAccount();
        final MessagesController mc = MessagesController.getInstance(account);
        final StoriesController sc = mc.getStoriesController();

        ScrollView scroll = new ScrollView(activity);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setBackgroundColor(Theme.getColor(Theme.key_dialogBackground));

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(22));
        scroll.addView(root, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        List<Object> peerStoriesList = fetchPeerStories(sc);

        TextView header = new TextView(activity);
        header.setText("Stories (" + peerStoriesList.size() + ")");
        header.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        header.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), AndroidUtilities.dp(12));
        root.addView(header);

        final BottomSheet[] holder = new BottomSheet[1];

        // Row 1: My Story (+)
        TLRPC.User selfUser = UserConfig.getInstance(account).getCurrentUser();
        root.addView(makeStoryRow(activity, account, selfUser, null, "My Story (+)", "Tap to post a new photo or video story", true, v -> {
            if (holder[0] != null) holder[0].dismiss();
            openStoryRecorder(dialogsActivity, activity, account);
        }));

        // Divider label
        TextView label = new TextView(activity);
        label.setText(peerStoriesList.isEmpty() ? "No active stories right now" : "Recent Stories");
        label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        label.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(8), AndroidUtilities.dp(6), AndroidUtilities.dp(8));
        root.addView(label);

        // Line-by-line Users' Stories
        long myId = UserConfig.getInstance(account).getClientUserId();
        for (int i = 0; i < peerStoriesList.size(); i++) {
            final Object ps = peerStoriesList.get(i);
            final long did = extractDialogId(ps);
            if (did == 0) continue;

            int count = extractStoryCount(ps);
            TLRPC.User u = did > 0 ? mc.getUser(did) : null;
            TLRPC.Chat c = did < 0 ? mc.getChat(-did) : null;

            String title;
            if (did == myId) {
                title = "Your Active Story";
            } else if (u != null) {
                title = UserObject.getUserName(u);
            } else if (c != null) {
                title = c.title;
            } else {
                title = "User " + did;
            }
            String sub = (count > 0 ? count + (count == 1 ? " story" : " stories") : "Active story") + " • Tap to view";

            root.addView(makeStoryRow(activity, account, u, c, title, sub, false, v -> {
                if (holder[0] != null) holder[0].dismiss();
                openPeerStory(dialogsActivity, activity, did, ps);
            }));
        }

        BottomSheet.Builder builder = new BottomSheet.Builder(activity);
        builder.setCustomView(scroll);
        holder[0] = builder.show();
    }

    private static View makeStoryRow(Context ctx, int account, TLRPC.User user, TLRPC.Chat chat,
                                     String title, String sub, boolean isAddSelf, View.OnClickListener onClick) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(14));
        bg.setColor(Theme.isCurrentThemeDark() ? 0x18FFFFFF : 0x0B000000);
        card.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.bottomMargin = AndroidUtilities.dp(8);
        card.setLayoutParams(lp);

        // Ring container
        FrameLayout ring = new FrameLayout(ctx);
        GradientDrawable ringBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                isAddSelf ? new int[]{0xFF2AABEE, 0xFF7C4DFF} : new int[]{0xFF00E676, 0xFF29B6F6}
        );
        ringBg.setShape(GradientDrawable.OVAL);
        ring.setBackground(ringBg);
        ring.setPadding(AndroidUtilities.dp(2.5f), AndroidUtilities.dp(2.5f), AndroidUtilities.dp(2.5f), AndroidUtilities.dp(2.5f));

        BackupImageView avatar = new BackupImageView(ctx);
        avatar.setRoundRadius(AndroidUtilities.dp(22));
        AvatarDrawable ad = new AvatarDrawable();
        if (user != null) {
            ad.setInfo(account, user);
            avatar.setForUserOrChat(user, ad);
        } else if (chat != null) {
            ad.setInfo(account, chat);
            avatar.setForUserOrChat(chat, ad);
        }
        ring.addView(avatar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        if (isAddSelf) {
            TextView plus = new TextView(ctx);
            plus.setText("+");
            plus.setTextColor(Color.WHITE);
            plus.setTypeface(Typeface.DEFAULT_BOLD);
            plus.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            plus.setGravity(Gravity.CENTER);
            GradientDrawable pbg = new GradientDrawable();
            pbg.setShape(GradientDrawable.OVAL);
            pbg.setColor(0xFF2AABEE);
            pbg.setStroke(AndroidUtilities.dp(1.5f), Theme.getColor(Theme.key_dialogBackground));
            plus.setBackground(pbg);
            ring.addView(plus, LayoutHelper.createFrame(18, 18, Gravity.RIGHT | Gravity.BOTTOM));
        }

        card.addView(ring, LayoutHelper.createLinear(48, 48, Gravity.CENTER_VERTICAL, 0, 0, 12, 0));

        LinearLayout textCol = new LinearLayout(ctx);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView titleView = new TextView(ctx);
        titleView.setText(TextUtils.isEmpty(title) ? "Story" : title);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setSingleLine(true);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        textCol.addView(titleView);

        TextView subView = new TextView(ctx);
        subView.setText(sub);
        subView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        subView.setTextColor(Theme.getColor(Theme.key_dialogTextGray3));
        textCol.addView(subView);

        card.addView(textCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

        TextView arrow = new TextView(ctx);
        arrow.setText(isAddSelf ? "ADD" : "VIEW");
        arrow.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        arrow.setTypeface(Typeface.DEFAULT_BOLD);
        arrow.setTextColor(Color.WHITE);
        arrow.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(5), AndroidUtilities.dp(10), AndroidUtilities.dp(5));
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setCornerRadius(AndroidUtilities.dp(10));
        btnBg.setColor(isAddSelf ? 0xFF7C4DFF : 0xFF2AABEE);
        arrow.setBackground(btnBg);
        card.addView(arrow, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));

        card.setOnClickListener(onClick);
        return card;
    }

    private static List<Object> fetchPeerStories(StoriesController sc) {
        List<Object> result = new ArrayList<>();
        if (sc == null) return result;
        try {
            Method m = sc.getClass().getMethod("getDialogs");
            Object obj = m.invoke(sc);
            if (obj instanceof List) {
                result.addAll((List<?>) obj);
            }
        } catch (Throwable ignored) {
        }
        try {
            Method m2 = sc.getClass().getMethod("getHiddenList");
            Object obj2 = m2.invoke(sc);
            if (obj2 instanceof List) {
                for (Object item : (List<?>) obj2) {
                    if (!result.contains(item)) result.add(item);
                }
            }
        } catch (Throwable ignored) {
        }
        return result;
    }

    private static long extractDialogId(Object peerStories) {
        try {
            Field f = peerStories.getClass().getField("peer");
            Object peer = f.get(peerStories);
            if (peer instanceof TLRPC.Peer) {
                return DialogObject.getPeerDialogId((TLRPC.Peer) peer);
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static int extractStoryCount(Object peerStories) {
        try {
            Field f = peerStories.getClass().getField("stories");
            Object stories = f.get(peerStories);
            if (stories instanceof List) {
                return ((List<?>) stories).size();
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static void openStoryRecorder(DialogsActivity dialogsActivity, Activity activity, int account) {
        try {
            View fab = dialogsActivity.getDetorStoriesFab();
            if (fab != null && fab.performClick()) {
                return;
            }
        } catch (Throwable ignored) {
        }
        try {
            Object recorder = StoryRecorder.getInstance(activity, account);
            for (Method m : recorder.getClass().getMethods()) {
                if (m.getName().equals("open") && m.getParameterTypes().length == 1) {
                    m.invoke(recorder, new Object[]{null});
                    return;
                }
            }
        } catch (Throwable e) {
            Toast.makeText(activity, "Use camera button to add story", Toast.LENGTH_SHORT).show();
        }
    }

    private static void openPeerStory(DialogsActivity dialogsActivity, Activity activity, long dialogId, Object peerStories) {
        try {
            Object viewer = dialogsActivity.getOrCreateStoryViewer();
            if (viewer == null) return;
            for (Method m : viewer.getClass().getMethods()) {
                if (!m.getName().equals("open")) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 3 && p[0] == Context.class && p[1] == long.class) {
                    m.invoke(viewer, activity, dialogId, null);
                    return;
                }
            }
            for (Method m : viewer.getClass().getMethods()) {
                if (!m.getName().equals("open")) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 3 && p[0] == Context.class && peerStories != null && p[1].isAssignableFrom(peerStories.getClass())) {
                    m.invoke(viewer, activity, peerStories, null);
                    return;
                }
            }
        } catch (Throwable e) {
            Toast.makeText(activity, "Unable to open story", Toast.LENGTH_SHORT).show();
        }
    }
}
