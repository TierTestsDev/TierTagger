package com.tiertests.tiertagger.core.config;

import com.tiertests.tiertagger.core.model.TierFormat;

/** Everything the player can change. Serialized as JSON; field names are the file format. */
public final class TaggerConfig {
    public Slot primary = new Slot(true, "tiertests");
    public Slot secondary = new Slot(false, "mctiers");
    public DisplayMode displayMode = DisplayMode.SELECTED_OR_BEST;
    public Layout layout = Layout.PREFIX;
    public IconStyle icons = IconStyle.PIXEL;
    public TierFormat tierFormat = TierFormat.CODE;
    public boolean showRegion = true;
    public boolean showBadge = true;
    public boolean showInTab = true;

    /**
     * Which tier list and gamemode a tag shows. {@code mode} is the list's gamemode id, or {@link #BEST}
     * or {@link #RANK}. A gamemode the player is not ranked in falls back to their best tier.
     */
    public static final class Slot {
        public static final String BEST = "*best";
        public static final String RANK = "*rank";

        public boolean enabled;
        public String list;
        public String mode;

        public Slot() {
        }

        public Slot(boolean enabled, String list) {
            this.enabled = enabled;
            this.list = list;
        }
    }

    /** Replaces missing or unknown values (from hand edits or older files) with defaults. */
    void sanitize(String defaultList) {
        if (primary == null) primary = new Slot(true, defaultList);
        if (secondary == null) secondary = new Slot(false, "mctiers");
        primary.enabled = true;
        if (primary.list == null) primary.list = defaultList;
        if (secondary.list == null) secondary.list = "mctiers";
        if (displayMode == null) displayMode = DisplayMode.SELECTED_OR_BEST;
        if (displayMode == DisplayMode.BEST || displayMode == DisplayMode.BEST_ANY_LIST) primary.mode = Slot.BEST;
        else if (displayMode == DisplayMode.RANK) primary.mode = Slot.RANK;
        if (displayMode != DisplayMode.SELECTED_ANY_LIST) displayMode = DisplayMode.SELECTED_OR_BEST;
        if (layout == null) layout = Layout.PREFIX;
        if (icons == null) icons = IconStyle.PIXEL;
        if (tierFormat == null) tierFormat = TierFormat.CODE;
        if (layout == Layout.SPLIT && !secondary.enabled) layout = Layout.PREFIX;
    }
}
