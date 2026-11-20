package com.tiertests.tiertagger.core.source;

import com.tiertests.tiertagger.core.model.GameMode;
import com.tiertests.tiertagger.core.model.PlayerProfile;
import com.tiertests.tiertagger.core.model.Tier;
import com.tiertests.tiertagger.core.net.Http;
import com.tiertests.tiertagger.core.net.Json;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParsingTest {
    private static final UUID PLAYER = UUID.fromString("1246a0ec-662b-45de-bb0b-d68cb1cce800");
    private final Http http = new Http("test");

    @Test
    void parsesV2Profiles() throws Exception {
        String body = "{\"uuid\":\"1246a0ec662b45debb0bd68cb1cce800\",\"name\":\"JokingDK\",\"region\":\"EU\",\"points\":102,"
                + "\"overall\":53,\"rankings\":{\"sword\":{\"tier\":3,\"pos\":1,\"peak_tier\":3,\"peak_pos\":0,\"retired\":false},"
                + "\"smp\":{\"tier\":2,\"pos\":0,\"peak_tier\":2,\"peak_pos\":0,\"retired\":true},"
                + "\"bogus\":{\"tier\":0,\"pos\":0}}}";
        PlayerProfile profile = V2TierList.mcTiers(http).parseProfile(PLAYER, Json.parseObject(body));

        assertEquals("JokingDK", profile.name());
        assertEquals("EU", profile.region().code());
        assertEquals(53, profile.rank());
        assertEquals(2, profile.rankings().size());
        assertEquals(Tier.HT2, profile.best().getValue().tier());
        assertTrue(profile.best().getValue().retired());
        assertTrue(profile.ranking(new GameMode("sword", null)).hasHigherPeak());
    }

    @Test
    void parsesMcpvpPlayers() throws Exception {
        String body = "{\"uuid\":\"1246a0ec662b45debb0bd68cb1cce800\",\"name\":\"JokingDK\",\"rank\":1,\"points\":287.5,"
                + "\"region\":\"EU\",\"kitRanks\":{\"mace\":\"???\",\"smp\":\"MT1\",\"early-game\":\"HT2\"},"
                + "\"kitPeakRanks\":{\"smp\":\"MT1\",\"early-game\":\"HT1\"},\"kitRetired\":{\"smp\":false,\"early-game\":true}}";
        PlayerProfile profile = new McpvpList(http).parsePlayer(PLAYER, Json.parseObject(body));

        assertEquals(2, profile.rankings().size());
        assertEquals(Tier.MT1, profile.best().getValue().tier());
        assertEquals(287.5, profile.points());
        assertFalse(profile.best().getValue().retired());
        assertTrue(profile.ranking(new GameMode("early-game", null)).hasHigherPeak());
    }
}
