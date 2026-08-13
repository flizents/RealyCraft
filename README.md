# RealCraftAntiCheat

RealCraftAntiCheat 1.1.0 is a Paper-only anti-cheat for **Paper 1.21.8** and **Java 21**.

## Build

```bash
mvn clean package
```

The ready artifact is `target/RealCraftAntiCheat-1.1.0.jar`. Copy it to the `plugins` directory of a Paper 1.21.8 server.

## Included

- Combat, movement, player and world checks with per-check violation levels.
- Statistical AntiXray signals: rare ore ratio, mining interval, ore proximity and mining patterns.
- Persistent `PlayerData`, violations, actions and bans in SQLite; MySQL can be enabled in `config.yml`.
- `/check <player>` and `/ac player|violations|alerts|staff|reload|info|gui`.
- `/check <player> silent`, ping/TPS/risk output, interactive staff GUI with VL reset, watch mode, history and compact VL graph.
- Configurable Alert, Kick and Ban thresholds; LiteBans is preferred when installed and AdvancedBan remains compatible with its `ban` command.
- Permission nodes from `plugin.yml`, bypass support, async periodic data saves and low-cost event handlers.
- Optional PacketEvents detection through the soft dependency bridge. The plugin still works on a clean Paper 1.21.8 server using Paper events, so PacketEvents is not bundled or forced.

## Scope

This project deliberately targets only Paper 1.21.8. It does not promise compatibility with Spigot, Folia, older Paper versions, NMS internals or other Minecraft releases. Heuristic checks should be tuned in `config.yml` for the server's movement, latency and gameplay rules before enabling automatic punishments in production.