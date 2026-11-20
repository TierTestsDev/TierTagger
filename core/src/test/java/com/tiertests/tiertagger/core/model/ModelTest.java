package com.tiertests.tiertagger.core.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelTest {
    @Test
    void groupsGamemodesAcrossLists() {
        assertEquals(ModeKind.NETH_POT, ModeKind.of("neth_pot"));
        assertEquals(ModeKind.NETH_POT, ModeKind.of("NETH"));
        assertEquals(ModeKind.NETH_POT, ModeKind.of("netherite-pot"));
        assertEquals(ModeKind.CRYSTAL, ModeKind.of("vanilla"));
        assertEquals(ModeKind.EARLY_GAME, ModeKind.of("early-game"));
        assertEquals(ModeKind.BRIDGE, ModeKind.of("bridge"));
        assertEquals(ModeKind.BEDFIGHT, ModeKind.of("BEDFIGHT"));
        assertEquals("tiertests/nodebuff", ModeKind.of("nodebuff").texture());
        assertEquals(ModeKind.OTHER, ModeKind.of("parkour"));
        assertTrue(new GameMode("neth_pot", null).isSameKind(new GameMode("nethop", null)));
        assertFalse(new GameMode("parkour", null).isSameKind(new GameMode("archery", null)));
    }

    @Test
    void titlesFromIds() {
        assertEquals("Early Game", Strings.titleCase("early-game"));
        assertEquals("UHC", Strings.titleCase("uhc"));
        assertEquals("Neth Pot", Strings.titleCase("neth_pot"));
        assertEquals("Axe", Strings.titleCase("axe"));
    }

    @Test
    void parsesRegions() {
        assertEquals("NA", Region.parse("north america").code());
        assertEquals("AU", Region.parse("Oceania").code());
        assertEquals("EU", Region.parse("EU").code());
        assertNull(Region.parse("ANY"));
        assertNull(Region.parse("??"));
        assertNull(Region.parse(null));
    }

    @Test
    void parsesUuids() {
        UUID uuid = UUID.fromString("1246a0ec-662b-45de-bb0b-d68cb1cce800");
        assertEquals(uuid, Strings.parseUuid("1246a0ec662b45debb0bd68cb1cce800"));
        assertEquals(uuid, Strings.parseUuid(uuid.toString()));
        assertNull(Strings.parseUuid("nope"));
    }

    @Test
    void mapsColors() {
        assertEquals(0xAAAAAA, Colors.byLegacyCode("7"));
        assertEquals(0xAA0000, Colors.byName("dark_red"));
        assertEquals(0xFF0000, Colors.parseHex("#FF0000"));
        assertEquals('c', Colors.nearestLegacyCode(0xFF4444));
        assertEquals(0xFF5555, Colors.readable(0xFF5555));
        assertTrue(Colors.readable(0x0000AA) != 0x0000AA);
    }

    @Test
    void validatesUsernames() {
        assertTrue(Strings.isValidUsername("Jo_42"));
        assertFalse(Strings.isValidUsername("ab"));
        assertFalse(Strings.isValidUsername("bad name"));
    }
}
