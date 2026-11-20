package com.tiertests.tiertagger.core.model;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The same gamemode under the different names tier lists use (neth_pot, nethop, NETH, netherite-pot...).
 * Drives icons, fallback colors and cross-list matching.
 */
public enum ModeKind {
    SWORD('', "mctiers/sword", '⚔', 0xA4FDF0, "sword"),
    AXE('', "mctiers/axe", '⚔', 0x55FF55, "axe"),
    MACE('', "mctiers/mace", '⚒', 0xAAAAAA, "mace"),
    UHC('', "mctiers/uhc", '❤', 0xFF5555, "uhc"),
    POT('', "mctiers/pot", '❤', 0xFF5555, "pot"),
    NETH_POT('', "mctiers/nethop", '☠', 0x7D4A40, "nethpot", "nethop", "netherpot", "neth", "netheritepot"),
    SMP('', "mctiers/smp", '✦', 0xECCB45, "smp"),
    CRYSTAL('', "mctiers/vanilla", '✦', 0xFF55FF, "vanilla", "crystal", "cpvp"),
    DIA_SMP('', "subtiers/dia_smp", '❖', 0x8C668B, "diasmp", "diamondsmp", "diamond"),
    DIA_CRYSTAL('', "subtiers/dia_crystal", '❖', 0x55FFFF, "diacrystal", "diamondcrystal"),
    OG_VANILLA('', "subtiers/og_vanilla", '✦', 0xFFAA00, "ogvanilla"),
    BED('', "subtiers/bed", '⭐', 0xFF5555, "bed"),
    BOW('', "subtiers/bow", '➳', 0xAA6633, "bow"),
    CREEPER('', "subtiers/creeper", '☢', 0x55FF55, "creeper"),
    DEBUFF('', "subtiers/debuff", '☣', 0xAA55AA, "debuff"),
    ELYTRA('', "subtiers/elytra", '✈', 0x8D8DB1, "elytra"),
    MANHUNT('', "subtiers/manhunt", '⌖', 0xFF5555, "manhunt"),
    MINECART('', "subtiers/minecart", '⛏', 0xAAAAAA, "minecart", "cart"),
    SPEED('', "subtiers/speed", '⚡', 0x55FFFF, "speed"),
    TRIDENT('', "subtiers/trident", '⚓', 0x55AA55, "trident"),
    BEDFIGHT('', "tiertests/bedfight", '⭐', 0xFF5555, "bedfight"),
    BEDWARS('', "tiertests/bedwars", '⭐', 0xFF5555, "bedwars"),
    BOXING('', "tiertests/boxing", '⚔', 0x55FFFF, "boxing"),
    BRIDGE('', "tiertests/bridge", '≡', 0x5555FF, "bridge"),
    BUILD_UHC('', "tiertests/builduhc", '❤', 0xFFAA00, "builduhc"),
    EMERALD_RUSH('', "tiertests/emeraldrush", '✦', 0x55FF55, "emeraldrush"),
    FIREBALL('', "tiertests/fireballfight", '☀', 0xFFAA00, "fireballfight", "fireball"),
    NODEBUFF('', "tiertests/nodebuff", '❤', 0xFF5555, "nodebuff"),
    SUMO('', "tiertests/sumo", '●', 0xC8A86B, "sumo"),
    SPEAR((char) 0, null, '↑', 0x7FB3D5, "spear"),
    SHIELD((char) 0, null, '◆', 0xC8A86B, "shield"),
    EARLY_GAME((char) 0, null, '☘', 0x8BC34A, "earlygame"),
    LATE_GAME((char) 0, null, '☠', 0x9575CD, "lategame"),
    END_GAME((char) 0, null, '✦', 0xD580FF, "endgame"),
    OTHER((char) 0, null, '✦', 0xFFFFFF);

    private static final Map<String, ModeKind> BY_ALIAS = new HashMap<String, ModeKind>();

    static {
        for (ModeKind kind : values()) {
            for (String alias : kind.aliases) BY_ALIAS.put(alias, kind);
        }
    }

    private final char glyph;
    private final String texture;
    private final char symbol;
    private final int color;
    private final String[] aliases;

    ModeKind(char glyph, String texture, char symbol, int color, String... aliases) {
        this.glyph = glyph;
        this.texture = texture;
        this.symbol = symbol;
        this.color = color;
        this.aliases = aliases;
    }

    /** Character in the mod's icon font, or 0 when this gamemode has no pixel icon. */
    public char glyph() {
        return glyph;
    }

    /**
     * The pixel icon as a texture path under {@code assets/tiertag/textures/} without extension, or null.
     * Used where an icon font is not available.
     */
    public String texture() {
        return texture;
    }

    /** Plain unicode symbol, used by the classic icon style. */
    public char symbol() {
        return symbol;
    }

    public int color() {
        return color;
    }

    /**
     * 1.8.9 draws these characters from the mod's HD unicode font pages; this marks each one as a
     * full-width glyph (columns 0 to 15) in that version's glyph size table.
     */
    public static void markGlyphWidths(byte[] glyphSizes) {
        for (ModeKind kind : values()) {
            if (kind.glyph != 0) glyphSizes[kind.glyph] = 0x0F;
        }
    }

    public static ModeKind of(String id) {
        if (id == null) return OTHER;
        String key = id.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "");
        ModeKind kind = BY_ALIAS.get(key);
        return kind != null ? kind : OTHER;
    }
}
