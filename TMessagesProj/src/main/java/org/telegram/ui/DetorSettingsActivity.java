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
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ScaleStateListAnimator;

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
        contentList.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(34));
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
            case PAGE_AUTOREPLY: return "Auto-Reply (Away)";
            case PAGE_ANTISPAM: return "Smart Anti-Spam";
            case PAGE_UI: return "Chat Hub & UI";
            default: return "DetorGram Settings";
        }
    }

    private String getAccountDisplayLabel() {
        TLRPC.User u = UserConfig.getInstance(currentAccount).getCurrentUser();
        if (u == null) return "Account #" + (currentAccount + 1);
        String name = UserObject.getUserName(u);
        String uname = UserObject.getPublicUsername(u);
        return TextUtils.isEmpty(uname) ? name : (name + " (@" + uname + ")");
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
        // Premium Gradient Hero Card
        LinearLayout banner = new LinearLayout(ctx);
        banner.setOrientation(LinearLayout.VERTICAL);
        banner.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(18), AndroidUtilities.dp(20), AndroidUtilities.dp(18));
        GradientDrawable bannerBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF2FB8FF, 0xFF6C3BFF, 0xFFA83BFF}
        );
        bannerBg.setCornerRadius(AndroidUtilities.dp(20));
        bannerBg.setStroke(AndroidUtilities.dp(1), 0x44FFFFFF);
        banner.setBackground(bannerBg);
        banner.setElevation(AndroidUtilities.dp(6));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.bottomMargin = AndroidUtilities.dp(16);
        banner.setLayoutParams(blp);

        TextView bt = new TextView(ctx);
        bt.setText("⚡ DetorGram Pro");
        bt.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        bt.setTypeface(Typeface.DEFAULT_BOLD);
        bt.setTextColor(Color.WHITE);
        banner.addView(bt);

        TextView bs = new TextView(ctx);
        bs.setText("Active Account: " + getAccountDisplayLabel());
        bs.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        bs.setTypeface(Typeface.DEFAULT_BOLD);
        bs.setTextColor(0xF0FFFFFF);
        bs.setPadding(0, AndroidUtilities.dp(5), 0, 0);
        banner.addView(bs);

        TextView bsub = new TextView(ctx);
        bsub.setText("Privacy, Bypass, Per-Account Auto-Reply & 3-Strike Anti-Spam.");
        bsub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        bsub.setTextColor(0xCCFFFFFF);
        bsub.setPadding(0, AndroidUtilities.dp(3), 0, 0);
        banner.addView(bsub);
        contentList.addView(banner);

        // Core Sections Card
        LinearLayout secCard = makeSectionCard(ctx, "Core Modules");
        secCard.addView(makeNavRow(ctx, "👻", 0xFF00BFA5, 0xFF00796B, "Ghost & Privacy",
                "Hide Seen, Typing, Always Offline, Anon Story", null, true,
                v -> presentFragment(new DetorSettingsActivity(PAGE_PRIVACY))));
        secCard.addView(makeNavRow(ctx, "🛡️", 0xFF7C4DFF, 0xFF512DA8, "Media & Bypass",
                "Anti-Delete, Save Restricted, View-Once, No Ads", null, true,
                v -> presentFragment(new DetorSettingsActivity(PAGE_MEDIA))));
        secCard.addView(makeNavRow(ctx, "⚡", 0xFF29B6F6, 0xFF0277BD, "Chat Hub & Interface",
                "Floating Hub, ID & DC, Stories Bar, Call Confirm", null, false,
                v -> presentFragment(new DetorSettingsActivity(PAGE_UI))));
        contentList.addView(secCard);

        // Automation & Protection (Per-Account)
        LinearLayout autoCard = makeSectionCard(ctx, "Per-Account Automation");
        autoCard.addView(makeNavRow(ctx, "🌙", 0xFFFFB74D, 0xFFE65100, "Auto-Reply (Away Mode)",
                "Only for " + getAccountDisplayLabel(),
                DetorConfig.isAutoReplyEnabled(currentAccount) ? "ACTIVE" : "OFF", true,
                v -> presentFragment(new DetorSettingsActivity(PAGE_AUTOREPLY))));
        autoCard.addView(makeNavRow(ctx, "🚫", 0xFFFF5252, 0xFFC62828, "Smart Anti-Spam Guard",
                "3 warnings -> Auto-Block • Per-account",
                DetorConfig.isAntiSpamEnabled(currentAccount) ? "ACTIVE" : "OFF", false,
                v -> presentFragment(new DetorSettingsActivity(PAGE_ANTISPAM))));
        contentList.addView(autoCard);

        // Developer Card
        LinearLayout devCard = makeSectionCard(ctx, "Developer Credit");
        devCard.addView(makeNavRow(ctx, "👨‍💻", 0xFF42A5F5, 0xFF1565C0, "Made by - Detor", "@iTsDetor", null, true,
                v -> Browser.openUrl(getParentActivity(), "https://t.me/iTsDetor")));
        devCard.addView(makeNavRow(ctx, "📢", 0xFFAB47BC, 0xFF6A1B9A, "Dev - Channel", "@DetorLab", null, false,
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
        addSwitch(ctx, card, "Save Restricted (Users, Channels & Bots)", DetorConfig.isSaveRestricted(), DetorConfig::setSaveRestricted, true);
        addSwitch(ctx, card, "View-Once Save & Screenshot", DetorConfig.isViewOnceSave(), DetorConfig::setViewOnceSave, true);
        addSwitch(ctx, card, "Hide Sponsored Ads", DetorConfig.isHideSponsored(), DetorConfig::setHideSponsored, true);
        addSwitch(ctx, card, "Multi-Story Limit Bypass", DetorConfig.isMultiStoryUnlock(), DetorConfig::setMultiStoryUnlock, false);
        contentList.addView(card);
    }

    // ==================== SUB-PAGE 3: AUTO-REPLY (PER-ACCOUNT) ====================

    private void buildAutoReplyPage(Context ctx) {
        contentList.addView(makeAccountScopeBanner(ctx, 0xFFFF9800, 0xFFE65100));

        LinearLayout card = makeSectionCard(ctx, "Away Auto-Reply");
        addSwitch(ctx, card, "Enable Auto-Reply (This Account)", DetorConfig.isAutoReplyEnabled(currentAccount), v -> {
            DetorConfig.setAutoReplyEnabled(currentAccount, v);
            toast(ctx, v ? "Auto-Reply ON for " + getAccountDisplayLabel() : "Auto-Reply OFF");
        }, true);
        addSwitch(ctx, card, "Only When App in Background (Away)", DetorConfig.isAutoReplyBgOnly(currentAccount),
                v -> DetorConfig.setAutoReplyBgOnly(currentAccount, v), true);
        addSwitch(ctx, card, "Non-Contacts Only", DetorConfig.isAutoReplyNonContactsOnly(currentAccount),
                v -> DetorConfig.setAutoReplyNonContactsOnly(currentAccount, v), false);
        contentList.addView(card);

        LinearLayout cfg = makeSectionCard(ctx, "Reply Message & Rules");
        cfg.addView(makeActionRow(ctx, "Away Message Text", DetorConfig.getAutoReplyText(currentAccount), true, v -> showEditDialog(
                ctx, "Edit Away Message", DetorConfig.getAutoReplyText(currentAccount), true, val -> {
                    DetorConfig.setAutoReplyText(currentAccount, val);
                    rebuildContent(ctx);
                }
        )));
        cfg.addView(makeActionRow(ctx, "Cooldown Per User",
                DetorConfig.getAutoReplyCooldownMin(currentAccount) + " minutes (prevents reply loops)", true, v -> {
            final String[] labels = {"5 minutes", "15 minutes", "30 minutes", "1 hour", "6 hours", "24 hours"};
            final int[] mins = {5, 15, 30, 60, 360, 1440};
            new AlertDialog.Builder(ctx).setTitle("Auto-Reply Cooldown").setItems(labels, (d, which) -> {
                DetorConfig.setAutoReplyCooldownMin(currentAccount, mins[which]);
                rebuildContent(ctx);
            }).show();
        }));
        cfg.addView(makeActionRow(ctx, "Whitelisted User IDs",
                TextUtils.isEmpty(DetorConfig.getGuardWhitelist(currentAccount)) ? "None (tap to add comma-separated IDs)" : DetorConfig.getGuardWhitelist(currentAccount),
                false, v -> showEditDialog(ctx, "Whitelist User IDs (comma separated)", DetorConfig.getGuardWhitelist(currentAccount), false, val -> {
                    DetorConfig.setGuardWhitelist(currentAccount, val);
                    rebuildContent(ctx);
                })));
        contentList.addView(cfg);

        addFootnote(ctx, "Ye setting sirf current active account (" + getAccountDisplayLabel() + ") par apply hogi. Dusre accounts par iska koi asar nahi padega.");
    }

    // ==================== SUB-PAGE 4: SMART ANTI-SPAM (PER-ACCOUNT) ====================

    private void buildAntiSpamPage(Context ctx) {
        contentList.addView(makeAccountScopeBanner(ctx, 0xFFFF5252, 0xFFC62828));

        LinearLayout card = makeSectionCard(ctx, "3-Warning Auto-Block Engine");
        addSwitch(ctx, card, "Enable Anti-Spam Guard (This Account)", DetorConfig.isAntiSpamEnabled(currentAccount), v -> {
            DetorConfig.setAntiSpamEnabled(currentAccount, v);
            toast(ctx, v ? "Anti-Spam ON for " + getAccountDisplayLabel() : "Anti-Spam OFF");
        }, true);
        addSwitch(ctx, card, "Ignore Saved Contacts", DetorConfig.isAntiSpamIgnoreContacts(currentAccount),
                v -> DetorConfig.setAntiSpamIgnoreContacts(currentAccount, v), true);
        addSwitch(ctx, card, "Detect Repeated Text Spam", DetorConfig.isAntiSpamRepeatDetect(currentAccount),
                v -> DetorConfig.setAntiSpamRepeatDetect(currentAccount, v), true);
        addSwitch(ctx, card, "Detect Unsolicited Links (t.me / http)", DetorConfig.isAntiSpamLinkDetect(currentAccount),
                v -> DetorConfig.setAntiSpamLinkDetect(currentAccount, v), false);
        contentList.addView(card);

        LinearLayout rules = makeSectionCard(ctx, "Sensitivity & Messages");
        rules.addView(makeActionRow(ctx, "Flood Sensitivity",
                DetorConfig.getAntiSpamThreshold(currentAccount) + " messages within 8 seconds = 1 Strike", true,
                v -> {
                    final String[] labels = {"Strict (4 msgs / 8s)", "Balanced (5 msgs / 8s)", "Relaxed (8 msgs / 8s)", "Heavy Flood Only (12 msgs / 8s)"};
                    final int[] vals = {4, 5, 8, 12};
                    new AlertDialog.Builder(ctx).setTitle("Flood Sensitivity").setItems(labels, (d, which) -> {
                        DetorConfig.setAntiSpamThreshold(currentAccount, vals[which]);
                        rebuildContent(ctx);
                    }).show();
                }));
        rules.addView(makeActionRow(ctx, "Warning Message Template",
                DetorConfig.getAntiSpamWarnMsg(currentAccount), true,
                v -> showEditDialog(ctx, "Warning Msg ({count}, {reason})", DetorConfig.getAntiSpamWarnMsg(currentAccount), true, val -> {
                    DetorConfig.setAntiSpamWarnMsg(currentAccount, val);
                    rebuildContent(ctx);
                })));
        rules.addView(makeActionRow(ctx, "Final Auto-Block Message",
                DetorConfig.getAntiSpamBlockMsg(currentAccount), true,
                v -> showEditDialog(ctx, "Block Notice ({reason})", DetorConfig.getAntiSpamBlockMsg(currentAccount), true, val -> {
                    DetorConfig.setAntiSpamBlockMsg(currentAccount, val);
                    rebuildContent(ctx);
                })));
        rules.addView(makeActionRow(ctx, "Whitelisted User IDs",
                TextUtils.isEmpty(DetorConfig.getGuardWhitelist(currentAccount)) ? "None (tap to add)" : DetorConfig.getGuardWhitelist(currentAccount),
                false, v -> showEditDialog(ctx, "Whitelist User IDs (comma separated)", DetorConfig.getGuardWhitelist(currentAccount), false, val -> {
                    DetorConfig.setGuardWhitelist(currentAccount, val);
                    rebuildContent(ctx);
                })));
        contentList.addView(rules);

        LinearLayout logs = makeSectionCard(ctx, "Logs & Reset");
        logs.addView(makeActionRow(ctx, "View Auto-Blocked Spammers Log",
                DetorConfig.getSpamLog(currentAccount).size() + " blocked on this account", true,
                v -> {
                    List<String> list = DetorConfig.getSpamLog(currentAccount);
                    String msg = list.isEmpty() ? "No spammers blocked yet on this account." : TextUtils.join("\n\n", list);
                    new AlertDialog.Builder(ctx)
                            .setTitle("Anti-Spam Block Log")
                            .setMessage(msg)
                            .setPositiveButton("OK", null)
                            .setNeutralButton("Clear Log", (d, w) -> {
                                DetorConfig.clearSpamLog(currentAccount);
                                rebuildContent(ctx);
                            })
                            .show();
                }));
        logs.addView(makeActionRow(ctx, "Reset Strike Counters", "Clears warning counts for this account", false, v -> {
            DetorAutoGuard.clearSpamMemory(currentAccount);
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

    // ==================== PREMIUM UI HELPERS ====================

    private View makeAccountScopeBanner(Context ctx, int cStart, int cEnd) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(10), AndroidUtilities.dp(14), AndroidUtilities.dp(10));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{cStart, cEnd});
        bg.setCornerRadius(AndroidUtilities.dp(14));
        box.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = AndroidUtilities.dp(12);
        box.setLayoutParams(lp);

        TextView tv = new TextView(ctx);
        tv.setText("👤 Specific Account Only: " + getAccountDisplayLabel());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(Color.WHITE);
        box.addView(tv);
        return box;
    }

    private LinearLayout makeSectionCard(Context ctx, String headerTitle) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(18));
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        bg.setStroke(AndroidUtilities.dp(1), Theme.isCurrentThemeDark() ? 0x1AFFFFFF : 0x12000000);
        card.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = AndroidUtilities.dp(14);
        card.setLayoutParams(lp);
        card.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(6));

        HeaderCell hc = new HeaderCell(ctx);
        hc.setText(headerTitle);
        card.addView(hc);
        return card;
    }

    private View makeNavRow(Context ctx, String emoji, int cTop, int cBottom, String title, String sub, String statusPill, boolean divider, View.OnClickListener click) {
        LinearLayout wrap = new LinearLayout(ctx);
        wrap.setOrientation(LinearLayout.VERTICAL);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        row.setBackground(Theme.getSelectorDrawable(false));
        ScaleStateListAnimator.apply(row);

        TextView icon = new TextView(ctx);
        icon.setText(emoji);
        icon.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        icon.setGravity(Gravity.CENTER);
        GradientDrawable ibg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{cTop, cBottom});
        ibg.setCornerRadius(AndroidUtilities.dp(12));
        ibg.setStroke(AndroidUtilities.dp(1), 0x33FFFFFF);
        icon.setBackground(ibg);
        row.addView(icon, LayoutHelper.createLinear(38, 38, Gravity.CENTER_VERTICAL, 0, 0, 14, 0));

        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);

        TextView t = new TextView(ctx);
        t.setText(title);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15.5f);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        col.addView(t);

        TextView s = new TextView(ctx);
        s.setText(sub);
        s.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        s.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        col.addView(s);

        row.addView(col, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

        if (!TextUtils.isEmpty(statusPill)) {
            boolean active = "ACTIVE".equals(statusPill);
            TextView pill = new TextView(ctx);
            pill.setText(statusPill);
            pill.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10.5f);
            pill.setTypeface(Typeface.DEFAULT_BOLD);
            pill.setTextColor(Color.WHITE);
            pill.setPadding(AndroidUtilities.dp(9), AndroidUtilities.dp(4), AndroidUtilities.dp(9), AndroidUtilities.dp(4));
            GradientDrawable pbg = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    active ? new int[]{0xFF00C853, 0xFF009688} : new int[]{0xFF616161, 0xFF424242}
            );
            pbg.setCornerRadius(AndroidUtilities.dp(10));
            pill.setBackground(pbg);
            row.addView(pill, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 8, 0, 4, 0));
        }

        TextView chev = new TextView(ctx);
        chev.setText("›");
        chev.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        chev.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        row.addView(chev, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 6, 0, 0, 0));

        row.setOnClickListener(click);
        wrap.addView(row);

        if (divider) {
            View div = new View(ctx);
            div.setBackgroundColor(Theme.isCurrentThemeDark() ? 0x18FFFFFF : 0x10000000);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
            dlp.leftMargin = AndroidUtilities.dp(68);
            wrap.addView(div, dlp);
        }
        return wrap;
    }

    private View makeActionRow(Context ctx, String title, String sub, boolean divider, View.OnClickListener click) {
        LinearLayout wrap = new LinearLayout(ctx);
        wrap.setOrientation(LinearLayout.VERTICAL);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(12), AndroidUtilities.dp(20), AndroidUtilities.dp(12));
        row.setBackground(Theme.getSelectorDrawable(false));

        TextView t = new TextView(ctx);
        t.setText(title);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        row.addView(t);

        TextView s = new TextView(ctx);
        s.setText(sub);
        s.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        s.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        s.setPadding(0, AndroidUtilities.dp(2), 0, 0);
        row.addView(s);

        row.setOnClickListener(click);
        wrap.addView(row);

        if (divider) {
            View div = new View(ctx);
            div.setBackgroundColor(Theme.isCurrentThemeDark() ? 0x18FFFFFF : 0x10000000);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
            dlp.leftMargin = AndroidUtilities.dp(20);
            wrap.addView(div, dlp);
        }
        return wrap;
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
