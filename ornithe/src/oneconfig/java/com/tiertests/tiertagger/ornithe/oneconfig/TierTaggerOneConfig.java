package com.tiertests.tiertagger.ornithe.oneconfig;

import com.tiertests.tiertagger.core.TierTagger;
import com.tiertests.tiertagger.core.config.IconStyle;
import com.tiertests.tiertagger.core.config.Layout;
import com.tiertests.tiertagger.core.config.TaggerConfig;
import com.tiertests.tiertagger.ornithe.TierTaggerOrnithe;
import com.tiertests.tiertagger.ornithe.gui.ConfigScreen;
import com.tiertests.tiertagger.ornithe.gui.SearchScreen;
import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.config.v1.Property;
import org.polyfrost.oneconfig.api.config.v1.Tree;
import org.polyfrost.oneconfig.api.config.v1.annotations.Button;
import org.polyfrost.oneconfig.api.config.v1.annotations.Dropdown;
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch;

/**
 * Tier Tagger's page in the OneConfig menu. Tier Tagger's own settings stay the source of truth: changes
 * here are applied to them, and changes made elsewhere are copied back in. Tier lists and gamemodes are
 * only known at runtime, so they open the full settings screen instead.
 */
public final class TierTaggerOneConfig extends Config {
    private static final Layout[] LAYOUTS = {Layout.PREFIX, Layout.SUFFIX, Layout.SPLIT};
    private static TierTaggerOneConfig instance;

    @Button(title = "Tier List and Gamemodes", text = "Open", description = "Pick the tier lists and gamemodes your tags show.")
    public void openSettings() {
        TierTaggerOrnithe.openNextTick(new ConfigScreen(null));
    }

    @Button(title = "Player Search", text = "Search", description = "Look up any player on every tier list.")
    public void openSearch() {
        TierTaggerOrnithe.openNextTick(new SearchScreen(null, null));
    }

    @Dropdown(title = "Position", options = {"Before Name", "After Name", "Both Sides"},
            description = "Both Sides puts one tag on each side when a second tag is on.")
    public int position;

    @Switch(title = "Icons", description = "Gamemode icons next to tiers.")
    public boolean icons = true;

    @Switch(title = "Region", description = "The player's region, like NA or EU.")
    public boolean region = true;

    @Switch(title = "Badge", description = "The Tier Tests badge next to tiers.")
    public boolean badge = true;

    @Switch(title = "Tab List", description = "Show tiers in the player list too.")
    public boolean tabList = true;

    private TierTaggerOneConfig() {
        super("tiertag-oneconfig.json", "/assets/tiertag/icon.png", "Tier Tagger", Category.HYPIXEL);
    }

    /** Adds the page to OneConfig and returns the sync to run every tick. */
    public static Runnable register() {
        instance = new TierTaggerOneConfig();
        return TierTaggerOneConfig::sync;
    }

    /** Copies Tier Tagger's settings into this page; call every tick, it only writes on changes. */
    public static void sync() {
        if (instance != null && instance.tree != null) instance.pull(false);
    }

    @Override
    protected void onInitialized(Tree tree) {
        pull(true);
        TierTagger tagger = TierTagger.get();
        this.<Integer>watch("position", value -> tagger.config().layout = LAYOUTS[Math.max(0, Math.min(2, value))]);
        this.<Boolean>watch("icons", value -> tagger.config().icons = value ? IconStyle.PIXEL : IconStyle.NONE);
        this.<Boolean>watch("region", value -> tagger.config().showRegion = value);
        this.<Boolean>watch("badge", value -> tagger.config().showBadge = value);
        this.<Boolean>watch("tabList", value -> tagger.config().showInTab = value);
    }

    private interface Apply<T> {
        void apply(T value);
    }

    private <T> void watch(String id, final Apply<T> apply) {
        Property<T> property = Property.recast(getProperty(id));
        property.addCallback(value -> {
            apply.apply(value);
            TierTagger.get().configChanged();
            return false;
        });
    }

    private void pull(boolean force) {
        TierTagger tagger = TierTagger.get();
        if (tagger == null) return;
        TaggerConfig config = tagger.config();
        int layout = config.layout == Layout.SUFFIX ? 1 : config.layout == Layout.SPLIT ? 2 : 0;
        boolean iconsOn = config.icons != IconStyle.NONE;
        if (!force && layout == position && iconsOn == icons && config.showRegion == region
                && config.showBadge == badge && config.showInTab == tabList) {
            return;
        }
        set("position", layout);
        set("icons", iconsOn);
        set("region", config.showRegion);
        set("badge", config.showBadge);
        set("tabList", config.showInTab);
    }

    private void set(String id, Object value) {
        Property<?> property = getProperty(id);
        if (property != null) property.setAsSilently(value);
    }
}
