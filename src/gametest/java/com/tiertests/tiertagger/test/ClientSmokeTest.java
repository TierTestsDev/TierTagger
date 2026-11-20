package com.tiertests.tiertagger.test;

import com.mojang.blaze3d.platform.InputConstants;
import com.tiertests.tiertagger.core.TierTagger;
import com.tiertests.tiertagger.gui.BaseScreen;
import com.tiertests.tiertagger.gui.ConfigScreen;
import com.tiertests.tiertagger.gui.Gfx;
import com.tiertests.tiertagger.gui.LookupHud;
import com.tiertests.tiertagger.gui.SearchScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import com.tiertests.tiertagger.render.Components;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Boots a client, joins a world and screenshots every part of the mod (run/screenshots).
 * Run with {@code ./gradlew :<version>-fabric:runClientGameTest}. Needs internet access for the tier list APIs.
 */
public final class ClientSmokeTest implements FabricClientGameTest {
    private static final int TIMEOUT = 20 * 30;
    private static final String[][] SAMPLES_RAW = {
            {"JokingDK", "1246a0ec-662b-45de-bb0b-d68cb1cce800"},
            {"Stxroid", "92578d9a-09a5-4894-817c-3b23ff8587f1"},
    };
    private static final List<String[]> SAMPLES = Arrays.asList(SAMPLES_RAW);

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            context.waitTicks(60);
            context.waitFor(ClientSmokeTest::ownProfileResolved, TIMEOUT);

            context.setScreen(() -> new ConfigScreen(null));
            context.waitTicks(20);
            context.takeScreenshot("tiertag-config");

            context.setScreen(() -> new SearchScreen(null));
            context.waitTicks(5);
            context.getInput().typeChars("JokingDK");
            context.getInput().pressKey(InputConstants.KEY_RETURN);
            context.waitTicks(20 * 8);
            context.takeScreenshot("tiertag-search");

            context.setScreen(() -> null);
            context.runOnClient(minecraft -> TierTagger.get().settings().cycleSecondaryList());
            context.waitFor(minecraft -> SAMPLES.stream().allMatch(ClientSmokeTest::resolved), TIMEOUT);
            context.setScreen(TagPreviewScreen::new);
            context.waitTicks(10);
            context.takeScreenshot("tiertag-tags");

            context.setScreen(() -> null);
            context.runOnClient(minecraft -> LookupHud.lookup(UUID.fromString(SAMPLES_RAW[0][1]), SAMPLES_RAW[0][0]));
            context.waitTicks(20 * 3);
            context.takeScreenshot("tiertag-lookup");
        }
    }

    /** Draws each sample player's name the way nametags show it. */
    private static final class TagPreviewScreen extends BaseScreen {
        TagPreviewScreen() {
            super(Component.literal("Tag preview"), null);
        }

        @Override
        protected void draw(Gfx gfx, int mouseX, int mouseY, float delta) {
            int y = 40;
            for (String[] sample : SAMPLES_RAW) {
                Component name = Component.literal(sample[0]);
                Component tagged = Components.decorate(UUID.fromString(sample[1]), sample[0], name, false);
                gfx.centered(tagged != null ? tagged : name, width / 2, y, 0xFFFFFFFF);
                y += 16;
            }
        }
    }

    private static boolean resolved(String[] sample) {
        TierTagger tagger = TierTagger.get();
        UUID uuid = UUID.fromString(sample[1]);
        tagger.tag(uuid, sample[0], false);
        return tagger.store().entry(tagger.primaryList(), uuid, sample[0]).isResolved()
                && tagger.store().entry(tagger.secondaryList(), uuid, sample[0]).isResolved();
    }

    private static boolean ownProfileResolved(Minecraft minecraft) {
        TierTagger tagger = TierTagger.get();
        return minecraft.player != null && tagger.store()
                .entry(tagger.primaryList(), minecraft.player.getUUID(), minecraft.player.getScoreboardName())
                .isResolved();
    }
}
