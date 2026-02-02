<div align="center">

<h1 style="color:#00BFFF;">AwesomeChat</h1>

<h3>The all-in-one chat management plugin for Paper servers.</h3>

<img src="https://img.shields.io/badge/Paper-1.19--1.21.11%2B-blue" />
<img src="https://img.shields.io/badge/Java-21%2B-orange?logo=openjdk&logoColor=white" />
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

<h2 style="color:#FFD700;">Requirements</h2>
<ul>
<li><strong>Server:</strong> Paper 1.19 – 1.21.11+</li>
<li><strong>Java:</strong> 21+</li>
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
/clearchat (/cc)
/mutechat (/mc)
/chatlogs (/cl)
/chatcolor (/chatcolour, /ccolor)
</pre>

<hr>

<h2 style="color:#FFD700;">Support</h2>

<a href="https://discord.gg/Z4gtF25jpC"><strong>Join our Discord</strong></a><br>
Discord: <code>adf.dev</code>
