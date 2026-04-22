# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build (skip tests)
./gradlew clean bootJar -x test

# Run unit tests (excludes tests tagged "live")
./gradlew test

# Run live integration tests only (tagged "live", requires real DB/API keys)
./gradlew liveTest

# Run a single test class
./gradlew test --tests "com.DiscordBot.KotlinDiscordBot.SomeTestClass"
```

**Java 21** is required. The project uses Spring Boot 4.0.0-SNAPSHOT with Kotlin 2.1.21.

## Configuration

Real config files (`application.yaml`, `application-*.yaml`) are gitignored. Use `src/main/resources/application.yaml.example` as a template. Key env vars:

- `DISCORD_TOKEN` — Discord bot token
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` — PostgreSQL connection (default: `localhost:5432/discordbot_db`)
- `DDL_AUTO` — Hibernate DDL mode (default: `validate`)
- `PUBG_API_KEY` — PUBG API key (injected via `.env` in production)
- `JDA_INTENTS`, `BOTCOMMANDS_OWNERS` — JDA configuration

The app runs with `web-application-type: none` (no HTTP server exposed). H2 is used for tests.

## Architecture

This is a Spring Boot Discord bot using **JDA 5** for Discord interaction. All bot features are implemented as **slash commands**.

### Slash Command Pattern

Every command implements the `SlashCommand` interface (`command/SlashCommand.kt`) which requires `name`, `description`, `handle(event)`, and `getCommandData()`. Commands are Spring `@Component`s — they're auto-collected via `List<SlashCommand>` injection. `JdaConfig` registers all commands with Discord on startup, and `SlashCommandListener` dispatches incoming events to the matching command by name.

To add a new command: create a `@Component` implementing `SlashCommand`. No registration boilerplate needed.

### Domain Modules

- **member** — Discord user registration. `Member` entity stores Discord username, userId, nickname, level. One-to-one relationship with Wallet.
- **coin** — Cryptocurrency trading simulation using the **Bithumb API** (`https://api.bithumb.com/v1/ticker`). Uses `WebClient` (Spring WebFlux) for reactive HTTP calls. The `Market` enum (~200 entries) maps Bithumb market codes to Korean/English names.
- **money** — Wallet and Position tracking. Each member gets a `Wallet` (starting cash: 1,000,000,000 KRW). `Position` tracks coin holdings per wallet. `WealthUpdateScheduler` runs every 60 seconds to recalculate total wealth using live market prices.
- **pubp** (PUBG) — PUBG player registration and match/stats lookup via the PUBG API. Links Discord members to PUBG player IDs.

### Entity Relationships

```
Member (1) ←→ (1) Wallet ←→ (many) Position
Member (1) ←→ (1) PubgPlayers
```

### External APIs

- **Bithumb** (`WebConfig` bean) — coin price data. Base WebClient configured globally.
- **PUBG** (`PubgService`) — creates its own WebClient with Bearer token auth.

## Deployment

GitHub Actions workflow (`deploy.yml`) builds a bootJar, connects to the deployment host via Tailscale VPN + SSH, uploads the jar, and restarts the service. Triggered on push to `main` or manual dispatch.

## Language

Code comments and Discord-facing strings are primarily in **Korean**. Commit messages are also in Korean.