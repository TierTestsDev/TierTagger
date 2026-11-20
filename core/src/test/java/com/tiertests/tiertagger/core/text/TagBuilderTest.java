package com.tiertests.tiertagger.core.text;

import com.tiertests.tiertagger.core.config.DisplayMode;
import com.tiertests.tiertagger.core.config.IconStyle;
import com.tiertests.tiertagger.core.config.Layout;
import com.tiertests.tiertagger.core.config.TaggerConfig;
import com.tiertests.tiertagger.core.model.GameMode;
import com.tiertests.tiertagger.core.model.PlayerProfile;
import com.tiertests.tiertagger.core.model.Ranking;
import com.tiertests.tiertagger.core.model.Tier;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TagBuilderTest {
    private static final GameMode SWORD = new GameMode("sword", "Sword");
    private static final GameMode AXE = new GameMode("axe", "Axe");
    private static final GameMode NETH = new GameMode("nethop", "NethPot");
    private final TagBuilder builder = new TagBuilder(false);

    private static PlayerProfile profile(GameMode mode, Tier tier) {
        return PlayerProfile.builder(UUID.randomUUID()).ranking(mode, new Ranking(tier, null, false)).rank(42).build();
    }

    @Test
    void picksSelectedThenBest() {
        PlayerProfile profile = PlayerProfile.builder(UUID.randomUUID())
                .ranking(SWORD, new Ranking(Tier.LT3, null, false))
                .ranking(AXE, new Ranking(Tier.HT2, null, false)).build();

        assertEquals(Tier.LT3, builder.pick(profile, SWORD, DisplayMode.SELECTED_OR_BEST, null).ranking().tier());
        assertEquals(Tier.HT2, builder.pick(profile, SWORD, DisplayMode.BEST, null).ranking().tier());
        assertNull(builder.pick(profile, NETH, DisplayMode.SELECTED, null));
        assertEquals(Tier.HT2, builder.pick(profile, NETH, DisplayMode.SELECTED_OR_BEST, null).ranking().tier());
    }

    @Test
    void anyListMatchesSameGamemodeOnOtherLists() {
        PlayerProfile own = profile(SWORD, Tier.LT4);
        PlayerProfile other = profile(new GameMode("neth_pot", "Netherite Pot"), Tier.HT3);
        TagBuilder.Pick pick = builder.pick(own, NETH, DisplayMode.SELECTED_ANY_LIST, Collections.singletonList(other));
        assertEquals(Tier.HT3, pick.ranking().tier());
        assertEquals("neth_pot", pick.mode().id());
    }

    @Test
    void composesLayouts() {
        TaggerConfig config = new TaggerConfig();
        config.icons = IconStyle.NONE;
        Text a = builder.slot(builder.pick(profile(SWORD, Tier.HT1), SWORD, DisplayMode.SELECTED, null), config);
        Text b = builder.slot(builder.pick(profile(AXE, Tier.LT2), AXE, DisplayMode.SELECTED, null), config);

        config.layout = Layout.PREFIX;
        assertEquals("HT1 | LT2 | ", builder.compose(a, b, config).prefix().plain());
        config.layout = Layout.SUFFIX;
        assertEquals(" | HT1 | LT2", builder.compose(a, b, config).suffix().plain());
        config.layout = Layout.SPLIT;
        Tag split = builder.compose(a, b, config);
        assertEquals("HT1 | ", split.prefix().plain());
        assertEquals(" | LT2", split.suffix().plain());
        assertNull(builder.compose(Text.EMPTY, Text.EMPTY, config));
    }

    @Test
    void showsRankAndFallsBackToSymbols() {
        TaggerConfig config = new TaggerConfig();
        assertEquals("#42", builder.slot(builder.pick(profile(SWORD, Tier.HT1), SWORD, DisplayMode.RANK, null), config).plain());
        config.icons = IconStyle.PIXEL;
        assertEquals("⚔ HT1", builder.slot(builder.pick(profile(SWORD, Tier.HT1), SWORD, DisplayMode.BEST, null), config).plain());
    }
}
