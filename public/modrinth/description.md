# AwesomeChat

The all-in-one chat management plugin for Paper servers. Format chat, filter messages, manage private messaging, create chat channels, and more -- all from a single, highly configurable plugin.

---

## Why AwesomeChat?

Most servers need 3-5 separate plugins to handle chat formatting, filtering, private messaging, and moderation. AwesomeChat replaces them all with one lightweight, well-integrated solution.

- Zero bloat. Every feature is toggleable.
- Deep LuckPerms integration. Per-group everything.
- Adventure API native. Full MiniMessage and hex color support.
- Actively maintained for 1.19+.

---

## Features at a Glance

### Chat Formatting
Fully customizable chat layout with per-group formats, LuckPerms prefix/suffix, PlaceholderAPI support, hex colors (`&#RRGGBB`), and MiniMessage formatting. Permission-based color/style gating lets you control exactly which formatting each rank can use.

### Chat Channels
Create unlimited custom chat channels (staff, admin, VIP, or anything you want). Each channel has its own permission, prefix, format, and sound. Players toggle into channels with `/ch join <channel>` or send one-off messages with `/ch <channel> <message>`.

### Chat Filter Engine
Built-in multi-layer filter pipeline:
- **Cooldown** -- Rate limit messages
- **Spam detection** -- Jaro-Winkler similarity algorithm catches repeated messages
- **Censor mode** -- Replace bad words with asterisks instead of blocking
- **Banned words** -- Wildcard patterns loaded from `.txt` files
- **Anti-advertising** -- TLD blocking, phrase matching, domain regex
- **Custom regex rules** -- Create named rules with your own patterns
- **Graduated punishments** -- Escalating actions per offense (warn -> mute -> ban)
- **Command filtering** -- Filters apply to commands too, not just chat

### Private Messaging
`/msg`, `/reply`, `/whisper`, and more. Custom formats, sounds for sent/received, message toggle, and social spy for staff monitoring.

### Player Ignore
`/ignore <player>` hides their chat messages and blocks their PMs. Persists across restarts. Staff bypass available.

### Mentions
Tag players with `@name`, groups with `@(role)`, or everyone with `@everyone`/`@here`. Each mention type has its own highlight color, notification sound, and action bar alert. Fully permission-gated.

### Emoji Shortcuts
Type `:heart:`, `:star:`, `:fire:` and more in chat to insert unicode symbols. 20 built-in shortcuts, fully configurable, and permission-gated via `awesomechat.emoji`.

### Join/Leave Messages
Per-group join/leave messages with first-join support, MOTD lines, sounds, PlaceholderAPI placeholders, and EssentialsX vanish integration. Fully toggleable.

### Interactive Messages
Hover text and click events on chat messages. Per-group and per-component (username vs message) configuration. Supports suggest command, run command, and copy to clipboard.

### Moderation Tools
- `/clearchat` -- Clear chat (self or global, with staff bypass)
- `/mutechat` -- Toggle server-wide chat mute
- `/broadcast` -- Formatted announcements with sounds and cooldowns
- Auto-broadcaster -- Scheduled rotating messages

### Sound System
Per-group chat sounds via LuckPerms groups. Separate sounds for chat messages, broadcasts, PMs, and channels. All configurable with custom sound name, volume, and pitch.

### Developer API
Public `AwesomeChatAPI` interface for other plugins to access player data, violations, filter checks, and moderation features. Custom `ChatFilterViolationEvent` for hooking into the filter pipeline.

---

## Requirements

| | |
|---|---|
| **Server** | Paper 1.19+ |
| **Java** | 21+ |
| **Required** | [LuckPerms](https://luckperms.net) |
| **Optional** | [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) |

---

## Commands

| Command | Description |
|---------|-------------|
| `/awesomechat` (`/ac`) | Plugin info and reload |
| `/broadcast` | Send announcements |
| `/msg` (`/tell`, `/w`, `/pm`) | Private message |
| `/reply` (`/r`) | Reply to last PM |
| `/msgtoggle` | Toggle PMs on/off |
| `/socialspy` (`/sspy`) | Monitor private messages |
| `/channel` (`/ch`) | Chat channel management |
| `/ignore` (`/block`) | Ignore a player |
| `/clearchat` (`/cc`) | Clear chat |
| `/mutechat` (`/mc`) | Toggle chat mute |

---

## Configuration

Everything is configurable in `config.yml`. Every feature can be enabled or disabled independently. Auto-broadcaster has its own `AutoBroadcaster.yml`.

---

## Support

- Discord: [Join Server](https://discord.gg/Z4gtF25jpC)
- Discord: `adf.dev`
