package com.tiertests.tiertagger.core.text;

import com.tiertests.tiertagger.core.config.DisplayMode;
import com.tiertests.tiertagger.core.config.IconStyle;
import com.tiertests.tiertagger.core.config.TaggerConfig;
import com.tiertests.tiertagger.core.model.Badge;
import com.tiertests.tiertagger.core.model.Colors;
import com.tiertests.tiertagger.core.model.GameMode;
import com.tiertests.tiertagger.core.model.PlayerProfile;
import com.tiertests.tiertagger.core.model.Ranking;
import com.tiertests.tiertagger.core.model.Region;
import com.tiertests.tiertagger.core.model.Tier;
import com.tiertests.tiertagger.core.model.TierFormat;

import java.util.List;
import java.util.Map;

/** Turns profiles into styled tag text. Stateless; caching lives in the caller. */
public final class TagBuilder {
    public static final int SEPARATOR_COLOR = Colors.GRAY;
    private static final int RANK_COLOR = 0xFFFF55;
    private static final int REGION_COLOR = 0xE0E0E0;

    private final boolean iconFont;

    public TagBuilder(boolean iconFont) {
        this.iconFont = iconFont;
    }

    /** Result of picking what one tag slot shows. */
    public static final class Pick {
        final GameMode mode;
        final Ranking ranking;
        final int rank;

        Pick(GameMode mode, Ranking ranking, int rank) {
            this.mode = mode;
            this.ranking = ranking;
            this.rank = rank;
        }

        public GameMode mode() {
            return mode;
        }

        public Ranking ranking() {
            return ranking;
        }
    }

    /**
     * Picks the ranking for a slot.
     * @param others profiles of the same player on other lists, used by the "any list" modes
     */
    public Pick pick(PlayerProfile profile, GameMode selected, DisplayMode mode, List<PlayerProfile> others) {
        if (mode == DisplayMode.RANK) {
            return profile != null && profile.rank() > 0 ? new Pick(null, null, profile.rank()) : null;
        }
        if (mode == DisplayMode.BEST) return bestOf(profile, null);
        Ranking own = profile == null ? null : profile.ranking(selected);
        if (own != null) return new Pick(selected, own, 0);
        if (mode == DisplayMode.SELECTED) return null;

        if (mode.usesOtherLists() && selected != null && others != null) {
            Pick best = null;
            for (PlayerProfile other : others) {
                Map.Entry<GameMode, Ranking> match = other.rankingOfKind(selected);
                if (match != null && (best == null || match.getValue().compareTo(best.ranking) < 0)) {
                    best = new Pick(match.getKey(), match.getValue(), 0);
                }
            }
            if (best != null) return best;
        }

        Pick best = bestOf(profile, null);
        if (mode == DisplayMode.BEST_ANY_LIST && others != null) {
            for (PlayerProfile other : others) best = bestOf(other, best);
        }
        return best;
    }

    private static Pick bestOf(PlayerProfile profile, Pick current) {
        Map.Entry<GameMode, Ranking> best = profile == null ? null : profile.best();
        if (best == null) return current;
        if (current == null || best.getValue().compareTo(current.ranking) < 0) {
            return new Pick(best.getKey(), best.getValue(), 0);
        }
        return current;
    }

    /** One slot as text: icon and tier, or "#rank". */
    public Text slot(Pick pick, TaggerConfig config) {
        Text.Builder text = Text.builder();
        if (pick == null) return text.build();
        if (pick.ranking == null) {
            return text.append("#", Colors.GRAY).append(String.valueOf(pick.rank), RANK_COLOR).build();
        }
        appendIcon(text, pick.mode, config.icons);
        appendTier(text, pick.ranking, config.tierFormat);
        return text.build();
    }

    public void appendIcon(Text.Builder text, GameMode mode, IconStyle style) {
        if (mode == null || style == IconStyle.NONE) return;
        char glyph = mode.kind().glyph();
        if (style == IconStyle.PIXEL && iconFont && glyph != 0) {
            text.icon(glyph);
        } else {
            text.append(String.valueOf(mode.kind().symbol()), mode.color());
        }
        text.append(" ", -1);
    }

    public void appendTier(Text.Builder text, Ranking ranking, TierFormat format) {
        Tier tier = ranking.tier();
        if (ranking.retired()) text.append("R", Colors.GRAY);
        text.append(tier.format(format), tier.color());
    }

    public void appendDetails(Text.Builder text, PlayerProfile regionSource, Badge badge, TaggerConfig config) {
        Region region = regionSource == null ? null : regionSource.region();
        if (config.showRegion && region != null) {
            text.append(" [", Colors.GRAY).append(region.code(), REGION_COLOR).append("]", Colors.GRAY);
        }
        if (config.showBadge && badge != null) {
            text.append(" ", -1).append(badge.symbol(), badge.color());
        }
    }

    /** Places the slots around the name according to the layout. Either slot may be empty. */
    public Tag compose(Text primary, Text secondary, TaggerConfig config) {
        boolean hasPrimary = primary != null && !primary.isEmpty();
        boolean hasSecondary = secondary != null && !secondary.isEmpty();
        if (!hasPrimary && !hasSecondary) return null;
        switch (config.layout) {
            case SUFFIX:
                return new Tag(null, Text.builder().append(separator()).append(join(primary, secondary)).build());
            case SPLIT:
                if (!hasPrimary) return new Tag(Text.builder().append(secondary).append(separator()).build(), null);
                Text before = Text.builder().append(primary).append(separator()).build();
                Text after = hasSecondary ? Text.builder().append(separator()).append(secondary).build() : null;
                return new Tag(before, after);
            default:
                return new Tag(Text.builder().append(join(primary, secondary)).append(separator()).build(), null);
        }
    }

    private static Text join(Text a, Text b) {
        if (a == null || a.isEmpty()) return b;
        if (b == null || b.isEmpty()) return a;
        return Text.builder().append(a).append(separator()).append(b).build();
    }

    public static Text separator() {
        return Text.builder().append(" | ", SEPARATOR_COLOR).build();
    }
}
