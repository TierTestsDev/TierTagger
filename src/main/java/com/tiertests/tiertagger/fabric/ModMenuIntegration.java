package com.tiertests.tiertagger.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.tiertests.tiertagger.gui.ConfigScreen;

/** Adds the settings screen to Mod Menu's "Configure" button. Only loaded when Mod Menu is installed. */
public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<ConfigScreen> getModConfigScreenFactory() {
        return ConfigScreen::new;
    }
}
