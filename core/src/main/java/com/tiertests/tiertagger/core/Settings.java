package com.tiertests.tiertagger.core;

import com.tiertests.tiertagger.core.config.DisplayMode;
import com.tiertests.tiertagger.core.config.IconStyle;
import com.tiertests.tiertagger.core.config.Layout;
import com.tiertests.tiertagger.core.config.TaggerConfig;
import com.tiertests.tiertagger.core.model.GameMode;
import com.tiertests.tiertagger.core.model.TierFormat;
import com.tiertests.tiertagger.core.source.TierList;

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

    public String secondaryListLabel() {
        return tagger.secondaryList().displayName();
    }

    public String primaryModeLabel() {
        return modeLabel(config().primary);
    }

    public String secondaryModeLabel() {
        return modeLabel(config().secondary);
    }

    private String modeLabel(TaggerConfig.Slot slot) {
        GameMode mode = tagger.selectedMode(slot);
        if (mode != null) return mode.title();
        TierList list = tagger.list(slot.list);
        tagger.loadModes(list);
        return list.modesLoaded() ? "None" : "Loading...";
    }

    public void cyclePrimaryList() {
        TaggerConfig.Slot slot = config().primary;
        slot.list = next(tagger.lists(), tagger.list(slot.list)).id();
        slot.mode = null;
        tagger.loadModes(tagger.list(slot.list));
        avoidDuplicate(slot, config().secondary);
        tagger.configChanged();
    }

    public void cyclePrimaryMode() {
        cycleMode(config().primary, config().secondary);
    }

    public void toggleSecondary() {
        TaggerConfig.Slot secondary = config().secondary;
        secondary.enabled = !secondary.enabled;
        if (secondary.enabled) {
            tagger.loadModes(tagger.list(secondary.list));
            if (config().layout == Layout.PREFIX) config().layout = Layout.SPLIT;
            avoidDuplicate(secondary, config().primary);
        } else if (config().layout == Layout.SPLIT) {
            config().layout = Layout.PREFIX;
        }
        tagger.configChanged();
    }

    public void cycleSecondaryList() {
        TaggerConfig.Slot slot = config().secondary;
        slot.list = next(tagger.lists(), tagger.list(slot.list)).id();
        slot.mode = null;
        tagger.loadModes(tagger.list(slot.list));
        avoidDuplicate(slot, config().primary);
        tagger.configChanged();
    }

    public void cycleSecondaryMode() {
        cycleMode(config().secondary, config().primary);
    }

    public void cycleDisplayMode() {
        config().displayMode = next(DisplayMode.values(), config().displayMode);
        tagger.configChanged();
    }

    public void cycleLayout() {
        Layout layout = next(Layout.values(), config().layout);
        if (layout == Layout.SPLIT && !config().secondary.enabled) layout = next(Layout.values(), layout);
        config().layout = layout;
        tagger.configChanged();
    }

    public void cycleIcons() {
        config().icons = next(IconStyle.values(), config().icons);
        tagger.configChanged();
    }

    public void cycleTierFormat() {
        config().tierFormat = next(TierFormat.values(), config().tierFormat);
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

    /** Moves to the next gamemode, skipping the exact list and gamemode the other slot already shows. */
    private void cycleMode(TaggerConfig.Slot slot, TaggerConfig.Slot other) {
        List<GameMode> modes = tagger.list(slot.list).modes();
        if (modes.isEmpty()) {
            tagger.loadModes(tagger.list(slot.list));
            return;
        }
        int index = modes.indexOf(tagger.selectedMode(slot));
        for (int step = 1; step <= modes.size(); step++) {
            GameMode candidate = modes.get((index + step) % modes.size());
            if (!duplicates(slot, candidate, other)) {
                slot.mode = candidate.id();
                break;
            }
        }
        tagger.configChanged();
    }

    private void avoidDuplicate(TaggerConfig.Slot slot, TaggerConfig.Slot other) {
        if (!other.enabled && other != config().primary) return;
        List<GameMode> modes = tagger.list(slot.list).modes();
        if (modes.isEmpty() || !duplicates(slot, tagger.selectedMode(slot), other)) return;
        for (GameMode mode : modes) {
            if (!duplicates(slot, mode, other)) {
                slot.mode = mode.id();
                return;
            }
        }
    }

    private boolean duplicates(TaggerConfig.Slot slot, GameMode mode, TaggerConfig.Slot other) {
        if (mode == null || !other.enabled || !slot.list.equals(other.list)) return false;
        GameMode otherMode = tagger.selectedMode(other);
        return otherMode != null && otherMode.equals(mode);
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
