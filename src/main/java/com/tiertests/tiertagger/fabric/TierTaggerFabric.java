package com.tiertests.tiertagger.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import com.tiertests.tiertagger.TierTaggerClient;
import com.tiertests.tiertagger.core.Platform;
import com.tiertests.tiertagger.gui.Gfx;
import com.tiertests.tiertagger.gui.LookupHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
//?} else
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;*/
//? if >=1.21.6 {
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
//?} else
/*import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;*/

import java.nio.file.Path;

public final class TierTaggerFabric implements ClientModInitializer {
    //? if >=1.21.11 {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(TierTaggerClient.id("general"));
    //?} else if >=1.21.9 {
    /*private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(TierTaggerClient.id("general"));
    *///?} else
    /*private static final String CATEGORY = "key.category.tiertag.general";*/

    @Override
    public void onInitializeClient() {
        KeyMapping config = register("key.tiertag.openconfig", InputConstants.KEY_N);
        KeyMapping lookup = register("key.tiertag.lookup", InputConstants.KEY_U);
        KeyMapping lastHit = register("key.tiertag.lookup_last_hit", InputConstants.KEY_C);
        TierTaggerClient.init(new FabricPlatform(), config, lookup, lastHit);

        ClientTickEvents.END_CLIENT_TICK.register(TierTaggerClient::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> TierTaggerClient.onDisconnect());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> TierTaggerClient.shutdown());
        //? if >=1.21.6 {
        HudElementRegistry.addLast(TierTaggerClient.id("lookup"), (graphics, tracker) -> LookupHud.render(new Gfx(graphics)));
        //?} else
        /*HudRenderCallback.EVENT.register((graphics, tracker) -> LookupHud.render(new Gfx(graphics)));*/
    }

    private static KeyMapping register(String name, int key) {
        KeyMapping mapping = new KeyMapping(name, key, CATEGORY);
        //? if >=26.1 {
        return KeyMappingHelper.registerKeyMapping(mapping);
        //?} else
        /*return KeyBindingHelper.registerKeyBinding(mapping);*/
    }

    private static final class FabricPlatform implements Platform {
        @Override
        public String modVersion() {
            return version(TierTaggerClient.MOD_ID);
        }

        @Override
        public String minecraftVersion() {
            return version("minecraft");
        }

        @Override
        public String loader() {
            return "fabric";
        }

        @Override
        public Path configDir() {
            return FabricLoader.getInstance().getConfigDir();
        }

        @Override
        public boolean legacyCombat() {
            return false;
        }

        @Override
        public boolean supportsIconFont() {
            return true;
        }

        private static String version(String modId) {
            return FabricLoader.getInstance().getModContainer(modId)
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("unknown");
        }
    }
}
