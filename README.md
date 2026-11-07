# surf-seasonal-events

Seasonal events (Easter, Christmas, Halloween …) for the survival server. Staff mark blocks in the
world; while an event runs, every player sees their own fake blocks there and collects loot packages
by clicking them.

Two modes:

- **DAILY** – each player gets one block per day. Packages are handed out at random; a package can be
  pinned to a fixed day, which then takes precedence.
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

Requirements: JDK 25, MariaDB, RabbitMQ.

1. Create a MariaDB database `seasonal_events`.
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
| `edit <event>` – marker tool, left click marks, right click unmarks | `.edit` |
| `setend <event> <yyyy-MM-ddTHH:mm\|none>` | `.edit` |
| `loot <event>` – loot package GUI | `.loot` |
| `copyloot <from> <to>` – copies packages, not fixed days | `.loot` |
| `start <event> [end]` | `.start` |
| `stop` | `.stop` |
| `restore <event>` – puts the real blocks back | `.restore` |
| `delete <event>` | `.delete` |

Every subcommand also requires the base permission. Times are Europe/Berlin.

## Event lifecycle

1. `create`, then `edit` to mark blocks, then `loot` to fill packages.
2. `start` – blocks disappear from the world and become per-player fake blocks.
3. The event ends via `stop` or its end date (checked every 15 seconds).
4. `restore` – real blocks come back; until then nobody can build at the marker positions.
5. Optionally `delete`, which is only allowed after `restore`.
