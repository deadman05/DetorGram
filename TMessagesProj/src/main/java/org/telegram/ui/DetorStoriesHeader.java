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
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
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
import java.util.WeakHashMap;

public class DetorStoriesHeader {

    private static final WeakHashMap<DialogsActivity, Controller> controllers = new WeakHashMap<>();

    public static void attach(DialogsActivity fragment, ViewGroup contentView) {
        if (fragment == null || contentView == null) return;
        ActionBar actionBar = fragment.getActionBar();
        Context ctx = fragment.getParentActivity();
        if (actionBar == null || ctx == null) return;

        Controller existing = controllers.get(fragment);
        if (existing != null) {
            existing.refresh();
            return;
        }
        Controller c = new Controller(fragment, actionBar, contentView, ctx);
        controllers.put(fragment, c);
    }

    public static boolean collapseIfExpanded(DialogsActivity fragment) {
        Controller c = controllers.get(fragment);
        if (c != null && c.expanded) {
            c.setExpanded(false);
            return true;
        }
        return false;
    }

    public static void refresh(DialogsActivity fragment) {
        Controller c = controllers.get(fragment);
        if (c != null) {
            c.refresh();
        }
    }

    private static class Controller implements NotificationCenter.NotificationCenterDelegate {
        final DialogsActivity fragment;
        final ActionBar actionBar;
        final ViewGroup contentView;
        final Context ctx;
        final int account;

        final FrameLayout circlesContainer;
        final FrameLayout stripOverlay;
        final LinearLayout stripRow;

        boolean expanded = false;
        int currentOffsetDp = 42;
        int storiesUpdatedEventId = -1;

        Controller(DialogsActivity fragment, ActionBar actionBar, ViewGroup contentView, Context ctx) {
            this.fragment = fragment;
            this.actionBar = actionBar;
            this.contentView = contentView;
            this.ctx = ctx;
            this.account = fragment.getCurrentAccount();

            // 1. Overlapping ○))) circles inside ActionBar (before "DetorGram")
            circlesContainer = new FrameLayout(ctx);
            circlesContainer.setOnClickListener(v -> setExpanded(!expanded));
            actionBar.addView(circlesContainer, LayoutHelper.createFrame(
                    LayoutHelper.WRAP_CONTENT, 32, Gravity.BOTTOM | Gravity.LEFT, 14, 0, 0, 11
            ));

            // 2. Expandable horizontal side-by-side Stories strip below ActionBar
            stripOverlay = new FrameLayout(ctx);
            stripOverlay.setVisibility(View.GONE);
            stripOverlay.setElevation(AndroidUtilities.dp(8));
            stripOverlay.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

            LinearLayout wrapper = new LinearLayout(ctx);
            wrapper.setOrientation(LinearLayout.VERTICAL);

            HorizontalScrollView hScroll = new HorizontalScrollView(ctx);
            hScroll.setHorizontalScrollBarEnabled(false);
            hScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

            stripRow = new LinearLayout(ctx);
            stripRow.setOrientation(LinearLayout.HORIZONTAL);
            stripRow.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
            hScroll.addView(stripRow, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            wrapper.addView(hScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            View divider = new View(ctx);
            divider.setBackgroundColor(0x26888888);
            wrapper.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1));

            stripOverlay.addView(wrapper, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            contentView.addView(stripOverlay, LayoutHelper.createFrame(
                    LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT
            ));

            // Sync Y position & hide circles cleanly whenever Search field opens
            actionBar.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
                if (stripOverlay.getVisibility() == View.VISIBLE) {
                    stripOverlay.setTranslationY(actionBar.getBottom());
                    stripOverlay.bringToFront();
                }
                syncHeaderLayout();
            });

            // Listen for storiesUpdated safely via reflection
            try {
                Field f = NotificationCenter.class.getField("storiesUpdated");
                storiesUpdatedEventId = f.getInt(null);
                NotificationCenter.getInstance(account).addObserver(this, storiesUpdatedEventId);
            } catch (Throwable ignored) {
            }

            circlesContainer.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    if (storiesUpdatedEventId != -1) {
                        NotificationCenter.getInstance(account).removeObserver(Controller.this, storiesUpdatedEventId);
                    }
                    controllers.remove(fragment);
                }
            });

            refresh();
            AndroidUtilities.runOnUIThread(this::refresh, 1500);
            AndroidUtilities.runOnUIThread(this::refresh, 4000);
        }

        @Override
        public void didReceivedNotification(int id, int account, Object... args) {
            if (id == storiesUpdatedEventId) {
                refresh();
            }
        }

        void setExpanded(boolean expand) {
            expanded = expand;
            if (expanded) {
                rebuildExpandedStrip();
                stripOverlay.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                stripOverlay.setTranslationY(actionBar.getBottom());
                stripOverlay.setVisibility(View.VISIBLE);
                stripOverlay.setAlpha(0f);
                stripOverlay.animate().alpha(1f).setDuration(180).start();
                stripOverlay.bringToFront();
            } else {
                stripOverlay.setVisibility(View.GONE);
            }
            syncHeaderLayout();
        }

        void syncHeaderLayout() {
            boolean searchOpen = actionBar.isSearchFieldVisible();
            if (searchOpen && expanded) {
                expanded = false;
                stripOverlay.setVisibility(View.GONE);
            }
            if (expanded || searchOpen) {
                circlesContainer.setVisibility(View.GONE);
                actionBar.setAdditionalTextLeft(0);
            } else {
                circlesContainer.setVisibility(View.VISIBLE);
                actionBar.setAdditionalTextLeft(AndroidUtilities.dp(currentOffsetDp));
            }
        }

        void refresh() {
            rebuildCollapsedCircles();
            if (expanded) {
                rebuildExpandedStrip();
            }
            syncHeaderLayout();
        }

        private void rebuildCollapsedCircles() {
            circlesContainer.removeAllViews();
            MessagesController mc = MessagesController.getInstance(account);
            StoriesController sc = mc.getStoriesController();
            List<Object> peers = fetchPeerStories(sc);

            List<Long> topIds = new ArrayList<>();
            for (int i = 0; i < peers.size() && topIds.size() < 3; i++) {
                long did = extractDialogId(peers.get(i));
                if (did != 0 && !topIds.contains(did)) {
                    topIds.add(did);
                }
            }

            int borderBgColor = Theme.getColor(Theme.key_actionBarDefault);

            if (topIds.isEmpty()) {
                // Show self circle with small + badge when no other stories exist yet
                TLRPC.User self = UserConfig.getInstance(account).getCurrentUser();
                FrameLayout item = buildMiniCircle(self, null, borderBgColor, true);
                circlesContainer.addView(item, LayoutHelper.createFrame(30, 30, Gravity.CENTER_VERTICAL | Gravity.LEFT, 0, 0, 0, 0));
                currentOffsetDp = 40;
                return;
            }

            // Draw overlapping ○))) circles (right-to-left z-order so leftmost sits cleanly on top)
            int count = topIds.size();
            for (int i = count - 1; i >= 0; i--) {
                long did = topIds.get(i);
                TLRPC.User u = did > 0 ? mc.getUser(did) : null;
                TLRPC.Chat c = did < 0 ? mc.getChat(-did) : null;
                FrameLayout item = buildMiniCircle(u, c, borderBgColor, false);
                circlesContainer.addView(item, LayoutHelper.createFrame(
                        28, 28, Gravity.CENTER_VERTICAL | Gravity.LEFT, i * 14, 0, 0, 0
                ));
            }
            int totalW = 28 + (count - 1) * 14;
            currentOffsetDp = totalW + 10;
        }

        private FrameLayout buildMiniCircle(TLRPC.User u, TLRPC.Chat c, int borderColor, boolean isAddSelf) {
            FrameLayout outer = new FrameLayout(ctx);
            GradientDrawable outerBg = new GradientDrawable();
            outerBg.setShape(GradientDrawable.OVAL);
            outerBg.setColor(borderColor);
            outer.setBackground(outerBg);
            outer.setPadding(AndroidUtilities.dp(1.5f), AndroidUtilities.dp(1.5f), AndroidUtilities.dp(1.5f), AndroidUtilities.dp(1.5f));

            FrameLayout ring = new FrameLayout(ctx);
            GradientDrawable ringBg = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    isAddSelf ? new int[]{0xFF2AABEE, 0xFF7C4DFF} : new int[]{0xFF00E676, 0xFF29B6F6}
            );
            ringBg.setShape(GradientDrawable.OVAL);
            ring.setBackground(ringBg);
            ring.setPadding(AndroidUtilities.dp(1.5f), AndroidUtilities.dp(1.5f), AndroidUtilities.dp(1.5f), AndroidUtilities.dp(1.5f));

            BackupImageView av = new BackupImageView(ctx);
            av.setRoundRadius(AndroidUtilities.dp(14));
            AvatarDrawable ad = new AvatarDrawable();
            if (u != null) {
                ad.setInfo(account, u);
                av.setForUserOrChat(u, ad);
            } else if (c != null) {
                ad.setInfo(account, c);
                av.setForUserOrChat(c, ad);
            }
            ring.addView(av, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            outer.addView(ring, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            if (isAddSelf) {
                TextView plus = new TextView(ctx);
                plus.setText("+");
                plus.setTextColor(Color.WHITE);
                plus.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9);
                plus.setTypeface(Typeface.DEFAULT_BOLD);
                plus.setGravity(Gravity.CENTER);
                GradientDrawable pbg = new GradientDrawable();
                pbg.setShape(GradientDrawable.OVAL);
                pbg.setColor(0xFF2AABEE);
                plus.setBackground(pbg);
                outer.addView(plus, LayoutHelper.createFrame(12, 12, Gravity.RIGHT | Gravity.BOTTOM));
            }
            return outer;
        }

        private void rebuildExpandedStrip() {
            stripRow.removeAllViews();
            MessagesController mc = MessagesController.getInstance(account);
            StoriesController sc = mc.getStoriesController();
            List<Object> peers = fetchPeerStories(sc);

            // 1. First item: My Story (○+)
            TLRPC.User self = UserConfig.getInstance(account).getCurrentUser();
            stripRow.addView(buildStoryColumn(self, null, "My Story", true, v -> {
                setExpanded(false);
                openStoryRecorder();
            }));

            // 2. Side-by-side users' stories
            long myId = UserConfig.getInstance(account).getClientUserId();
            for (int i = 0; i < peers.size(); i++) {
                final Object ps = peers.get(i);
                final long did = extractDialogId(ps);
                if (did == 0) continue;

                TLRPC.User u = did > 0 ? mc.getUser(did) : null;
                TLRPC.Chat c = did < 0 ? mc.getChat(-did) : null;
                String name;
                if (did == myId) {
                    name = "Your Story";
                } else if (u != null) {
                    name = !TextUtils.isEmpty(u.first_name) ? u.first_name : UserObject.getUserName(u);
                } else if (c != null) {
                    name = c.title;
                } else {
                    name = "Story";
                }

                stripRow.addView(buildStoryColumn(u, c, name, false, v -> {
                    setExpanded(false);
                    openPeerStory(did, ps);
                }));
            }
        }

        private View buildStoryColumn(TLRPC.User u, TLRPC.Chat c, String label, boolean isAddSelf, View.OnClickListener onClick) {
            LinearLayout col = new LinearLayout(ctx);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER_HORIZONTAL);
            col.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(2), AndroidUtilities.dp(6), AndroidUtilities.dp(2));

            FrameLayout avatarWrap = new FrameLayout(ctx);

            FrameLayout ring = new FrameLayout(ctx);
            GradientDrawable ringBg = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    isAddSelf ? new int[]{0xFF2AABEE, 0xFF7C4DFF} : new int[]{0xFF00E676, 0xFF29B6F6}
            );
            ringBg.setShape(GradientDrawable.OVAL);
            ring.setBackground(ringBg);
            ring.setPadding(AndroidUtilities.dp(2.5f), AndroidUtilities.dp(2.5f), AndroidUtilities.dp(2.5f), AndroidUtilities.dp(2.5f));

            FrameLayout innerGap = new FrameLayout(ctx);
            GradientDrawable gapBg = new GradientDrawable();
            gapBg.setShape(GradientDrawable.OVAL);
            gapBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            innerGap.setBackground(gapBg);
            innerGap.setPadding(AndroidUtilities.dp(2f), AndroidUtilities.dp(2f), AndroidUtilities.dp(2f), AndroidUtilities.dp(2f));

            BackupImageView av = new BackupImageView(ctx);
            av.setRoundRadius(AndroidUtilities.dp(26));
            AvatarDrawable ad = new AvatarDrawable();
            if (u != null) {
                ad.setInfo(account, u);
                av.setForUserOrChat(u, ad);
            } else if (c != null) {
                ad.setInfo(account, c);
                av.setForUserOrChat(c, ad);
            }
            innerGap.addView(av, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            ring.addView(innerGap, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            avatarWrap.addView(ring, LayoutHelper.createFrame(58, 58, Gravity.CENTER));

            if (isAddSelf) {
                TextView plus = new TextView(ctx);
                plus.setText("+");
                plus.setTextColor(Color.WHITE);
                plus.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                plus.setTypeface(Typeface.DEFAULT_BOLD);
                plus.setGravity(Gravity.CENTER);
                GradientDrawable pbg = new GradientDrawable();
                pbg.setShape(GradientDrawable.OVAL);
                pbg.setColor(0xFF2AABEE);
                pbg.setStroke(AndroidUtilities.dp(2f), Theme.getColor(Theme.key_windowBackgroundWhite));
                plus.setBackground(pbg);
                avatarWrap.addView(plus, LayoutHelper.createFrame(22, 22, Gravity.RIGHT | Gravity.BOTTOM));
            }

            col.addView(avatarWrap, LayoutHelper.createLinear(60, 60));

            TextView tv = new TextView(ctx);
            tv.setText(TextUtils.isEmpty(label) ? "Story" : label);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            tv.setSingleLine(true);
            tv.setEllipsize(TextUtils.TruncateAt.END);
            tv.setGravity(Gravity.CENTER);
            tv.setMaxWidth(AndroidUtilities.dp(66));
            tv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            col.addView(tv, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 0));

            col.setOnClickListener(onClick);
            return col;
        }

        private void openStoryRecorder() {
            try {
                View fab = fragment.getDetorStoriesFab();
                if (fab != null && fab.performClick()) {
                    return;
                }
            } catch (Throwable ignored) {
            }
            try {
                Activity act = fragment.getParentActivity();
                if (act == null) return;
                Object recorder = StoryRecorder.getInstance(act, account);
                for (Method m : recorder.getClass().getMethods()) {
                    if (m.getName().equals("open") && m.getParameterTypes().length == 1) {
                        m.invoke(recorder, new Object[]{null});
                        return;
                    }
                }
            } catch (Throwable e) {
                Toast.makeText(ctx, "Use camera button to add story", Toast.LENGTH_SHORT).show();
            }
        }

        private void openPeerStory(long dialogId, Object peerStories) {
            try {
                Activity act = fragment.getParentActivity();
                Object viewer = fragment.getOrCreateStoryViewer();
                if (act == null || viewer == null) return;
                for (Method m : viewer.getClass().getMethods()) {
                    if (!m.getName().equals("open")) continue;
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length == 3 && p[0] == Context.class && p[1] == long.class) {
                        m.invoke(viewer, act, dialogId, null);
                        return;
                    }
                }
                for (Method m : viewer.getClass().getMethods()) {
                    if (!m.getName().equals("open")) continue;
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length == 3 && p[0] == Context.class && peerStories != null && p[1].isAssignableFrom(peerStories.getClass())) {
                        m.invoke(viewer, act, peerStories, null);
                        return;
                    }
                }
            } catch (Throwable e) {
                Toast.makeText(ctx, "Unable to open story", Toast.LENGTH_SHORT).show();
            }
        }
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
}
