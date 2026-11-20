# Tier Tagger

Shows players' PvP tiers next to their names in nametags and the tab list.

[![Build](https://github.com/TierTestsDev/TierTagger/actions/workflows/build.yml/badge.svg)](https://github.com/TierTestsDev/TierTagger/actions/workflows/build.yml)
[![Modrinth](https://img.shields.io/modrinth/dt/tiertests?logo=modrinth&label=Modrinth)](https://modrinth.com/mod/tiertests)

Supported tier lists:

- [Tier Tests](https://tiertests.com), both modern and legacy (1.8) gamemodes
- [MCTiers](https://mctiers.net)
- [PvPTiers](https://pvptiers.com)
- [SubTiers](https://subtiers.net)
- [MCPVP](https://www.mcpvp.com/tiers)

## Features

- **Two tags at once.** Show a primary and a secondary tier list or gamemode, before the name, after it, or split around it.
- **Display modes.** Show the selected gamemode, the best tier, the overall rank, or fall back to other lists when a player is not ranked on yours.
- **Lookups.** Press a key to see every tier of the nearest player or the last player you hit, with peak tiers, or search any username across all lists.
- **Fast and light.** Players are only fetched when they are actually drawn. Results are cached, refreshed in the background and shared between features, and every API is rate limited on the client side.

| Default key | Action |
|---|---|
| `N` | Open settings |
| `U` | Look up the nearest player |
| `C` | Look up the last player you hit |

On Forge 1.8.9, `/tiertag` opens the settings and `/tiertag <name>` searches a player. On Fabric, the settings screen is also available from [Mod Menu](https://modrinth.com/mod/modmenu).

## Supported versions

| Loader | Minecraft |
|---|---|
| Fabric | 1.21 to 1.21.11, 26.1, 26.2, 26.3 |
| Forge | 1.8.9 |
| [Ornithe](https://ornithemc.net) (Fabric for old versions) | 1.8.9 |

Downloads are on [Modrinth](https://modrinth.com/mod/tiertests). The Fabric version needs [Fabric API](https://modrinth.com/mod/fabric-api) and the Ornithe version needs [Ornithe Standard Libraries](https://modrinth.com/mod/osl). The Ornithe version also works in [OneClient](https://polyfrost.org/projects/oneclient).

## Building

Fabric (all versions) needs JDK 25:

```sh
./gradlew build
```

Jars end up in `versions/<minecraft>-fabric/build/libs/`. To build one version, run for example `./gradlew :1.21.4-fabric:build`.

Forge 1.8.9 is a separate Gradle build, because its toolchain needs an older Gradle. Run it with JDK 17 or 21:

```sh
cd forge
./gradlew build
```

Ornithe 1.8.9 is a separate Gradle build as well. Run it with JDK 25:

```sh
cd ornithe
./gradlew build
```

## Project layout

```
core/       Plain Java 8 library: tier list APIs, caching, config and tag text. No Minecraft code.
src/        Fabric mod for every modern version, kept in one source tree with Stonecutter.
forge/      Forge 1.8.9 mod.
ornithe/    Ornithe (Fabric) 1.8.9 mod.
gradle/     Shared build logic, such as the 1.8.9 icon font.
```

The core does all the work that does not depend on the game: fetching, parsing, caching, choosing which tier to show and building the styled tag text. Each platform only has to draw that text and wire up its events, so porting Tier Tagger to another client or loader means writing a thin adapter around `core`.

### Version differences

Modern versions share one source tree. Code that differs between Minecraft versions uses [Stonecutter](https://stonecutter.kikugie.dev) comments:

```java
//? if >=1.21.6 {
g.pose().pushMatrix();
//?} else
/*g.pose().pushPose();*/
```

Almost all of these live in `gui/Gfx`, `gui/BaseScreen`, `gui/Skins` and the Fabric entrypoint, so screens and the HUD are written once.

To add a Minecraft version:

1. Add it to `settings.gradle.kts` and give it a section in `stonecutter.properties.toml`.
2. Run `./gradlew :<version>-fabric:build` and fix what no longer compiles behind a version condition.
3. Run `./gradlew :<version>-fabric:runClientGameTest` (1.21.4 and newer) and check the screenshots in `run/screenshots`.

### Adding a tier list

Extend `TierList` in `core/src/main/java/.../core/source`, then register it in `TierTagger`. Lists that use the MCTiers profile format only need a new factory method in `V2TierList`.

## Testing

```sh
./gradlew :core:test
./gradlew :26.3-fabric:runClientGameTest
```

The client game test starts the game, creates a world and saves screenshots of the settings screen, the player search, tag rendering and the lookup popup.

## License

[GNU General Public License v3.0](LICENSE)
