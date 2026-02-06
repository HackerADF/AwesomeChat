# 🚀 AwesomeChat v1.0.8-DEV Changelog (official)

### New Features
- **Chat Channels** — Staff, admin, VIP + custom channels with per-channel formats, sounds, and permissions (`/ch join`, `/ch send`, `/ch leave`)
- **Ignore System** — Block players from chat and PMs with persistent JSON storage (`/ignore`, `/ignore list`)
- **Mute Chat** — Server-wide chat mute toggle with bypass permission (`/mutechat`)
- **Clear Chat** — Self or global chat clear with bypass and announcements (`/clearchat`)
- **Emoji Shortcuts** — 20 built-in `:shortcode:` → unicode replacements, permission-gated
- **Censor Mode** — Alternative to blocking: replaces matched words with `****` instead of cancelling the message
- **Join/Leave Messages** — Per-group join, leave, first-join, MOTD, and sounds with EssentialsX vanish support
- **Mention System** — `@player`, `@(role)`, `@everyone`, `@here` with per-type highlight colors, sounds, and action bar notifications
- **Item Display** — `[item]`, `[inventory]`, `[enderchest]`, `[/command]` triggers in chat with hover tooltips and snapshot GUIs
- **Chat Radius** — Distance-based local chat with per-group radii, shout prefix for global messages, cross-world toggle
- **Chat Logging** — SQLite/MySQL storage with `/chatlogs` search, time filters, and clickable pagination
- **Chat Color GUI** — `/chatcolor` opens a 54-slot chest GUI to pick a persistent chat color: 16 standard colors, 13 gradient presets (dual + triple), custom gradients up to 4 hex stops, and style toggles (bold, italic, etc.)
- **Per-Group Sounds** — Different chat sounds per LuckPerms group
- **Per-Group Hovers** — Component-specific hover text (username + message) with per-group overrides and click actions
- **Public API** — `AwesomeChatAPI` interface for other plugins to access violations, permissions, and managers
- **Config Migration** — Incremental v3 → v7 migrator that preserves user settings and adds new sections automatically

### Bug Fixes
- Fixed hex color codes (`&#RRGGBB`) not parsing in chat formats, hover messages, or console
- Fixed hex colors rendering as wrong colors due to `Component.text()` not interpreting BungeeCord hex format — now uses Adventure `LegacyComponentSerializer`
- Fixed hex colors in per-group formats rendering as the wrong color — `extractTrailingColor` now correctly identifies `§x` hex sequences instead of treating the last `§` pair as a standalone color
- Fixed per-group hovers not working — `HoverManager` methods now accept the player's group for per-group hover lookups
- Fixed `/ignore` toggle not working reliably — replaced `HashMap`/`HashSet` with `ConcurrentHashMap` for thread safety between async chat and main thread commands
- Fixed color code permission bypass via uppercase `&` codes — color pattern matching is now case-insensitive
- Fixed persistent chat colors and manual `&` codes being stripped from messages — `buildComponentFromFormat` now wraps messages using Adventure's component tree instead of serializing to plain text
- Fixed auto-broadcaster indexing errors
- Fixed config version mismatch causing config to regenerate on every startup
