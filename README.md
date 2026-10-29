# GOTR Assistant

A RuneLite plugin for the Guardians of the Rift minigame. It only shows information and highlights. It never clicks, never changes menus, and never acts for you.

## Features

**Step guidance**
- Shows the next step in the info panel: mine fragments, craft essence, fill pouches, enter a guardian, craft, leave, power up the Great Guardian, deposit runes, place a cell, build a guardian, or take the portal.
- Draws a bouncing arrow above the object of the current step, and can optionally use the game's own hint arrow.
- Picks an altar with a configurable strategy: balanced elemental/catalytic energy, most energy (combination runes), or highest cell tier. Locked altars (level or quest) are skipped.

**Combination runes**
- A toggle turns combination runes on or off. When on, the panel shows *YES/NO* and the reason. It checks for a binding necklace, runes of the other element, the talisman (or Magic Imbue) and the required level.

**Highlights**
- The active guardians, coloured by cell tier or alignment, with labels and the time until rotation. Guardians you cannot enter are shown in red.
- The Great Guardian while you hold stones.
- The essence piles when you can build a guardian (chisel + charged cell + room left).
- The cell tiles, portal, deposit pool, workbench, uncharged cell table, and Apprentice Cordelia (when a pouch needs repair).

**Points**
- Energy this game, and progress to the 300-energy XP threshold.
- Potential points once your stones and carried essence are handed in.
- Banked reward points and the number of searches, plus points earned this session.

**Timers**
- Rift countdown and game time.
- Time the portal stays open, or an estimate until the next portal.
- Time until the guardians rotate.

**Essence pouches**
- Stored/capacity for each pouch, shown in the panel and on the inventory icon.
- Wear per pouch, read from the game's degradation counter, with the estimated number of games until a repair is needed.
- Repair advice: on the Lunar spellbook with 67 Magic it suggests NPC Contact on the Dark Mage and highlights the spell. Otherwise it points to Apprentice Cordelia.

**Pickaxe special attack**
- Special attack energy for dragon, infernal, crystal, 3rd age and trailblazer pickaxes, with a "use it now" hint while mining.

**Notifications**
- Optional alerts for: game starting, portal opened, guardians changed, special attack ready, and pouches needing repair.

Every feature can be turned on or off in the plugin settings.

## Notes
- The portal estimate is not exact. The game does not expose when the next portal spawns. The first-portal time and the interval can be adjusted in the settings.
- The number of games left on a pouch is measured from your own games. Until the plugin has measured some, it uses the "Fills per game" setting.
- Banked reward points are read from the end-of-game message, or when you check them at the Rewards Guardian.
- Only runes in your inventory are counted for combination runes, not runes in a rune pouch.
