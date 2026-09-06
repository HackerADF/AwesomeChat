# 🚀 AwesomeChat v1.0.10.1 Changelog (official)
> _Targets Paper **26.2** for the `-mc26` jar (was 26.1). The 1.19–1.21.x jar is unchanged in its requirements._

<hr>

### NEW
- **Custom item-display triggers** — define your own `[name]` triggers under `item-display.custom-triggers`, printing whatever you want (typically PlaceholderAPI output), with optional hover text, permission, and click action
  - Shorthand form (`ping: "&e[Ping: &a%player_ping%ms&e]"`) or full form with `text` / `hover` / `permission` / `click`
  - `{player}` and PlaceholderAPI placeholders resolve against the sender, so every viewer sees the same value
  - Names are merged into the trigger regex longest-first, so a short name can't shadow a longer one
  - Reserved and malformed names are skipped with a console warning
- **Paper 26.2 support** — the 26.x build moves from 26.1 alpha to 26.2 beta

<hr>

### Bug Fixes
- Fixed `/msg`, `/tell`, `/pm`, `/msgtoggle`, `/socialspy` and `/clearchat self` being operator-only — their permission nodes were never declared, so Bukkit fell back to an OP-only default and non-op players got "unknown command"
- Fixed `private-messages.enabled` and `private-messages.socialspy.enabled` doing nothing — the PM commands were registered unconditionally and kept taking priority over EssentialsX even when disabled; `/awesomechat reload` now re-applies the toggles
- Fixed hex colors (`&#RRGGBB` and `&x&R&R&G&G&B&B`) printing literally in `/broadcast` and auto-broadcasts
- Fixed the `/chatcolor` gradient menu overwriting its own Previous button with glass on every page, making it impossible to page back past the first 21 gradients
- Fixed `/mutechat` printing a literal `{prefix}` and double-prefixing its announcement — the configured message now owns its `{prefix}` the same way `/clearchat` does
- Declared `awesomechat.styling.*`, `awesomechat.format.*` and `awesomechat.display.custom.*` so permission plugins can see and tab-complete them
- Fixed the shipped `config.yml` reporting a stale `config-version`, which made fresh installs re-run the v19/v20 migrations on their second startup
- Corrected the `/chatcolor` permission documentation in `config.yml`, which listed a node the plugin never checks and omitted the real per-color and per-gradient nodes

<hr>

### Internal
- Click-event construction now uses Adventure's single-argument factories, so one source tree compiles against both Adventure 4 (1.21) and Adventure 5 (26.x)
- Config migrated to v22

<hr>
<hr>

# 🚀 AwesomeChat v1.0.8-BETA Changelog (official)
> _🚨NOTE: This is a BREAKING change, the config migrator is still expirimental and is likely to break. You may need to delete and regenerate the plugin folder if things are not working as expected. As always, make sure to_ ___**back it up**___ first.

<hr>

### NEW
- New chat filter system, easier to use and configure.
- Chatlog Support 

<hr>

### Bug Fixes
- Fixed hex color codes (`&#RRGGBB`) not parsing in chat formats, hover messages, or console
- Fixed hex colors rendering as wrong colors due to `Component.text()` not interpreting BungeeCord hex format — now uses Adventure `LegacyComponentSerializer`
- Fixed hex colors in per-group formats rendering as the wrong color — `extractTrailingColor` now correctly identifies `§x` hex sequences instead of treating the last `§` pair as a standalone color
- Fixed per-group hovers not working — `HoverManager` methods now accept the player's group for per-group hover lookups
- Fixed `/ignore` toggle not working reliably — replaced `HashMap`/`HashSet` with `ConcurrentHashMap` for thread safety between async chat and main thread commands
- Fixed /unignore and /unblock commands not registering
- Fixed color code permission bypass via uppercase `&` codes — color pattern matching is now case-insensitive
- Fixed persistent chat colors and manual `&` codes being stripped from messages — `buildComponentFromFormat` now wraps messages using Adventure's component tree instead of serializing to plain text
- Fixed auto-broadcaster indexing errors
- Fixed config version mismatch causing config to regenerate on every startup
- Fixed [item] and [inventory] formats not working
- Fixed chatcolor gui and default permissions
- Fixed `per-group-hovers` not working properly
- Fixed ignored messages still taking up a blank space
- Fixed Anti-Advertising chat filter module breaking
- Fixed colors not properly registering in the `per-group-format` section



