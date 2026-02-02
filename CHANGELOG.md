# 🚀 AwesomeChat v1.0.7-DEV Changelog

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
- **Per-Group Sounds** — Different chat sounds per LuckPerms group
- **Per-Group Hovers** — Component-specific hover text (username + message) with per-group overrides and click actions
- **Public API** — `AwesomeChatAPI` interface for other plugins to access violations, permissions, and managers
- **Config Migration** — Incremental v3 → v6 migrator that preserves user settings and adds new sections automatically

### Bug Fixes
- Fixed hex color codes (`&#RRGGBB`) not parsing in chat formats, hover messages, or console
- Fixed hex colors rendering as wrong colors due to `Component.text()` not interpreting BungeeCord hex format — now uses Adventure `LegacyComponentSerializer`
- Fixed auto-broadcaster indexing errors
- Fixed config version mismatch causing config to regenerate on every startup
