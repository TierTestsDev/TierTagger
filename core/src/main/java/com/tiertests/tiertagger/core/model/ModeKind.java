package com.tiertests.tiertagger.core.model;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The same gamemode under the different names tier lists use (neth_pot, nethop, NETH, netherite-pot...).
 * Drives icons, fallback colors and cross-list matching.
 */
public enum ModeKind {
    SWORD('', '⚔', 0xA4FDF0, "sword"),
    AXE('', '⚔', 0x55FF55, "axe"),
    MACE('', '⚒', 0xAAAAAA, "mace"),
    UHC('', '❤', 0xFF5555, "uhc"),
    POT('', '❤', 0xFF5555, "pot"),
    NETH_POT('', '☠', 0x7D4A40, "nethpot", "nethop", "netherpot", "neth", "netheritepot"),
    SMP('', '✦', 0xECCB45, "smp"),
    CRYSTAL('', '✦', 0xFF55FF, "vanilla", "crystal", "cpvp"),
    DIA_SMP('', '❖', 0x8C668B, "diasmp", "diamondsmp", "diamond"),
    DIA_CRYSTAL('', '❖', 0x55FFFF, "diacrystal", "diamondcrystal"),
    OG_VANILLA('', '✦', 0xFFAA00, "ogvanilla"),
    BED('', '⭐', 0xFF5555, "bed", "bedwars", "bedfight"),
    BOW('', '➳', 0xAA6633, "bow"),
    CREEPER('', '☢', 0x55FF55, "creeper"),
    DEBUFF('', '☣', 0xAA55AA, "debuff"),
    ELYTRA('', '✈', 0x8D8DB1, "elytra"),
    MANHUNT('', '⌖', 0xFF5555, "manhunt"),
    MINECART('', '⛏', 0xAAAAAA, "minecart", "cart"),
    SPEED('', '⚡', 0x55FFFF, "speed"),
    TRIDENT('', '⚓', 0x55AA55, "trident"),
    SPEAR((char) 0, '↑', 0x7FB3D5, "spear"),
    SHIELD((char) 0, '◆', 0xC8A86B, "shield"),
    EARLY_GAME((char) 0, '☘', 0x8BC34A, "earlygame"),
    LATE_GAME((char) 0, '☠', 0x9575CD, "lategame"),
    END_GAME((char) 0, '✦', 0xD580FF, "endgame"),
    NODEBUFF((char) 0, '⚗', 0xFF55FF, "nodebuff"),
    BRIDGE((char) 0, '≡', 0x5555FF, "bridge"),
    SUMO((char) 0, '●', 0xFFAA00, "sumo"),
    BOXING((char) 0, '✊', 0xFF5555, "boxing"),
    FIREBALL((char) 0, '☄', 0xFFAA00, "fireballfight", "fireball"),
    OTHER((char) 0, '✦', 0xFFFFFF);

    private static final Map<String, ModeKind> BY_ALIAS = new HashMap<String, ModeKind>();

    static {
        for (ModeKind kind : values()) {
            for (String alias : kind.aliases) BY_ALIAS.put(alias, kind);
        }
    }

    private final char glyph;
    private final char symbol;
    private final int color;
    private final String[] aliases;

    ModeKind(char glyph, char symbol, int color, String... aliases) {
        this.glyph = glyph;
        this.symbol = symbol;
        this.color = color;
        this.aliases = aliases;
    }

    /** Character in the mod's icon font, or 0 when this gamemode has no pixel icon. */
    public char glyph() {
        return glyph;
    }

    /** Plain unicode symbol that every Minecraft font can draw. */
    public char symbol() {
        return symbol;
    }

    public int color() {
        return color;
    }

    public static ModeKind of(String id) {
        if (id == null) return OTHER;
        String key = id.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "");
        ModeKind kind = BY_ALIAS.get(key);
        return kind != null ? kind : OTHER;
    }
}
