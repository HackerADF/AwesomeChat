# AwesomeChat

A powerful, fully-featured chat management plugin for Paper/Spigot servers (1.19+).

## Requirements

| Dependency | Type | Notes |
|------------|------|-------|
| [LuckPerms](https://luckperms.net/download) | **Required** | Provides groups, prefixes, suffixes, and meta |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | Optional | Dynamic placeholders in chat, hovers, and messages |
| Java 21+ | **Required** | Server must run Java 21 or higher |
| Paper 1.19+ | **Required** | Uses Paper's Adventure API and AsyncChatEvent |

## Features

### Chat Formatting
- **Per-group chat formats** - Different chat layouts per LuckPerms group
- **LuckPerms integration** - Automatic prefix/suffix from permissions
- **PlaceholderAPI support** - Use any PAPI placeholder in chat formats
- **Custom placeholders** - Define your own `{player}`, `{prefix}`, `{suffix}`, `{message}` tokens
- **Hex color support** - Use `&#RRGGBB` for custom RGB colors
- **MiniMessage support** - Full Adventure MiniMessage formatting (toggleable)
- **Permission-based formatting** - Gate color codes and styles behind permissions

### Chat Channels
- **Custom channels** - Create unlimited permission-gated channels (staff, admin, VIP, etc.)
- **Toggle mode** - `/ch join <channel>` routes all messages to that channel
- **Quick send** - `/ch <channel> <message>` for one-off channel messages
- **Per-channel config** - Custom prefix, format, sound, and permission per channel
- **Easy setup** - Define channels in `config.yml` with no code changes

### Chat Filter
- **Cooldown system** - Rate-limit messages with configurable ms delay
- **Anti-spam** - Jaro-Winkler similarity detection blocks repeated messages
- **Banned words** - Wildcard pattern matching from external .txt files
- **Anti-advertising** - TLD blocking, phrase blocking, domain regex
- **Custom regex rules** - Named rules with configurable patterns
- **Graduated punishments** - Escalating actions (warn, mute, ban) per offense count
- **Censor mode** - Replace bad words with asterisks instead of blocking (configurable)
- **Command filtering** - Apply filters to commands, not just chat
- **Bypass permission** - Staff can bypass all filters
- **Violation logging** - File-based logs + per-player JSON violation records
- **Custom events** - `ChatFilterViolationEvent` for other plugins to hook into

### Private Messaging
- **`/msg` and `/reply`** - Full private messaging with aliases (`/tell`, `/whisper`, `/w`, `/pm`)
- **Message toggle** - `/msgtoggle` to disable incoming PMs
- **Social spy** - `/socialspy` lets staff monitor private messages
- **Custom sounds** - Separate sounds for sent and received messages
- **Configurable formats** - Customize sender, receiver, and spy message formats

### Player Ignore
- **`/ignore <player>`** - Toggle hiding a player's chat messages and PMs
- **Persistent storage** - Ignore lists survive server restarts (JSON-based)
- **PM integration** - Ignored players can't send you private messages
- **Staff bypass** - Players with `awesomechat.ignore.bypass` can't be ignored
- **List view** - `/ignore list` shows all currently ignored players

### Mentions
- **`@player`** - Mention a specific player by name
- **`@(role)`** - Mention all players in a LuckPerms group (e.g., `@(admin)`)
- **`@everyone` / `@here`** - Mention all online players
- **Per-type sounds** - Different notification sounds for each mention type
- **Action bar alerts** - Configurable action bar message for mentioned players
- **Highlight colors** - Mentions are color-highlighted in chat
- **Permission-gated** - Each mention type has its own permission

### Emoji Shortcuts
- **`:shortcode:` replacement** - Type `:heart:`, `:star:`, `:fire:` etc. in chat
- **20 built-in emojis** - Common symbols included out of the box
- **Fully configurable** - Add, remove, or change shortcuts in `config.yml`
- **Permission-gated** - Requires `awesomechat.emoji`

### Join/Leave Messages
- **Per-group messages** - Different join/leave messages per LuckPerms group
- **First join** - Special message for new players
- **MOTD** - Send welcome lines to players on join
- **Sounds** - Configurable join/leave sounds
- **PlaceholderAPI support** - Dynamic placeholders in join/leave messages
- **Vanish support** - Hides messages for vanished players (EssentialsX)

### Item Display
- **`[item]` / `[hand]`** - Show your held item in chat with native hover tooltip
- **`[inventory]` / `[inv]`** - Clickable link to view a snapshot of your inventory
- **`[enderchest]` / `[echest]`** - Clickable link to view your ender chest
- **`[/command]`** - Clickable command suggestion in chat
- **Snapshot system** - Inventory snapshots with configurable TTL expiry
- **Read-only GUIs** - Viewers see a locked display, no item theft
- **Permission-gated** - Separate permissions for each trigger type

### Chat Radius / Local Chat
- **Distance-based chat** - Players only see messages from nearby players
- **Per-group radii** - Staff can have unlimited range, default players limited
- **Shout prefix** - `!` prefix sends messages server-wide (configurable)
- **Cross-world toggle** - Control whether chat crosses world boundaries
- **Fully toggleable** - Disabled by default, enable when ready

### Chat Logging
- **Database storage** - Log all chat messages to SQLite or MySQL
- **`/chatlogs` search** - Search by player with time filters (`time:`, `after:`, `before:`)
- **Clickable pagination** - Browse results with clickable page navigation
- **Filtered message tracking** - Censored/blocked messages flagged in logs
- **Async writes** - Zero performance impact on the main thread

### Chat Color
- **`/chatcolor` GUI** - 54-slot chest interface for choosing a persistent chat color
- **16 standard colors** - All classic Minecraft color codes
- **Gradient presets** - 8 dual-color and 5 triple-color gradient presets (Sunset, Ocean, Royal, etc.)
- **Custom gradients** - Create your own gradient with up to 4 hex color stops (OP only by default)
- **Style toggles** - Bold, italic, underline, strikethrough, obfuscated (permission-gated)
- **Persistent storage** - Per-player JSON files survive restarts
- **LuckPerms priority** - `chat-color` meta overrides the GUI selection
- **Non-destructive** - Manual `&` color codes still work and override the persistent color
- **Reset option** - `/chatcolor reset` clears your persistent color

### Interactive Messages
- **Hover text** - Per-group and global hover tooltips on usernames and messages
- **Click events** - Suggest command, run command, or copy to clipboard on click
- **Per-component** - Different hover/click for username vs message
- **PlaceholderAPI in hovers** - Dynamic data in hover tooltips

### Broadcasting
- **`/broadcast`** - Send formatted announcements with sounds
- **Auto-broadcaster** - Scheduled rotating messages from `AutoBroadcaster.yml`
- **Permission-based cooldown** - `awesomechat.broadcast.cooldown.<seconds>`

### Moderation Tools
- **`/clearchat`** - Clear chat for yourself or all players
- **`/mutechat`** - Toggle global chat mute (staff bypass available)
- **Chat clear bypass** - Staff exempt from `/clearchat all`
- **Chat signing disable** - Disable 1.19+ chat signature enforcement
- **Filter notifications** - Staff receive alerts when filters trigger

### Sound System
- **Per-group chat sounds** - Different sounds per LuckPerms group when chatting
- **Global fallback** - Default sound when no group-specific sound is set
- **Broadcast sounds** - Sound effects on broadcast messages
- **PM sounds** - Separate sent/received sounds for private messages
- **Channel sounds** - Per-channel notification sounds

### Developer API
- **Public API interface** - `AwesomeChatAPI` for other plugins to access
- **Permission checks** - `isModerator()`, `canModerate()`, `hasPermission()`
- **Player data** - Get group, prefix, suffix, metadata via LuckPerms
- **Violation access** - Query, add, and clear player violations
- **Filter access** - Check if a message would be blocked
- **Manager access** - Get ChatFilterManager, PrivateMessageManager, SocialSpyManager

## Commands

| Command | Aliases | Permission | Description |
|---------|---------|------------|-------------|
| `/awesomechat` | `/ac` | `awesomechat.use` | Plugin info and reload |
| `/broadcast` | - | `awesomechat.broadcast` | Broadcast announcements |
| `/msg <player> <message>` | `/tell`, `/whisper`, `/w`, `/pm` | `awesomechat.msg` | Private message |
| `/reply <message>` | `/r` | `awesomechat.msg` | Reply to last PM |
| `/msgtoggle` | `/togglepm` | `awesomechat.msgtoggle` | Toggle private messages |
| `/socialspy` | `/sspy` | `awesomechat.socialspy` | Monitor private messages |
| `/channel` | `/ch` | `awesomechat.channel` | Chat channel management |
| `/ignore` | `/block` | `awesomechat.ignore` | Ignore a player |
| `/clearchat` | `/cc` | `awesomechat.clearchat` | Clear chat |
| `/mutechat` | `/chatmute`, `/mc` | `awesomechat.mutechat` | Toggle chat mute |
| `/chatlogs` | `/cl`, `/chatlog` | `awesomechat.chatlogs` | Search chat logs |
| `/chatcolor` | `/chatcolour`, `/ccolor` | `awesomechat.command.chatcolor` | Chat color picker GUI |

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `awesomechat.use` | OP | Base plugin command |
| `awesomechat.reload` | OP | Reload configuration |
| `awesomechat.broadcast` | OP | Broadcast messages |
| `awesomechat.broadcast.cooldown.<s>` | - | Set broadcast cooldown |
| `awesomechat.msg` | true | Private messaging |
| `awesomechat.msgtoggle` | true | Toggle PMs |
| `awesomechat.socialspy` | OP | Social spy |
| `awesomechat.channel` | true | Chat channels base |
| `awesomechat.channel.<name>` | varies | Per-channel access |
| `awesomechat.ignore` | true | Ignore command |
| `awesomechat.ignore.bypass` | OP | Can't be ignored |
| `awesomechat.clearchat` | true | Clear own chat |
| `awesomechat.clearchat.all` | OP | Clear all chat |
| `awesomechat.clearchat.bypass` | OP | Exempt from chat clear |
| `awesomechat.mutechat` | OP | Toggle global chat mute |
| `awesomechat.mutechat.bypass` | OP | Chat when muted |
| `awesomechat.filter.bypass` | OP | Bypass chat filter |
| `awesomechat.filter.notify` | OP | Receive filter alerts |
| `awesomechat.moderator` | OP | API moderator access |
| `awesomechat.mention.player` | true | Mention players with @name |
| `awesomechat.mention.role` | OP | Mention groups with @(role) |
| `awesomechat.mention.everyone` | OP | Use @everyone |
| `awesomechat.mention.here` | OP | Use @here |
| `awesomechat.emoji` | true | Use emoji shortcuts |
| `awesomechat.chat.shout` | true | Shout in chat radius mode |
| `awesomechat.chatlogs` | OP | Search chat logs |
| `awesomechat.display.item` | true | Use `[item]`/`[hand]` in chat |
| `awesomechat.display.inventory` | true | Use `[inventory]`/`[inv]` in chat |
| `awesomechat.display.enderchest` | OP | Use `[enderchest]`/`[echest]` in chat |
| `awesomechat.display.command` | true | Use `[/command]` in chat |
| `awesomechat.format.minimessage` | OP | Use MiniMessage tags |
| `awesomechat.command.chatcolor` | true | Use `/chatcolor` command |
| `awesomechat.chatcolor.gradient` | true | Use gradient presets in GUI |
| `awesomechat.chatcolor.custom` | OP | Create custom gradient colors |
| `awesomechat.styling.*` | - | Per-code formatting permissions |

## Configuration

All settings are in `config.yml`. Key sections:

- **`channels`** - Define custom chat channels
- **`chat-format`** - Global and per-group chat formatting
- **`chat-filter`** - Filter rules, punishments, cooldowns, spam detection
- **`private-messages`** - PM format, sounds, and error messages
- **`hoverable-messages`** - Hover text configuration
- **`clickable-messages`** - Click action configuration
- **`broadcast`** - Broadcast format and sound
- **`clearchat`** - Chat clear settings
- **`mutechat`** - Mute chat messages and configuration
- **`join-leave`** - Join/leave messages, first join, MOTD, sounds
- **`emoji`** - Emoji shortcut definitions
- **`mentions`** - Mention types, highlight colors, sounds, action bar
- **`item-display`** - Item display triggers and snapshot TTL
- **`chat-radius`** - Local chat distance, shout prefix, per-group radii
- **`chat-logging`** - Database storage (SQLite/MySQL), search settings
- **`chatcolor`** - Persistent chat color GUI (enable/disable)

Auto-broadcaster messages are configured separately in `AutoBroadcaster.yml`.

## Building

```
./gradlew shadowJar
```

Output: `build/libs/AwesomeChat-<version>.jar`

## Support

- Discord: [Join Server](https://discord.gg/Z4gtF25jpC)
- Discord: adf.dev
- Website: [adfindustries.org](https://adfindustries.org)
