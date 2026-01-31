<!-- VECTOR:AI_CONTEXT_DOC v1 | AwesomeChat | LAST_SYNC:2026-01-30 -->
<!-- OPTIMIZED FOR: LLM consumption, codebase recall, feature planning -->
<!-- FORMAT: Dense structured data, minimal prose, max signal -->

# PROJECT:AWESOMECHAT

## META
- type: Bukkit/Paper Minecraft plugin (Java)
- package: dev.adf.awesomeChat
- version: 1.0.6-DEV
- api-version: 1.19+ (Paper API 1.21-R0.1-SNAPSHOT)
- java: 21
- build: Gradle + Shadow plugin (com.gradleup.shadow:8.3.3)
- author: ADF
- website: adfindustries.org
- artifact: build/libs/AwesomeChat-1.0.6-DEV.jar

## DEPENDENCIES
- HARD: LuckPerms (api:5.4) -- plugin disables without it
- SOFT: PlaceholderAPI (2.11.6) -- placeholders degrade gracefully
- SOFT: EssentialsX (2.21.1-dev+18) -- local jar in libs/
- COMPILE_ONLY: AdvancedBan (v2.3.0) -- hooks planned but not implemented
- COMPILE_ONLY: bstats-bukkit (3.1.0) -- not yet integrated
- RUNTIME: Paper server (Adventure API, MiniMessage, AsyncChatEvent)
- RUNTIME: BungeeCord Chat API (net.md_5.bungee.api.ChatColor for hex)
- LIB: Gson (bundled with server) -- used for JSON persistence

## FILE_MAP
```
src/main/java/dev/adf/awesomeChat/
  AwesomeChat.java                          -- MAIN PLUGIN CLASS (JavaPlugin)
  api/
    AwesomeChatAPI.java                     -- PUBLIC API INTERFACE
    AwesomeChatAPIImpl.java                 -- API IMPLEMENTATION
    events/
      ChatFilterViolationEvent.java         -- CUSTOM BUKKIT EVENT (Cancellable)
  commands/
    AwesomeChatCommand.java                 -- /awesomechat [reload|info]
    AwesomeChatTabCompleter.java            -- tab-complete for /awesomechat
    BroadcastCommand.java                   -- /broadcast <message>
    MessageCommand.java                     -- /msg <player> <message>
    MessageTabCompleter.java                -- tab-complete for /msg
    MsgToggleCommand.java                   -- /msgtoggle [on|off]
    MsgToggleTabCompleter.java              -- tab-complete for /msgtoggle
    ReplyCommand.java                       -- /reply <message>
    SocialSpyCommand.java                   -- /socialspy [on|off]
    SocialSpyTabCompleter.java              -- tab-complete for /socialspy
  listeners/
    ChatListener.java                       -- AsyncChatEvent handler (CORE)
    CommandListener.java                    -- PlayerCommandPreprocessEvent
    ExampleListener.java                    -- EMPTY/UNUSED PLACEHOLDER
  logging/
    FilterLogger.java                       -- file-based violation logging
  managers/
    AutoBroadcasterManager.java             -- scheduled broadcast system
    ChatFilterManager.java                  -- LARGEST CLASS: filter engine
    ConfigManager.java                      -- config version migration
    HoverManager.java                       -- hover text component builder
    PrivateMessageManager.java              -- PM state tracking
    SocialSpyManager.java                   -- spy toggle state
    SoundManager.java                       -- per-group sound system
  storage/
    ViolationStorage.java                   -- per-player JSON file storage
  utils/
    ChatFormatPermissionUtil.java           -- permission-gated formatting
    LuckPermsUtil.java                      -- LuckPerms static helpers

src/main/resources/
  plugin.yml                                -- plugin descriptor
  config.yml                                -- main configuration (~500 lines)
  AutoBroadcaster.yml                       -- broadcast messages config
  filter/
    abuse_violence.txt                      -- wildcard filter list
    curse_words.txt                         -- wildcard filter list
    sexual.txt                              -- wildcard filter list
    slurs.txt                               -- wildcard filter list
```

## ARCHITECTURE

### LIFECYCLE
1. onEnable: ConfigManager(version check) -> saveDefaultConfig -> init managers (PM, SocialSpy, ChatFilter, AutoBroadcaster, Sound, Hover, API) -> register listeners -> register commands
2. onDisable: log message only (no cleanup of schedulers explicitly)
3. Reload: /awesomechat reload -> reloadConfig + AutoBroadcaster restart + ChatFilterManager re-instantiation

### MANAGER_REGISTRY (all held as fields on AwesomeChat)
| Manager                  | State Storage           | Persistence          |
|--------------------------|------------------------|----------------------|
| ConfigManager            | config version int     | config.yml           |
| ChatFilterManager        | offensesCache Map<UUID,Map<String,Integer>>, spam/cooldown caches | data/uuid.json (Gson) |
| PrivateMessageManager    | lastMessaged Map<UUID,UUID>, disabledMessages Set<UUID> | IN-MEMORY ONLY |
| SocialSpyManager         | spies Set<UUID>        | IN-MEMORY ONLY       |
| AutoBroadcasterManager   | broadcasts list, taskId, currentIndex | AutoBroadcaster.yml |
| SoundManager             | stateless (reads config each call) | config.yml |
| HoverManager             | stateless (reads config each call) | config.yml |
| ViolationStorage         | per-player JSON files   | data/<uuid>.json     |
| FilterLogger             | append-only log files   | logs/log-violations-YYYY-MM-DD.log |

### EVENT_FLOW
```
AsyncChatEvent -> ChatListener.onPlayerChat()
  1. Check chat-formatting.enabled
  2. Extract plain text from Component
  3. ChatFilterManager.checkAndHandle(player, msg, "message")
     -> bypass check -> cooldown -> spam/similarity -> banned words -> anti-advertising -> regex rules
     -> on violation: increment offenses, save async, fire ChatFilterViolationEvent, execute punishment
  4. Resolve LuckPerms prefix/suffix
  5. Resolve PlaceholderAPI placeholders
  6. Determine chat format (global or per-group)
  7. Apply LuckPerms meta (chat-color, chat-format)
  8. Process message formatting (MiniMessage or legacy color codes)
  9. Permission-based format filtering (ChatFormatPermissionUtil)
  10. Build Adventure Component with hover/click events (HoverManager)
  11. Render via event.renderer() or manual broadcast (if chat-signing disabled)
  12. Play sound via SoundManager

PlayerCommandPreprocessEvent -> CommandListener.onCommand()
  1. Check chat-filter.filter-commands config
  2. ChatFilterManager.checkAndHandle(player, commandMsg, "command")
```

### COMMAND_REGISTRY
| Command      | Aliases                              | Permission              | Executor              |
|--------------|--------------------------------------|------------------------|-----------------------|
| /awesomechat | /ac                                  | awesomechat.use        | AwesomeChatCommand    |
| /broadcast   | (none)                               | awesomechat.broadcast  | BroadcastCommand      |
| /msg         | /tell /message /whisper /w /pm       | awesomechat.msg        | MessageCommand        |
| /reply       | /r                                   | awesomechat.msg        | ReplyCommand          |
| /msgtoggle   | /togglepm                            | awesomechat.msgtoggle  | MsgToggleCommand      |
| /socialspy   | /sspy                                | awesomechat.socialspy  | SocialSpyCommand      |

### PERMISSION_TREE
```
awesomechat.use                          -- /awesomechat base
awesomechat.reload                       -- /awesomechat reload
awesomechat.broadcast                    -- /broadcast
awesomechat.broadcast.cooldown.<seconds> -- dynamic cooldown per-player
awesomechat.msg                          -- /msg /reply
awesomechat.msgtoggle                    -- /msgtoggle
awesomechat.socialspy                    -- /socialspy
awesomechat.filter.bypass                -- bypass all chat filters
awesomechat.filter.notify                -- receive staff filter messages
awesomechat.moderator                    -- API moderator check
awesomechat.moderator.punish             -- API can-punish check
awesomechat.moderator.view               -- API can-view check
awesomechat.format.minimessage           -- use MiniMessage tags
awesomechat.format.minimessage.advanced  -- use click/hover/insertion tags
awesomechat.styling.bold                 -- &l / <bold>
awesomechat.styling.italic               -- &o / <italic>
awesomechat.styling.underline            -- &n / <underlined>
awesomechat.styling.strikethrough        -- &m / <strikethrough>
awesomechat.styling.obfuscated           -- &k / <obfuscated>
awesomechat.styling.color.<0-f>          -- legacy color codes
awesomechat.styling.color.hex            -- &#RRGGBB / <#hex>
```

### CHAT_FILTER_ENGINE (ChatFilterManager)
Pipeline order: bypass -> cooldown -> spam/similarity -> banned-words -> anti-advertising -> regex rules

**Cooldown**: configurable ms, per-command whitelist/targeting
**Spam**: Jaro-Winkler similarity, configurable threshold (0.0-1.0), limit count, arg-sensitive mode
**Banned Words**: loaded from filter/ directory (txt files with wildcard patterns using * glob), word-boundary matching
**Anti-Advertising**: TLD blocking, phrase blocking, generic domain regex
**Regex Rules**: named rules with per-offense-count punishments, "repeat" fallback
**Punishments**: graduated system (offense count -> console command), supports {player}, {uuid}, {message}, {matched} placeholders
**Persistence**: offenses in data/uuid.json, violations per-player in data/<uuid>.json, logs in logs/ directory

### API (AwesomeChatAPI interface)
```java
getAPIVersion() -> "1.0"
isModerator(Player) -> boolean
hasPermission(Player, String) -> boolean
canModerate(Player) -> boolean
getPlayerGroup(Player) -> String
getPlayerPrefix(Player) -> String
getPlayerSuffix(Player) -> String
getPlayerMetadata(Player, String) -> String
getViolations(UUID) -> List<ViolationRecord>
getViolationCount(UUID) -> int
getViolationsByRule(UUID, String) -> List<ViolationRecord>
addViolation(UUID, String) -> void
clearViolations(UUID) -> void
isMessageBlocked(Player, String) -> boolean
getMatchedRule(String) -> String  // STUB: returns null
getChatFilterManager() -> ChatFilterManager
getPrivateMessageManager() -> PrivateMessageManager
getSocialSpyManager() -> SocialSpyManager
```

### COLOR/FORMAT_SYSTEM
- Legacy: &0-&f, &k-&o, &r via ChatColor.translateAlternateColorCodes
- Hex: &#RRGGBB via regex -> ChatColor.of("#hex")
- BungeeCord hex: &x&R&R&G&G&B&B format
- MiniMessage: <bold>, <italic>, <#hex>, <gradient>, <rainbow>, etc.
- Section sign conversion: convertToMiniMessageFormat() maps section codes to MiniMessage tags
- Permission-gated: ChatFormatPermissionUtil strips codes player lacks permission for

### CONFIG_SYSTEM
- config-version tracking (currently expects 5, file says 6 -- MISMATCH BUG)
- Auto-backup old config to config.yml.old on version mismatch
- Regenerates default config on version mismatch
- AutoBroadcaster has separate YAML file

### SOUND_SYSTEM
- Chat sounds: global fallback + per-group (LuckPerms group -> sound config)
- Broadcast sounds: single sound from config
- PM sounds: sent/received separate sounds
- All sounds configurable: name (Sound enum), volume (float), pitch (float)

### HOVER/CLICK_SYSTEM
- Global hover text (username + message components)
- Per-group hover text (LuckPerms group -> different hover lines)
- Click actions: suggest_command, run_command, copy_to_clipboard
- Per-component click (username click vs message click)
- Supports PlaceholderAPI in hover lines
- Custom %awesomechat_violations% placeholder in hovers

## KNOWN_ISSUES
1. ConfigManager checks version==5 but config.yml has config-version:6 -- will always regenerate
2. ExampleListener.java is empty/unused -- dead code
3. PrivateMessageManager has socialSpy field/methods duplicating SocialSpyManager
4. MessageCommand line 95: reads "private-messages.sound.sent.name" for RECEIVED sound (copy-paste bug)
5. ReplyCommand has same copy-paste bug for received sound config paths
6. PlaceholderAPI.setPlaceholders called TWICE in ChatListener (lines 120+124) -- duplicate call
7. ChatFilterManager constructor reads offenses file twice (constructor body + loadOffensesFromFile)
8. getMatchedRule() in API returns null always -- unimplemented stub
9. broadcastCooldowns in main class + cooldown in ChatFilterManager are separate systems
10. No cleanup of BukkitRunnable tasks on disable (AutoBroadcaster taskId)
11. SocialSpy and PM toggle states are in-memory only -- lost on restart
12. Anti-advertising domain regex is overly broad -- matches any "word.word" pattern
13. ChatFilterManager.ChatSettings uses static fields mutated at construction -- not thread-safe if multiple instances
14. Sound volume of 100 is extremely loud (Bukkit Sound volume is typically 0.0-1.0 scale)
15. bstats dependency declared but never initialized

## FEATURE_INVENTORY
- [x] Custom chat formatting (global + per-group)
- [x] LuckPerms integration (prefix/suffix/group/meta)
- [x] PlaceholderAPI integration
- [x] Hover text on username/message (global + per-group)
- [x] Click events on username/message (suggest/execute/copy)
- [x] Private messaging (/msg /reply)
- [x] Message toggle (/msgtoggle)
- [x] Social spy (/socialspy)
- [x] Broadcast command (/broadcast) with cooldown
- [x] Auto-broadcaster (scheduled, configurable)
- [x] Chat filter (cooldown, spam, similarity, banned words, anti-advertising, regex)
- [x] Graduated punishment system (offense count -> commands)
- [x] Filter violation logging (file-based)
- [x] Violation storage (per-player JSON)
- [x] Custom Bukkit event (ChatFilterViolationEvent)
- [x] Permission-gated formatting (per color/style code)
- [x] MiniMessage support (toggleable)
- [x] Chat signing disable option
- [x] Sound system (chat + broadcast + PM, per-group)
- [x] Config version migration
- [x] Public API for other plugins
- [x] Hex color support (&#RRGGBB)
- [ ] AdvancedBan hooks (dependency declared, not implemented)
- [ ] bstats metrics (dependency declared, not integrated)
- [ ] PlaceholderAPI expansion registration (commented out)
- [x] Chat channels (staff, admin, vip + custom owner-defined channels)
- [x] Ignore player system (/ignore with JSON persistence + PM integration)
- [ ] Staff chat
- [ ] Chat clear command
- [ ] Mention/ping system
- [ ] Chat radius/local chat
- [ ] Tab list formatting
- [ ] Join/leave messages
- [ ] Death messages
- [ ] Item show in chat
- [ ] AFK detection/status
- [ ] Chat cooldown per-group
- [ ] Word replacement/censor mode (replace instead of block)
- [ ] Database storage (MySQL/SQLite)
- [ ] BungeeCord/Velocity cross-server messaging
- [ ] Chat logging to database
- [ ] Emoji/symbol shortcuts
- [ ] Chat games/interactivity

## BUILD_CONFIG
```groovy
// Gradle with Shadow plugin
// Paper API 1.21-R0.1-SNAPSHOT
// Java 21
// compileOnly: paper-api, bstats, luckperms, placeholderapi, advancedban, essentialsx
// shadowJar: archiveClassifier=""
```

## PLANNED_FEATURES

### FEATURE:CUSTOM_CHAT_CHANNELS
- Status: COMPLETE
- Priority: HIGH
- Scope: Staff chat + owner-configurable custom channels (admin-only, VIP, etc.)
- Commands: /channel (/ch) with subcommands: join, leave, toggle, create, delete, list, send
- Modes: toggle mode (all messages route to channel) + prefix mode (/ch send <channel> <msg>)
- Access: permission-gated per channel (awesomechat.channel.<name>)
- Format: per-channel display prefix, color, format string
- Sound: per-channel configurable sound
- Config: channels section with default "staff" channel, owners can add more via config
- Files: ChannelManager.java, ChannelCommand.java, ChannelTabCompleter.java
- Modified: AwesomeChat.java, ChatListener.java, config.yml, plugin.yml

### FEATURE:IGNORE_PLAYER_SYSTEM
- Status: COMPLETE
- Files: IgnoreManager.java, IgnoreCommand.java, IgnoreTabCompleter.java
- Modified: AwesomeChat.java, ChatListener.java, MessageCommand.java, ReplyCommand.java, plugin.yml
- Commands: /ignore <player> (toggle), /ignore list
- Persistence: JSON per-player in data/ignores/<uuid>.json
- Integration: hides chat messages, blocks PMs (msg + reply), staff bypass via awesomechat.ignore.bypass

### FEATURE:MENTION_PING_SYSTEM
- Status: APPROVED (modified)
- Priority: TBD
- Scope: @player mentions, @(luckperms_role) group mentions, @everyone/@here permission-gated
- Sounds: configurable per mention type (player mention sound, role mention sound, everyone/here sound)
- Permissions: awesomechat.mention.player, awesomechat.mention.role.<rolename>, awesomechat.mention.everyone, awesomechat.mention.here
- Highlight: colored/formatted mention text in chat
- Notification: sound + optional action bar alert for mentioned players
- Config: per-mention-type sound name/volume/pitch, highlight color, enable/disable each type

## RESEARCH_LOG
<!-- Append new research entries below this line -->
| Date       | Topic                  | Findings |
|------------|------------------------|----------|
| 2026-01-30 | Initial codebase scan  | Full project indexed. 28 Java source files, 3 YAML configs, 4 filter txt files. Core architecture: manager pattern with event-driven chat processing. Paper-native using Adventure API. |
| 2026-01-30 | Mention system design  | User wants @player, @(LP role), @everyone/@here (permission-gated), per-mention-type configurable sounds |
| 2026-01-30 | Chat channels impl     | Built ChannelManager, ChannelCommand, ChannelTabCompleter. 3 default channels (staff/admin/vip). Toggle mode + one-off send. Per-channel: prefix, format, permission, sound. Integrated with ChatListener routing + reload command. BUILD SUCCESSFUL. |
| 2026-01-30 | Ignore system impl     | Built IgnoreManager (JSON persistence), IgnoreCommand, IgnoreTabCompleter. Integrated with ChatListener (hide msgs), MessageCommand, ReplyCommand (block PMs). Staff bypass permission. BUILD SUCCESSFUL. |
