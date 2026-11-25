package org.telegram.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DetorConfig;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.LayoutHelper;

public class DetorSettingsSheet {

    public interface BoolSetter {
        void set(boolean value);
    }

    public static void show(Context context, Runnable onChanged) {
        if (context == null) return;

        ScrollView scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setBackgroundColor(Theme.getColor(Theme.key_dialogBackground));

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(16));
        scrollView.addView(container, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        // Top Title
        TextView titleView = new TextView(context);
        titleView.setText("DetorGram Features");
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        titleView.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(12), AndroidUtilities.dp(20), AndroidUtilities.dp(8));
        container.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Section 1: Privacy & Ghost Mode
        addHeader(context, container, "Privacy & Ghost Mode");
        addToggle(context, container, "Hide Seen (Ghost Read)", DetorConfig.isHideSeen(), v -> {
            DetorConfig.setHideSeen(v);
            if (onChanged != null) onChanged.run();
        }, true);
        addToggle(context, container, "Hide Typing / Recording", DetorConfig.isHideTyping(), v -> {
            DetorConfig.setHideTyping(v);
            if (onChanged != null) onChanged.run();
        }, true);
        addToggle(context, container, "Always Offline", DetorConfig.isAlwaysOffline(), v -> {
            DetorConfig.setAlwaysOffline(v);
            if (onChanged != null) onChanged.run();
        }, true);
        addToggle(context, container, "Anonymous Story View", DetorConfig.isAnonStoryView(), v -> {
            DetorConfig.setAnonStoryView(v);
            if (onChanged != null) onChanged.run();
        }, false);

        // Section 2: Anti-Delete & Media
        addHeader(context, container, "Anti-Delete & Media");
        addToggle(context, container, "Anti-Delete + Deleted Tag", DetorConfig.isAntiDelete(), v -> {
            DetorConfig.setAntiDelete(v);
            if (onChanged != null) onChanged.run();
        }, true);
        addToggle(context, container, "Save Restricted Content", DetorConfig.isSaveRestricted(), v -> {
            DetorConfig.setSaveRestricted(v);
            if (onChanged != null) onChanged.run();
        }, true);
        addToggle(context, container, "View-Once Save & Screenshot", DetorConfig.isViewOnceSave(), v -> {
            DetorConfig.setViewOnceSave(v);
            if (onChanged != null) onChanged.run();
        }, true);
        addToggle(context, container, "Hide Sponsored Ads", DetorConfig.isHideSponsored(), v -> {
            DetorConfig.setHideSponsored(v);
            if (onChanged != null) onChanged.run();
        }, false);

        // Section 3: Power-User & Utility
        addHeader(context, container, "Power-User & Utility");
        addToggle(context, container, "Show User / Chat ID & DC", DetorConfig.isShowIdDc(), v -> {
            DetorConfig.setShowIdDc(v);
            if (onChanged != null) onChanged.run();
        }, true);
        addToggle(context, container, "Confirm Before Call", DetorConfig.isConfirmCall(), v -> {
            DetorConfig.setConfirmCall(v);
            if (onChanged != null) onChanged.run();
        }, true);
        addToggle(context, container, "Hide Home Stories Bar", DetorConfig.isHideStoriesBar(), v -> {
            DetorConfig.setHideStoriesBar(v);
            if (onChanged != null) onChanged.run();
        }, true);
        addToggle(context, container, "Forward Without Quote Default", DetorConfig.isForwardNoQuote(), v -> {
            DetorConfig.setForwardNoQuote(v);
            if (onChanged != null) onChanged.run();
        }, true);
        addToggle(context, container, "Show Chat Floating Hub (⚡)", DetorConfig.isShowChatFab(), v -> {
            DetorConfig.setShowChatFab(v);
            if (onChanged != null) onChanged.run();
        }, true);
        addToggle(context, container, "Multi-Story Limit Bypass", DetorConfig.isMultiStoryUnlock(), v -> {
            DetorConfig.setMultiStoryUnlock(v);
            if (onChanged != null) onChanged.run();
        }, false);

        BottomSheet.Builder builder = new BottomSheet.Builder(context);
        builder.setCustomView(scrollView);
        builder.show();
    }

    private static void addHeader(Context context, LinearLayout parent, String title) {
        HeaderCell header = new HeaderCell(context);
        header.setText(title);
        parent.addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private static void addToggle(Context context, LinearLayout parent, String title, boolean initialValue, BoolSetter setter, boolean divider) {
        TextCheckCell cell = new TextCheckCell(context);
        cell.setTextAndCheck(title, initialValue, divider);
        cell.setOnClickListener(v -> {
            boolean newVal = !cell.isChecked();
            cell.setChecked(newVal);
            setter.set(newVal);
        });
        parent.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }
}
