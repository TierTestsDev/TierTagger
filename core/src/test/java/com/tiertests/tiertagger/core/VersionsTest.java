package com.tiertests.tiertagger.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionsTest {
    @Test
    void comparesNumerically() {
        assertTrue(Versions.compare("1.10.0", "1.9.9") > 0);
        assertTrue(Versions.compare("2.0.0", "1.3.2") > 0);
        assertEquals(0, Versions.compare("2.0", "2.0.0"));
        assertEquals(0, Versions.compare("2.0.0+26.2", "2.0.0"));
        assertTrue(Versions.compare("1.3.1", "1.3.2") < 0);
    }

    @Test
    void trustsOnlyDiscordInvites() {
        assertTrue(TierTagger.isTrustedInvite("https://discord.gg/tests"));
        assertFalse(TierTagger.isTrustedInvite("http://discord.gg/tests"));
        assertFalse(TierTagger.isTrustedInvite("https://discord.gg.evil.com/x"));
        assertFalse(TierTagger.isTrustedInvite(null));
    }
}
