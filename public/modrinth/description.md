<div align="center">

<h1 style="color:#00BFFF;">AwesomeChat</h1>

<h3>The all-in-one chat management plugin for Paper servers.</h3>

<img src="https://img.shields.io/badge/Paper-1.19--1.21.11%2B%20%7C%2026.2-blue" />
<img src="https://img.shields.io/badge/Java-21%20%7C%2025-orange?logo=openjdk&logoColor=white" />
<br>
<img src="https://img.shields.io/github/stars/HackerADF/AwesomeChat?style=flat&logo=github" />
<img src="https://img.shields.io/github/issues/HackerADF/AwesomeChat?logo=github" />
<img src="https://img.shields.io/github/license/HackerADF/AwesomeChat" />

</div>

<hr>

<h2 style="color:#FFD700;">Why AwesomeChat?</h2>

Most servers need 3–5 separate plugins to handle chat formatting, filtering, private messaging, and moderation.  
<strong>AwesomeChat</strong> replaces them all with one lightweight, well-integrated solution.

<ul>
<li><strong>Zero bloat</strong> – Every feature is toggleable</li>
<li><strong>Deep LuckPerms integration</strong> – Per-group everything</li>
<li><strong>Adventure API native</strong> – Full MiniMessage and hex color support</li>
<li><strong>Actively maintained</strong> for 1.19+</li>
</ul>

<hr>

<h2 style="color:#FFD700;">Features</h2>

<h3 style="color:#00BFFF;">Chat Formatting</h3>
<ul>
<li>Per-group chat formats via LuckPerms</li>
<li>Automatic prefix / suffix integration</li>
<li>PlaceholderAPI support</li>
<li>Hex color support (<code>&#RRGGBB</code>)</li>
<li>MiniMessage formatting (toggleable)</li>
<li>Permission-based color &amp; style gating</li>
</ul>

<h3 style="color:#00BFFF;">Chat Channels</h3>
<ul>
<li>Create unlimited custom channels (staff, admin, VIP, etc.)</li>
<li>Per-channel permission, prefix, format, and sound</li>
<li>Toggle mode or one-off messaging</li>
<li><code>/ch join staff</code> or <code>/ch staff Hello!</code></li>
</ul>

<h3 style="color:#00BFFF;">Chat Filter Engine</h3>
<ul>
<li><strong>Cooldown</strong> – Rate-limit messages</li>
<li><strong>Spam detection</strong> – Jaro-Winkler similarity algorithm</li>
<li><strong>Censor mode</strong> – Replace bad words instead of blocking</li>
<li><strong>Banned words</strong> – Wildcard patterns from <code>.txt</code> files</li>
<li><strong>Anti-advertising</strong> – TLD blocking, phrase matching, regex</li>
<li><strong>Custom regex rules</strong> – Named rules with custom patterns</li>
<li><strong>Graduated punishments</strong> – Warn → mute → ban</li>
<li><strong>Command filtering</strong> – Filters apply to commands</li>
<li><strong>Violation logging</strong> – File + per-player records</li>
</ul>

<h3 style="color:#00BFFF;">Private Messaging</h3>
<ul>
<li><code>/msg</code>, <code>/reply</code>, <code>/whisper</code>, and more</li>
<li>Custom formats and sounds</li>
<li>Message toggle (<code>/msgtoggle</code>)</li>
<li>Social spy for staff</li>
</ul>

<h3 style="color:#00BFFF;">Player Ignore</h3>
<ul>
<li><code>/ignore &lt;player&gt;</code> – Hide chat &amp; block PMs</li>
<li>Persistent across restarts</li>
<li>Staff bypass permission</li>
</ul>

<h3 style="color:#00BFFF;">Mentions</h3>
<ul>
<li><code>@player</code></li>
<li><code>@(role)</code></li>
<li><code>@everyone</code> / <code>@here</code></li>
<li>Per-type sounds, colors, and action-bar alerts</li>
<li>Permission-gated per mention type</li>
</ul>

<h3 style="color:#00BFFF;">Emoji Shortcuts</h3>
<ul>
<li><code>:heart:</code>, <code>:star:</code>, <code>:fire:</code>, etc.</li>
<li>20 built-in Unicode emojis</li>
<li>Fully configurable via <code>config.yml</code></li>
<li>Permission-gated (<code>awesomechat.emoji</code>)</li>
</ul>

<h3 style="color:#00BFFF;">Join / Leave Messages</h3>
<ul>
<li>Per-group join &amp; leave messages</li>
<li>First-join messages</li>
<li>MOTD on join</li>
<li>Configurable sounds</li>
<li>PlaceholderAPI support</li>
<li>EssentialsX vanish integration</li>
</ul>

<h3 style="color:#00BFFF;">Item Display</h3>
<ul>
<li><code>[item]</code>, <code>[inventory]</code>, <code>[enderchest]</code>, <code>[/command]</code></li>
<li><strong>Custom triggers</strong> – Define your own <code>[name]</code> triggers with PlaceholderAPI output</li>
<li>Hover tooltips &amp; inventory snapshots</li>
<li>Read-only GUI with expiry</li>
<li>Permission-gated triggers</li>
</ul>

<h3 style="color:#00BFFF;">Chat Radius / Local Chat</h3>
<ul>
<li>Distance-based chat</li>
<li>Per-group radii via LuckPerms</li>
<li>Shout prefix (<code>!</code>)</li>
<li>Optional cross-world support</li>
</ul>

<h3 style="color:#00BFFF;">Chat Logging</h3>
<ul>
<li>SQLite or MySQL storage</li>
<li><code>/chatlogs &lt;player&gt;</code></li>
<li>Clickable pagination</li>
<li>Fully async</li>
</ul>

<h3 style="color:#00BFFF;">Chat Color</h3>
<ul>
<li><code>/chatcolor</code> — GUI picker with 16 standard colors, gradient presets, and custom gradients</li>
<li>8 dual-color and 5 triple-color gradient presets (Sunset, Ocean, Royal, Ice, Aurora, etc.)</li>
<li>Custom gradients with up to 4 hex color stops</li>
<li>Style toggles (bold, italic, underline, strikethrough, obfuscated)</li>
<li>Persistent per-player storage — survives restarts</li>
<li>LuckPerms <code>chat-color</code> meta takes priority</li>
<li>Manual <code>&amp;</code> color codes still work and override</li>
</ul>

<h3 style="color:#00BFFF;">Developer API</h3>
<ul>
<li><code>AwesomeChatAPI</code></li>
<li><code>ChatFilterViolationEvent</code></li>
</ul>

<hr>

<h2 style="color:#FFD700;">What's New in 1.0.10.1</h2>

<h3 style="color:#00BFFF;">Custom Item-Display Triggers</h3>
<p>Define your own inline chat triggers under <code>item-display.custom-triggers</code>. They print whatever you want — typically PlaceholderAPI output — and support optional hover text, a permission, and a click action.</p>
<pre>
item-display:
  custom-triggers:
    # Shorthand – text only
    ping: "&amp;e[Ping: &amp;a%player_ping%ms&amp;e]"

    # Full form
    discord:
      text: "&amp;9[Discord]"
      hover: "&amp;7Click to join our server"
      permission: "awesomechat.display.custom.discord"
      click:
        action: "open_url"
        value: "https://discord.gg/Z4gtF25jpC"
</pre>
<ul>
<li>Used in chat as <code>[name]</code>, following your configured trigger prefix / suffix</li>
<li><code>{player}</code> and PlaceholderAPI placeholders resolve against the sender, so every viewer sees the same value</li>
<li>Click actions: <code>run_command</code>, <code>suggest_command</code>, <code>copy_to_clipboard</code>, <code>open_url</code></li>
<li>Optional per-trigger permission — grant them all with <code>awesomechat.display.custom.*</code></li>
<li>Names that reuse a built-in keyword or contain invalid characters are skipped with a console warning</li>
</ul>

<h3 style="color:#00BFFF;">Minecraft 26.x</h3>
<ul>
<li>The 26.x build now targets <strong>Paper 26.2</strong> (previously 26.1)</li>
<li>Shipped as a separate <code>-mc26</code> jar, built for Java 25</li>
</ul>

<h3 style="color:#00BFFF;">Fixes</h3>
<ul>
<li><strong>Private messaging was operator-only.</strong> <code>awesomechat.msg</code>, <code>awesomechat.msgtoggle</code>, <code>awesomechat.socialspy</code> and <code>awesomechat.clearchat.self</code> were never declared, so Bukkit fell back to an OP-only default and normal players got "unknown command" for <code>/msg</code>, <code>/tell</code> and <code>/pm</code>.</li>
<li><strong><code>private-messages.enabled</code> did nothing.</strong> The PM commands were registered unconditionally and kept taking priority over EssentialsX even with the feature switched off. That toggle — and <code>private-messages.socialspy.enabled</code> — are now honoured, and <code>/awesomechat reload</code> re-applies them.</li>
<li><strong>Hex colors printed literally in broadcasts.</strong> <code>&amp;#RRGGBB</code> and <code>&amp;x&amp;R&amp;R&amp;G&amp;G&amp;B&amp;B</code> now render in <code>/broadcast</code> and in auto-broadcasts.</li>
<li><strong><code>/chatcolor</code> could not page back.</strong> The gradient menu overwrote its own Previous button with glass on every page, stranding you past the first 21 gradients.</li>
<li><strong><code>/mutechat</code> leaked a raw placeholder.</strong> Its announcement rendered as <code>[AwesomeChat] {prefix}Chat has been muted by …</code>; the configured message now owns its <code>{prefix}</code> the same way <code>/clearchat</code> does.</li>
<li><strong>Permission nodes were invisible to admins.</strong> <code>awesomechat.styling.*</code>, <code>awesomechat.format.*</code> and <code>awesomechat.display.custom.*</code> are now declared, so permission plugins tab-complete them.</li>
<li><strong>The shipped config claimed the wrong version</strong>, so fresh installs re-ran migrations on their second startup.</li>
<li><strong>Corrected the <code>/chatcolor</code> permission comments</strong> in <code>config.yml</code>, which listed a node the plugin never checks and omitted the real per-color and per-gradient nodes.</li>
</ul>

<hr>

<h2 style="color:#FFD700;">Requirements</h2>
<ul>
<li><strong>Server:</strong> Paper 1.19 – 1.21.11+ — <code>AwesomeChat-&lt;version&gt;.jar</code>, Java 21+</li>
<li><strong>Server:</strong> Paper 26.2 — <code>AwesomeChat-&lt;version&gt;-mc26.jar</code>, Java 25</li>
<li><strong>Required:</strong> <a href="https://luckperms.net">LuckPerms</a></li>
<li><strong>Optional:</strong> <a href="https://www.spigotmc.org/resources/placeholderapi.6245/">PlaceholderAPI</a></li>
</ul>

<hr>

<h2 style="color:#FFD700;">Commands</h2>

<pre>
/awesomechat (/ac)
/broadcast
/msg (/tell, /w, /pm)
/reply (/r)
/msgtoggle
/socialspy (/sspy)
/channel (/ch)
/ignore (/block)
/ignorelist
/togglechatsounds
/clearchat (/cc)
/mutechat (/mc)
/chatlogs (/cl)
/chatcolor (/chatcolour, /ccolor)
</pre>

<hr>

<h2 style="color:#FFD700;">Support</h2>

<a href="https://discord.gg/Z4gtF25jpC"><strong>Join our Discord</strong></a><br>
Discord: <code>adf.dev</code>
