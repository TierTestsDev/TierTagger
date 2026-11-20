# Changelog

## 2.1.0

### New

- **Ornithe** support: Tier Tagger now runs on Fabric for Minecraft 1.8.9 through [Ornithe](https://ornithemc.net), and works in [OneClient](https://polyfrost.org/projects/oneclient) alongside its nametag and performance mods.
- Gamemode icons on 1.8.9, in nametags, the tab list, the lookup popup and player search.
- Real icons for the Tier Tests legacy gamemodes (Bedfight, Bedwars, Boxing, Bridge, Build UHC, Emerald Rush, Fireball Fight, No Debuff and Sumo) on every version.

### Fixed

- Gamemodes without an icon showed broken symbols on 1.8.9.
- Dark gamemode colors, like No Debuff's, were hard to read in the popup and player search.

## 2.0.0

A rewrite of the whole mod. Your settings carry over automatically.

### New

- **MCPVP** tier list, including its middle tiers (HT, MT, LT).
- **Tier Tests Legacy** list on every version, and modern lists on Forge 1.8.9.
- Support for **Minecraft 26.3**.
- Forge 1.8.9 now has the same features as Fabric: two tags, every display mode, the lookup popup, player search and all tier lists. It uses Forge's own events, so it works with other 1.8.9 mods and clients.
- Settings button in **Mod Menu** (Fabric) and Forge's mod list, and a `/tiertag` command on 1.8.9.
- **Retired** tiers are marked with an R, and peak tiers are shown for every list that reports them.
- Option to hide the Tier Tests badge.

### Fixed

- MCTiers stopped working after it moved to mctiers.net.
- The secondary tag often ignored its selected gamemode when the primary list had a gamemode with the same name.
- Lists other than the primary one could lose tiers whose gamemode was missing from the primary list.
- Minecraft 1.21 and 1.21.1 crashed when loading entity renderers.
- Switching tier lists while tiers were loading could drop a player's tiers for two minutes.
- Tags flickered off every five minutes while they were refreshed.
- The Discord button always opened the fallback invite instead of the configured one.
- The update check made a broken request first on every launch.
- MCPVP search results could match the wrong player.
- "Best tier" mode could show the selected gamemode instead of the best one.

### Faster

- Players are only fetched when they are drawn, instead of scanning the whole world every two seconds.
- Peak tiers are only fetched for lookups and are cached, instead of one request per gamemode for every player.
- Cached tiers are shown while they refresh, failed requests back off, and requests are spread out so busy servers no longer hit API rate limits.
- Smaller download: the YAML library is gone and settings are stored as JSON.
