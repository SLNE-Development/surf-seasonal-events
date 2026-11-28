# surf-seasonal-events

Seasonal events (Easter, Christmas, Halloween …) for the survival server. Staff mark blocks in the
world; while an event runs, every player sees their own fake blocks there and collects loot packages
by clicking them.

Two modes:

- **DAILY** – each player gets one block per day. Packages are handed out at random; a package can be
  pinned to a fixed day, which then takes precedence. A block can also be pinned to a day: it is then that
  day's hiding spot for everyone and stays until found, so players who start late catch up on every
  earlier day. One package can be the NPC reward, handed out by a surf-npc NPC (see below).
- **COLLECT** – every marked block can be found once per player, optionally limited per day. Each found
  block gives its own package, so the event needs at least one filled package per marker.

## Modules

| Module | Purpose |
|---|---|
| `surf-seasonal-events-api` | Data types and result types shared by everything |
| `surf-seasonal-events-core` | RPC service interfaces and the serializer module |
| `surf-seasonal-events-core-client` | RabbitMQ client and RPC proxies used by the plugin |
| `surf-seasonal-events-microservice` | Owns the database and answers the RPC calls. Run exactly **one** instance |
| `surf-seasonal-events-paper` | Paper plugin: commands, fake block rendering, marker tool, loot GUIs |

## Running locally

Requirements: JDK 25, MariaDB or PostgreSQL, RabbitMQ.

1. Create a database `seasonal_events`. Both MariaDB and PostgreSQL work; pick one with
   `database-type: MARIADB` or `database-type: POSTGRESQL` in `database.yml` (for PostgreSQL also set
   `schema`, default `public`). Loot packages are limited to 60 KB because of MariaDB's `BLOB` size.
2. Start the microservice once from `run/` so it writes `run/config/database.yml` and
   `run/config/rabbitmq.yml`, fill in the credentials and start it again:
   ```sh
   ./gradlew :surf-seasonal-events-microservice:shadowJar
   cd run && java -jar ../surf-seasonal-events-microservice/build/libs/surf-seasonal-events-microservice-1.0.0-all.jar
   ```
   Tables are created on startup.
3. Start the test server: `./gradlew :surf-seasonal-events-paper:runServer`. It needs these plugins in
   `surf-seasonal-events-paper/run/plugins`: surf-api-paper-server, surf-rabbitmq-paper, CommandAPI,
   LuckPerms and packetevents. Enter the RabbitMQ credentials in the plugin's `rabbitmq.yml`.

`run/` is git-ignored; credentials never belong in the repository.

## Deployment

- **Microservice**: `surf-seasonal-events-microservice/Dockerfile` (regenerate with
  `./gradlew :surf-seasonal-events-microservice:generateMicroserviceDockerfile`, do not edit by hand).
  Build context is the repository root. The service reads its config from `/app/config`, so mount
  `database.yml` and `rabbitmq.yml` there as secret files. Never run more than one replica: write locks
  are held in memory.
- **Plugin**: `./gradlew :surf-seasonal-events-paper:build`, then put the jar together with
  surf-rabbitmq-paper, CommandAPI and packetevents on the server.

## Commands

All under `/seasonalevents`. Without a subcommand it shows the player's status for the running event.

| Subcommand | Permission (`surf.seasonal-events.command.seasonal-events.*`) |
|---|---|
| *(none)* – own status | `surf.seasonal-events.command.seasonal-events` (give this to the default group) |
| `list` | `.list` |
| `info <event>` | `.info` |
| `create <name> <daily\|collect> <markerLimit> [dailyLimit]` | `.create` |
| `displayname <event> <MiniMessage>` | `.edit` |
| `edit <event>` – marker tool, left click marks, right click unmarks, sneak + right click pins a day | `.edit` |
| `setend <event> <yyyy-MM-ddTHH:mm\|none>` | `.edit` |
| `loot <event>` – loot package GUI | `.loot` |
| `copyloot <from> <to>` – copies packages, not fixed days | `.loot` |
| `start <event> [end]` | `.start` |
| `stop` | `.stop` |
| `restore <event>` – puts the real blocks back | `.restore` |
| `delete <event>` | `.delete` |
| `npc <event> <npc> [remove]` – links a surf-npc NPC to the event (only with surf-npc) | `.npc` |

Every subcommand also requires the base permission. Times are Europe/Berlin.

## Advent calendar with an NPC (e.g. Christmas)

1. `create christmas-2026 daily 23`, then `edit christmas-2026` and mark 23 blocks.
2. Sneak + right click each marker with the tool and pin it to its day (01.12. to 23.12.). From that day
   on it is visible to everyone until they found it; nothing expires.
3. In `loot christmas-2026` fill the packages. Packages without a fixed day are random, a fixed day gives
   that package for the block of that day. Give one package the fixed day 24.12. and switch on
   *NPC-Belohnung*: it is never found on the map, the NPC hands it out from the 24th on to everyone who
   found all blocks of the days before.
4. Create the NPC with surf-npc and link it: `npc christmas-2026 <npc name>`. Clicking it shows a dialog
   that depends on the date and the player's progress (introduction on the first talk, today's gift,
   older gifts to catch up, the reward, a farewell). An event has at most one NPC; linking another one
   replaces it. While an NPC is linked, players see the blocks only after talking to it once;
   `npc christmas-2026 <npc name> remove` lifts that again. `start` warns if the linked NPC is missing.
5. `start christmas-2026 2026-12-31T23:59`. Each find shows the progress for a few seconds in surf-hud if
   it runs, otherwise in the action bar.

## Event lifecycle

1. `create`, then `edit` to mark blocks, then `loot` to fill packages.
2. `start` – blocks disappear from the world and become per-player fake blocks.
3. The event ends via `stop` or its end date (checked every 15 seconds).
4. `restore` – real blocks come back; until then nobody can build at the marker positions.
5. Optionally `delete`, which is only allowed after `restore`.
