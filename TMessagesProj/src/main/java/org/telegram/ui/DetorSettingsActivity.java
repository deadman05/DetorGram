package org.telegram.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DetorAutoGuard;
import org.telegram.messenger.DetorConfig;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.LayoutHelper;

import java.util.List;

public class DetorSettingsActivity extends BaseFragment {

    public static final int PAGE_MAIN = 0;
    public static final int PAGE_PRIVACY = 1;
    public static final int PAGE_MEDIA = 2;
    public static final int PAGE_AUTOREPLY = 3;
    public static final int PAGE_ANTISPAM = 4;
    public static final int PAGE_UI = 5;

    private final int page;
    private LinearLayout contentList;

    public DetorSettingsActivity() {
        this(PAGE_MAIN);
    }

    public DetorSettingsActivity(int page) {
        super();
        this.page = page;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(getPageTitle());
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        contentList = new LinearLayout(context);
        contentList.setOrientation(LinearLayout.VERTICAL);
        contentList.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(32));
        scroll.addView(contentList, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        rebuildContent(context);
        fragmentView = scroll;
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (contentList != null && getContext() != null) {
            rebuildContent(getContext());
        }
    }

    private String getPageTitle() {
        switch (page) {
            case PAGE_PRIVACY: return "Ghost & Privacy";
            case PAGE_MEDIA: return "Media & Bypass";
            case PAGE_AUTOREPLY: return "Auto-Reply (Away Mode)";
            case PAGE_ANTISPAM: return "Smart Anti-Spam Guard";
            case PAGE_UI: return "Chat Hub & Interface";
            default: return "DetorGram Settings";
        }
    }

    private void rebuildContent(Context ctx) {
        contentList.removeAllViews();
        switch (page) {
            case PAGE_MAIN: buildMainPage(ctx); break;
            case PAGE_PRIVACY: buildPrivacyPage(ctx); break;
            case PAGE_MEDIA: buildMediaPage(ctx); break;
            case PAGE_AUTOREPLY: buildAutoReplyPage(ctx); break;
            case PAGE_ANTISPAM: buildAntiSpamPage(ctx); break;
            case PAGE_UI: buildUiPage(ctx); break;
        }
    }

    // ==================== MAIN HUB PAGE ====================

    private void buildMainPage(Context ctx) {
        // Banner card
        LinearLayout banner = new LinearLayout(ctx);
        banner.setOrientation(LinearLayout.VERTICAL);
        banner.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(18));
        GradientDrawable bannerBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF6C3BFF, 0xFF1E88E5}
        );
        bannerBg.setCornerRadius(AndroidUtilities.dp(18));
        banner.setBackground(bannerBg);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.bottomMargin = AndroidUtilities.dp(14);
        banner.setLayoutParams(blp);

        TextView bt = new TextView(ctx);
        bt.setText("⚡ DetorGram Pro Control");
        bt.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19);
        bt.setTypeface(Typeface.DEFAULT_BOLD);
        bt.setTextColor(Color.WHITE);
        banner.addView(bt);

        TextView bs = new TextView(ctx);
        bs.setText("Privacy, Bypass, Away Auto-Reply & 3-Strike Anti-Spam — All organized in dedicated sections.");
        bs.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        bs.setTextColor(0xE0FFFFFF);
        bs.setPadding(0, AndroidUtilities.dp(4), 0, 0);
        banner.addView(bs);
        contentList.addView(banner);

        // Core Sections Card
        LinearLayout secCard = makeSectionCard(ctx, "Core Features");
        secCard.addView(makeNavRow(ctx, "GHOST", 0xFF009688, "Ghost & Privacy",
                "Hide Seen, Typing, Always Offline, Anon Story", null,
                v -> presentFragment(new DetorSettingsActivity(PAGE_PRIVACY))));
        secCard.addView(makeNavRow(ctx, "BYPASS", 0xFF7C4DFF, "Media & Bypass",
                "Anti-Delete, Save Restricted (Bots/Channels), View-Once", null,
                v -> presentFragment(new DetorSettingsActivity(PAGE_MEDIA))));
        secCard.addView(makeNavRow(ctx, "UI", 0xFF2196F3, "Chat Hub & Interface",
                "Floating ⚡ Hub, ID & DC, Stories Bar, Call Confirm", null,
                v -> presentFragment(new DetorSettingsActivity(PAGE_UI))));
        contentList.addView(secCard);

        // Automation & Security Card (Business-style)
        LinearLayout autoCard = makeSectionCard(ctx, "Automation & Protection");
        autoCard.addView(makeNavRow(ctx, "AWAY", 0xFFFF9800, "Auto-Reply (Away Mode)",
                "Smart offline auto-responder with cooldown",
                DetorConfig.isAutoReplyEnabled() ? "ON" : "OFF",
                v -> presentFragment(new DetorSettingsActivity(PAGE_AUTOREPLY))));
        autoCard.addView(makeNavRow(ctx, "GUARD", 0xFFE53935, "Smart Anti-Spam Guard",
                "Flood, repeat & link detection • 3 warnings -> Auto-Block",
                DetorConfig.isAntiSpamEnabled() ? "ON" : "OFF",
                v -> presentFragment(new DetorSettingsActivity(PAGE_ANTISPAM))));
        contentList.addView(autoCard);

        // Developer Card
        LinearLayout devCard = makeSectionCard(ctx, "Developer Credit");
        devCard.addView(makeNavRow(ctx, "DEV", 0xFF1E88E5, "Made by - Detor", "@iTsDetor", null,
                v -> Browser.openUrl(getParentActivity(), "https://t.me/iTsDetor")));
        devCard.addView(makeNavRow(ctx, "CH", 0xFF8E24AA, "Dev - Channel", "@DetorLab", null,
                v -> Browser.openUrl(getParentActivity(), "https://t.me/+FJWj9qdmM3g1MjRl")));
        contentList.addView(devCard);
    }

    // ==================== SUB-PAGE 1: PRIVACY ====================

    private void buildPrivacyPage(Context ctx) {
        LinearLayout card = makeSectionCard(ctx, "Ghost Mode Toggles");
        addSwitch(ctx, card, "Hide Seen (Ghost Read)", DetorConfig.isHideSeen(), DetorConfig::setHideSeen, true);
        addSwitch(ctx, card, "Hide Typing / Recording", DetorConfig.isHideTyping(), DetorConfig::setHideTyping, true);
        addSwitch(ctx, card, "Always Offline", DetorConfig.isAlwaysOffline(), DetorConfig::setAlwaysOffline, true);
        addSwitch(ctx, card, "Anonymous Story View", DetorConfig.isAnonStoryView(), DetorConfig::setAnonStoryView, false);
        contentList.addView(card);
    }

    // ==================== SUB-PAGE 2: MEDIA & BYPASS ====================

    private void buildMediaPage(Context ctx) {
        LinearLayout card = makeSectionCard(ctx, "Anti-Delete & Restrictions");
        addSwitch(ctx, card, "Anti-Delete + Deleted Tag", DetorConfig.isAntiDelete(), DetorConfig::setAntiDelete, true);
        addSwitch(ctx, card, "Save Restricted (Channels & Bots + Auto Re-Send)", DetorConfig.isSaveRestricted(), DetorConfig::setSaveRestricted, true);
        addSwitch(ctx, card, "View-Once Save & Screenshot", DetorConfig.isViewOnceSave(), DetorConfig::setViewOnceSave, true);
        addSwitch(ctx, card, "Hide Sponsored Ads", DetorConfig.isHideSponsored(), DetorConfig::setHideSponsored, true);
        addSwitch(ctx, card, "Multi-Story Limit Bypass", DetorConfig.isMultiStoryUnlock(), DetorConfig::setMultiStoryUnlock, false);
        contentList.addView(card);
    }

    // ==================== SUB-PAGE 3: AUTO-REPLY ====================

    private void buildAutoReplyPage(Context ctx) {
        LinearLayout card = makeSectionCard(ctx, "Away Auto-Reply");
        addSwitch(ctx, card, "Enable Auto-Reply", DetorConfig.isAutoReplyEnabled(), v -> {
            DetorConfig.setAutoReplyEnabled(v);
            toast(ctx, v ? "Auto-Reply Enabled" : "Auto-Reply Disabled");
        }, true);
        addSwitch(ctx, card, "Only When App in Background (Offline/Away)", DetorConfig.isAutoReplyBgOnly(), DetorConfig::setAutoReplyBgOnly, true);
        addSwitch(ctx, card, "Non-Contacts Only (Ignore Saved Contacts)", DetorConfig.isAutoReplyNonContactsOnly(), DetorConfig::setAutoReplyNonContactsOnly, false);
        contentList.addView(card);

        LinearLayout cfg = makeSectionCard(ctx, "Reply Message & Rules");
        cfg.addView(makeActionRow(ctx, "Away Message Text", DetorConfig.getAutoReplyText(), v -> showEditDialog(
                ctx, "Edit Away Message", DetorConfig.getAutoReplyText(), true, val -> {
                    DetorConfig.setAutoReplyText(val);
                    rebuildContent(ctx);
                }
        )));
        cfg.addView(makeActionRow(ctx, "Cooldown Per User", DetorConfig.getAutoReplyCooldownMin() + " minutes (prevents spam loops)", v -> {
            final String[] labels = {"5 minutes", "15 minutes", "30 minutes", "1 hour", "6 hours", "24 hours"};
            final int[] mins = {5, 15, 30, 60, 360, 1440};
            new AlertDialog.Builder(ctx).setTitle("Auto-Reply Cooldown").setItems(labels, (d, which) -> {
                DetorConfig.setAutoReplyCooldownMin(mins[which]);
                rebuildContent(ctx);
            }).show();
        }));
        cfg.addView(makeActionRow(ctx, "Whitelisted User IDs",
                TextUtils.isEmpty(DetorConfig.getGuardWhitelist()) ? "None (tap to add comma-separated IDs)" : DetorConfig.getGuardWhitelist(),
                v -> showEditDialog(ctx, "Whitelist User IDs (comma separated)", DetorConfig.getGuardWhitelist(), false, val -> {
                    DetorConfig.setGuardWhitelist(val);
                    rebuildContent(ctx);
                })));
        contentList.addView(cfg);

        addFootnote(ctx, "Auto-Reply works automatically for private DMs whenever DetorGram is active in background/notifications. Bots & verified system chats are always skipped.");
    }

    // ==================== SUB-PAGE 4: SMART ANTI-SPAM ====================

    private void buildAntiSpamPage(Context ctx) {
        LinearLayout card = makeSectionCard(ctx, "3-Warning Auto-Block Engine");
        addSwitch(ctx, card, "Enable Smart Anti-Spam Guard", DetorConfig.isAntiSpamEnabled(), v -> {
            DetorConfig.setAntiSpamEnabled(v);
            toast(ctx, v ? "Anti-Spam Guard ON" : "Anti-Spam Guard OFF");
        }, true);
        addSwitch(ctx, card, "Ignore Saved Contacts", DetorConfig.isAntiSpamIgnoreContacts(), DetorConfig::setAntiSpamIgnoreContacts, true);
        addSwitch(ctx, card, "Detect Repeated Text Spam", DetorConfig.isAntiSpamRepeatDetect(), DetorConfig::setAntiSpamRepeatDetect, true);
        addSwitch(ctx, card, "Detect Unsolicited Links (t.me / http)", DetorConfig.isAntiSpamLinkDetect(), DetorConfig::setAntiSpamLinkDetect, false);
        contentList.addView(card);

        LinearLayout rules = makeSectionCard(ctx, "Sensitivity & Messages");
        rules.addView(makeActionRow(ctx, "Flood Sensitivity",
                DetorConfig.getAntiSpamThreshold() + " messages within 8 seconds = 1 Strike",
                v -> {
                    final String[] labels = {"Strict (4 msgs / 8s)", "Balanced (5 msgs / 8s)", "Relaxed (8 msgs / 8s)", "Heavy Flood Only (12 msgs / 8s)"};
                    final int[] vals = {4, 5, 8, 12};
                    new AlertDialog.Builder(ctx).setTitle("Flood Sensitivity").setItems(labels, (d, which) -> {
                        DetorConfig.setAntiSpamThreshold(vals[which]);
                        rebuildContent(ctx);
                    }).show();
                }));
        rules.addView(makeActionRow(ctx, "Warning Message Template",
                DetorConfig.getAntiSpamWarnMsg(),
                v -> showEditDialog(ctx, "Warning Msg ({count}, {reason})", DetorConfig.getAntiSpamWarnMsg(), true, val -> {
                    DetorConfig.setAntiSpamWarnMsg(val);
                    rebuildContent(ctx);
                })));
        rules.addView(makeActionRow(ctx, "Final Auto-Block Message",
                DetorConfig.getAntiSpamBlockMsg(),
                v -> showEditDialog(ctx, "Block Notice ({reason})", DetorConfig.getAntiSpamBlockMsg(), true, val -> {
                    DetorConfig.setAntiSpamBlockMsg(val);
                    rebuildContent(ctx);
                })));
        rules.addView(makeActionRow(ctx, "Whitelisted User IDs",
                TextUtils.isEmpty(DetorConfig.getGuardWhitelist()) ? "None (tap to add)" : DetorConfig.getGuardWhitelist(),
                v -> showEditDialog(ctx, "Whitelist User IDs (comma separated)", DetorConfig.getGuardWhitelist(), false, val -> {
                    DetorConfig.setGuardWhitelist(val);
                    rebuildContent(ctx);
                })));
        contentList.addView(rules);

        LinearLayout logs = makeSectionCard(ctx, "Logs & Reset");
        logs.addView(makeActionRow(ctx, "View Auto-Blocked Spammers Log",
                DetorConfig.getSpamLog().size() + " blocked entries",
                v -> {
                    List<String> list = DetorConfig.getSpamLog();
                    String msg = list.isEmpty() ? "No spammers blocked yet." : TextUtils.join("\n\n", list);
                    new AlertDialog.Builder(ctx)
                            .setTitle("Anti-Spam Block Log")
                            .setMessage(msg)
                            .setPositiveButton("OK", null)
                            .setNeutralButton("Clear Log", (d, w) -> {
                                DetorConfig.clearSpamLog();
                                rebuildContent(ctx);
                            })
                            .show();
                }));
        logs.addView(makeActionRow(ctx, "Reset Strike Counters", "Clears active warning counts in memory", v -> {
            DetorAutoGuard.clearSpamMemory();
            toast(ctx, "Warning counters cleared");
        }));
        contentList.addView(logs);
    }

    // ==================== SUB-PAGE 5: UI & CHAT HUB ====================

    private void buildUiPage(Context ctx) {
        LinearLayout card = makeSectionCard(ctx, "Interface & Utilities");
        addSwitch(ctx, card, "Show Chat Floating Hub (⚡)", DetorConfig.isShowChatFab(), DetorConfig::setShowChatFab, true);
        addSwitch(ctx, card, "Show User / Chat ID & DC", DetorConfig.isShowIdDc(), DetorConfig::setShowIdDc, true);
        addSwitch(ctx, card, "Confirm Before Call", DetorConfig.isConfirmCall(), DetorConfig::setConfirmCall, true);
        addSwitch(ctx, card, "Hide Home Stories Bar", DetorConfig.isHideStoriesBar(), DetorConfig::setHideStoriesBar, true);
        addSwitch(ctx, card, "Forward Without Quote Default", DetorConfig.isForwardNoQuote(), DetorConfig::setForwardNoQuote, false);
        contentList.addView(card);
    }

    // ==================== UI BUILDER HELPERS ====================

    private LinearLayout makeSectionCard(Context ctx, String headerTitle) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(16));
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        card.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = AndroidUtilities.dp(14);
        card.setLayoutParams(lp);
        card.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(8));

        HeaderCell hc = new HeaderCell(ctx);
        hc.setText(headerTitle);
        card.addView(hc);
        return card;
    }

    private View makeNavRow(Context ctx, String badge, int badgeColor, String title, String sub, String statusPill, View.OnClickListener click) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(11), AndroidUtilities.dp(16), AndroidUtilities.dp(11));
        row.setBackground(Theme.getSelectorDrawable(false));

        TextView icon = new TextView(ctx);
        icon.setText(badge);
        icon.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
        icon.setTypeface(Typeface.DEFAULT_BOLD);
        icon.setTextColor(Color.WHITE);
        icon.setGravity(Gravity.CENTER);
        GradientDrawable ibg = new GradientDrawable();
        ibg.setCornerRadius(AndroidUtilities.dp(10));
        ibg.setColor(badgeColor);
        icon.setBackground(ibg);
        row.addView(icon, LayoutHelper.createLinear(44, 34, Gravity.CENTER_VERTICAL, 0, 0, 14, 0));

        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);

        TextView t = new TextView(ctx);
        t.setText(title);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        col.addView(t);

        TextView s = new TextView(ctx);
        s.setText(sub);
        s.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        s.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        col.addView(s);

        row.addView(col, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

        if (!TextUtils.isEmpty(statusPill)) {
            TextView pill = new TextView(ctx);
            pill.setText(statusPill);
            pill.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            pill.setTypeface(Typeface.DEFAULT_BOLD);
            pill.setTextColor(Color.WHITE);
            pill.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(3), AndroidUtilities.dp(8), AndroidUtilities.dp(3));
            GradientDrawable pbg = new GradientDrawable();
            pbg.setCornerRadius(AndroidUtilities.dp(8));
            pbg.setColor("ON".equals(statusPill) ? 0xFF43A047 : 0xFF757575);
            pill.setBackground(pbg);
            row.addView(pill, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));
        }

        row.setOnClickListener(click);
        return row;
    }

    private View makeActionRow(Context ctx, String title, String sub, View.OnClickListener click) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(11), AndroidUtilities.dp(20), AndroidUtilities.dp(11));
        row.setBackground(Theme.getSelectorDrawable(false));

        TextView t = new TextView(ctx);
        t.setText(title);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        t.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        row.addView(t);

        TextView s = new TextView(ctx);
        s.setText(sub);
        s.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        s.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        row.addView(s);

        row.setOnClickListener(click);
        return row;
    }

    private void addSwitch(Context ctx, LinearLayout parent, String title, boolean init, DetorSettingsSheet.BoolSetter setter, boolean divider) {
        TextCheckCell cell = new TextCheckCell(ctx);
        cell.setTextAndCheck(title, init, divider);
        cell.setOnClickListener(v -> {
            boolean nv = !cell.isChecked();
            cell.setChecked(nv);
            setter.set(nv);
        });
        parent.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addFootnote(Context ctx, String text) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        tv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4));
        tv.setPadding(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8), AndroidUtilities.dp(12));
        contentList.addView(tv);
    }

    private interface TextSetter {
        void onSave(String val);
    }

    private void showEditDialog(Context ctx, String title, String current, boolean multiline, TextSetter onSave) {
        EditText et = new EditText(ctx);
        et.setText(current);
        et.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        et.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        et.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        if (multiline) {
            et.setMinLines(3);
            et.setMaxLines(6);
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        }
        LinearLayout box = new LinearLayout(ctx);
        box.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(10), AndroidUtilities.dp(20), 0);
        box.addView(et, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setView(box)
                .setPositiveButton("Save", (d, w) -> onSave.onSave(et.getText().toString().trim()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toast(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }
}
