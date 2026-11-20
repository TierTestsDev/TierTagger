package com.tiertests.tiertagger.core;

import com.tiertests.tiertagger.core.config.IconStyle;
import com.tiertests.tiertagger.core.config.Layout;
import com.tiertests.tiertagger.core.config.TaggerConfig;
import com.tiertests.tiertagger.core.model.GameMode;
import com.tiertests.tiertagger.core.source.TierList;

import java.util.ArrayList;
import java.util.List;

/**
 * The actions behind the settings screen, shared by every platform so the screens only do layout.
 * Each change is saved right away.
 */
public final class Settings {
    private final TierTagger tagger;

    Settings(TierTagger tagger) {
        this.tagger = tagger;
    }

    private TaggerConfig config() {
        return tagger.config();
    }

    public String primaryListLabel() {
        return tagger.primaryList().displayName();
    }

    /** The second tag's tier list, or "Off". */
    public String secondaryListLabel() {
        return config().secondary.enabled ? tagger.secondaryList().displayName() : "Off";
    }

    public String primaryModeLabel() {
        return modeLabel(config().primary);
    }

    public String secondaryModeLabel() {
        return config().secondary.enabled ? modeLabel(config().secondary) : "-";
    }

    public String layoutLabel() {
        return config().layout.label();
    }

    public boolean secondaryEnabled() {
        return config().secondary.enabled;
    }

    public boolean iconsOn() {
        return config().icons != IconStyle.NONE;
    }

    public boolean regionOn() {
        return config().showRegion;
    }

    public boolean badgeOn() {
        return config().showBadge;
    }

    public boolean tabOn() {
        return config().showInTab;
    }

    private String modeLabel(TaggerConfig.Slot slot) {
        if (TaggerConfig.Slot.BEST.equals(slot.mode)) return "Best Tier";
        if (TaggerConfig.Slot.RANK.equals(slot.mode)) return "Rank";
        GameMode mode = tagger.selectedMode(slot);
        if (mode != null) return mode.title();
        TierList list = tagger.list(slot.list);
        tagger.loadModes(list);
        return list.modesLoaded() ? "Best Tier" : "Loading...";
    }

    public void cyclePrimaryList() {
        TaggerConfig.Slot slot = config().primary;
        slot.list = next(tagger.lists(), tagger.list(slot.list)).id();
        resetMode(slot);
        avoidDuplicate(slot, config().secondary);
        tagger.configChanged();
    }

    public void cyclePrimaryMode() {
        cycleMode(config().primary, config().secondary);
    }

    /** Off, then each tier list in turn, then off again. */
    public void cycleSecondaryList() {
        TaggerConfig.Slot slot = config().secondary;
        List<TierList> lists = tagger.lists();
        if (!slot.enabled) {
            slot.enabled = true;
            slot.list = lists.get(0).id();
            if (config().layout == Layout.PREFIX) config().layout = Layout.SPLIT;
        } else {
            int index = lists.indexOf(tagger.list(slot.list));
            if (index + 1 >= lists.size()) {
                slot.enabled = false;
                if (config().layout == Layout.SPLIT) config().layout = Layout.PREFIX;
                tagger.configChanged();
                return;
            }
            slot.list = lists.get(index + 1).id();
        }
        resetMode(slot);
        avoidDuplicate(slot, config().primary);
        tagger.configChanged();
    }

    public void cycleSecondaryMode() {
        if (config().secondary.enabled) cycleMode(config().secondary, config().primary);
    }

    public void cycleLayout() {
        Layout layout = next(Layout.values(), config().layout);
        if (layout == Layout.SPLIT && !config().secondary.enabled) layout = next(Layout.values(), layout);
        config().layout = layout;
        tagger.configChanged();
    }

    public void toggleIcons() {
        config().icons = iconsOn() ? IconStyle.NONE : IconStyle.PIXEL;
        tagger.configChanged();
    }

    public void toggleRegion() {
        config().showRegion = !config().showRegion;
        tagger.configChanged();
    }

    public void toggleBadge() {
        config().showBadge = !config().showBadge;
        tagger.configChanged();
    }

    public void toggleTab() {
        config().showInTab = !config().showInTab;
        tagger.configChanged();
    }

    private void resetMode(TaggerConfig.Slot slot) {
        if (!TaggerConfig.Slot.BEST.equals(slot.mode) && !TaggerConfig.Slot.RANK.equals(slot.mode)) slot.mode = null;
        tagger.loadModes(tagger.list(slot.list));
    }

    /** Best tier, rank, then each gamemode, skipping whatever the other tag already shows. */
    private void cycleMode(TaggerConfig.Slot slot, TaggerConfig.Slot other) {
        List<GameMode> modes = tagger.list(slot.list).modes();
        if (modes.isEmpty()) tagger.loadModes(tagger.list(slot.list));
        List<String> choices = new ArrayList<String>();
        choices.add(TaggerConfig.Slot.BEST);
        choices.add(TaggerConfig.Slot.RANK);
        for (GameMode mode : modes) choices.add(mode.id());

        int index = choices.indexOf(choice(slot));
        for (int step = 1; step <= choices.size(); step++) {
            String candidate = choices.get((index + step) % choices.size());
            if (!duplicates(slot, candidate, other)) {
                slot.mode = candidate;
                break;
            }
        }
        tagger.configChanged();
    }

    private String choice(TaggerConfig.Slot slot) {
        if (TaggerConfig.Slot.BEST.equals(slot.mode) || TaggerConfig.Slot.RANK.equals(slot.mode)) return slot.mode;
        GameMode mode = tagger.selectedMode(slot);
        return mode != null ? mode.id() : TaggerConfig.Slot.BEST;
    }

    private void avoidDuplicate(TaggerConfig.Slot slot, TaggerConfig.Slot other) {
        if (!other.enabled || !duplicates(slot, choice(slot), other)) return;
        cycleMode(slot, other);
    }

    private boolean duplicates(TaggerConfig.Slot slot, String candidate, TaggerConfig.Slot other) {
        return other.enabled && slot.list.equals(other.list) && candidate.equals(choice(other));
    }

    private static <T> T next(T[] values, T current) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) return values[(i + 1) % values.length];
        }
        return values[0];
    }

    private static TierList next(List<TierList> values, TierList current) {
        int index = values.indexOf(current);
        return values.get((index + 1) % values.size());
    }
}
