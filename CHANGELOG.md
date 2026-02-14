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



