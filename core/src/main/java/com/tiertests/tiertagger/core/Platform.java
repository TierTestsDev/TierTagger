package com.tiertests.tiertagger.core;

import java.nio.file.Path;

/** What the core needs to know about the game and loader it runs in. */
public interface Platform {
    String modVersion();

    String minecraftVersion();

    /** Modrinth loader name, e.g. "fabric" or "forge". */
    String loader();

    Path configDir();

    /** True on 1.8-combat versions, which default to the Tier Tests legacy list. */
    boolean legacyCombat();

    /** Whether the {@code tiertag:icons} font is available for pixel gamemode icons. */
    boolean supportsIconFont();
}
