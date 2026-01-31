# Feature Suggestion Prompt Method

## Overview
This document captures the exact prompt flow used for suggesting and implementing new features in AwesomeChat. Follow this method exactly when the user asks for new feature suggestions.

## Prerequisites
1. Read `.data/dev/vector.md` first -- this is the infinite research tool containing the full codebase map, architecture, feature inventory, known issues, and planned features.
2. Check the FEATURE_INVENTORY section for `[ ]` unchecked features (not yet implemented) and existing `[x]` features (already done).
3. Check PLANNED_FEATURES section for features already approved but not yet built.
4. Check KNOWN_ISSUES section for bugs that could be fixed as features.

## Feature Suggestion Prompt Flow

### Step 1: Present Feature via AskUserQuestion
Use the AskUserQuestion tool with exactly this structure:
- **question**: "Feature Suggestion #N: <Feature Name> -- <2-3 sentence description covering: what it does, how users interact with it, how it integrates with existing systems>. <One sentence about technical approach using existing plugin patterns>."
- **header**: "Feature #N"
- **options** (exactly 4, always in this order):
  1. label: "Accept this feature", description: "I want to implement the <short feature name> with <key details>"
  2. label: "Skip, suggest next", description: "Not interested in <feature>, show me the next suggestion"
  3. label: "Modify this idea", description: "I like parts of it but want to tweak the scope or details"
  4. label: "Stop suggesting", description: "I'm done reviewing feature suggestions for now"
- **multiSelect**: false

### Step 2: Handle Response

#### If "Accept this feature":
1. Update `.data/dev/vector.md` PLANNED_FEATURES section with feature entry (Status: IMPLEMENTING)
2. Create a TaskCreate with subject, description, and activeForm
3. Set task to in_progress
4. Implement the feature (see Implementation Flow below)
5. Compile with `powershell.exe -Command "cd 'C:\Users\Rylen\IdeaProjects\AwesomeChat'; .\gradlew.bat compileJava 2>&1"`
6. Commit files individually using `git add <file1> <file2>...` (never `git add .` or `git add -A`)
7. Commit with conventional format: `feat(<scope>): <description>` -- NEVER add Co-Authored-By or make yourself an author
8. Update vector.md: change status to COMPLETE, update FEATURE_INVENTORY checkbox, add RESEARCH_LOG entry
9. Commit vector.md update separately: `docs(vector): update vector doc with <feature> completion`
10. Mark task as completed
11. Present next feature suggestion (go back to Step 1)

#### If "Skip, suggest next":
1. Go to next feature in priority list
2. Present it (go to Step 1)

#### If "Modify this idea":
1. Ask follow-up question via AskUserQuestion with 2-4 modification options based on the feature
2. Use multiSelect: true if modifications are not mutually exclusive
3. Incorporate feedback
4. Proceed to implementation (same as "Accept" flow)

#### If "Stop suggesting":
1. Stop the loop
2. Summarize what was implemented in the session

## Implementation Flow (for each feature)

### Files to Create (typical pattern):
1. `managers/<Feature>Manager.java` -- state management, business logic
2. `commands/<Feature>Command.java` -- CommandExecutor implementation
3. `commands/<Feature>TabCompleter.java` -- TabCompleter implementation

### Files to Modify:
1. `AwesomeChat.java` -- add manager field, init in onEnable, add getter, register command
2. `plugin.yml` -- add command entry with description/usage/aliases/permission + add permission entries
3. `config.yml` -- add configuration section for the feature
4. Integration files (ChatListener.java, MessageCommand.java, etc.) -- hook into existing event flow

### Commit Strategy:
- Stage files individually by path: `git add <path1> <path2> ...`
- Use conventional commit format: `type(scope): description`
- Types: feat, fix, chore, docs, refactor
- NEVER include Co-Authored-By
- NEVER use `git add .` or `git add -A`
- Commit feature code and vector doc update as separate commits

### Build Verification:
- Always run `powershell.exe -Command "cd 'C:\Users\Rylen\IdeaProjects\AwesomeChat'; .\gradlew.bat compileJava 2>&1"` after implementation
- Pre-existing warnings (deprecation, unchecked) are OK
- Actual errors must be fixed before committing

## Feature Priority List (suggest in this order)
Priority is based on: user value, integration with existing systems, complementing recent features.

1. Mention/Ping System (@player, @role, @everyone/@here) -- APPROVED, not yet implemented
2. Staff Chat Channel -- DONE (merged into chat channels system)
3. Ignore Player System -- DONE
4. Chat Clear Command (/clearchat)
5. Join/Leave/MOTD Messages (configurable, per-group)
6. Chat Radius / Local Chat (distance-based messaging)
7. Item Show in Chat ([item] placeholder shows held item)
8. Word Replacement / Censor Mode (replace bad words instead of blocking)
9. Emoji / Symbol Shortcuts (:heart: -> heart symbol)
10. Tab List Formatting (per-group tab header/footer)
11. Death Message Customization (per-group, per-cause)
12. AFK Detection / Status (auto-AFK, /afk command)
13. Chat Logging to Database (MySQL/SQLite option)
14. BungeeCord/Velocity Cross-Server Messaging
15. Chat Games / Interactivity (math quiz, trivia in chat)

## Description Templates for Common Feature Types

### Command Feature:
"Feature Suggestion #N: <Name> -- <What the command does>. <Usage syntax>. <How it integrates with existing systems like permissions, config, LuckPerms groups>."

### System Feature:
"Feature Suggestion #N: <Name> -- <What the system does automatically>. <What triggers it>. <Configuration options>. Uses existing <Manager/Listener> patterns."

### Integration Feature:
"Feature Suggestion #N: <Name> -- <What it connects to>. <How it hooks into existing chat flow>. <Dependencies and config options>."
