package com.tiertests.tiertagger.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TierTest {
    @Test
    void parsesCodesAndLetters() {
        assertEquals(Tier.HT1, Tier.parse("ht1"));
        assertEquals(Tier.MT3, Tier.parse("MT3"));
        assertEquals(Tier.HT1, Tier.parse("S"));
        assertEquals(Tier.LT1, Tier.parse("A_PLUS"));
        assertEquals(Tier.HT2, Tier.parse("A-"));
        assertEquals(Tier.LT5, Tier.parse("F"));
        assertNull(Tier.parse("???"));
        assertNull(Tier.parse("NONE"));
        assertNull(Tier.parse(""));
    }

    @Test
    void numericPositionZeroIsHigh() {
        assertEquals(Tier.HT3, Tier.of(3, 0));
        assertEquals(Tier.LT3, Tier.of(3, 1));
        assertNull(Tier.of(0, 0));
        assertNull(Tier.of(6, 1));
    }

    @Test
    void ordersBestFirst() {
        assertTrue(Tier.HT1.isBetterThan(Tier.MT1));
        assertTrue(Tier.MT2.isBetterThan(Tier.LT2));
        assertTrue(Tier.LT2.isBetterThan(Tier.HT3));
    }

    @Test
    void lettersFallBackToCodes() {
        assertEquals("C+", Tier.LT3.format(TierFormat.LETTER));
        assertEquals("MT3", Tier.MT3.format(TierFormat.LETTER));
        assertEquals("LT3", Tier.LT3.format(TierFormat.CODE));
    }
}
