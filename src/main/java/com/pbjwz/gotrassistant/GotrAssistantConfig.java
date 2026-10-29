package com.pbjwz.gotrassistant;

import com.pbjwz.gotrassistant.model.CombinationRune;
import java.awt.Color;
import javax.annotation.Nullable;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Notification;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(GotrAssistantConfig.GROUP)
public interface GotrAssistantConfig extends Config
{
	String GROUP = "gotrassistant";

	enum AltarStrategy
	{
		BALANCED,
		MOST_ENERGY,
		HIGHEST_CELL_TIER
	}

	enum CombinationChoice
	{
		ANY,
		MIST,
		DUST,
		MUD,
		SMOKE,
		STEAM,
		LAVA;

		@Nullable
		CombinationRune rune()
		{
			return this == ANY ? null : CombinationRune.valueOf(name());
		}
	}

	enum GuardianColorMode
	{
		CELL_TIER,
		ALIGNMENT
	}

	@ConfigSection(name = "Step guidance", description = "What to do next, with arrows", position = 0)
	String guidanceSection = "guidance";

	@ConfigSection(name = "Highlights", description = "Objects and NPCs to highlight", position = 1)
	String highlightSection = "highlights";

	@ConfigSection(name = "Info panel", description = "Points, power and status lines", position = 2)
	String infoSection = "info";

	@ConfigSection(name = "Timers", description = "Game, portal and guardian timers", position = 3)
	String timerSection = "timers";

	@ConfigSection(name = "Essence pouches", description = "Pouch contents and wear", position = 4)
	String pouchSection = "pouches";

	@ConfigSection(name = "Notifications", description = "Alerts", position = 5, closedByDefault = true)
	String notificationSection = "notifications";

	// --- Step guidance ---

	@ConfigItem(keyName = "showStep", name = "Show next step", description = "Show what to do next in the info panel", position = 0, section = guidanceSection)
	default boolean showStep()
	{
		return true;
	}

	@ConfigItem(keyName = "showArrow", name = "Arrow above target", description = "Draw a bouncing arrow above the object of the next step", position = 1, section = guidanceSection)
	default boolean showArrow()
	{
		return true;
	}

	@ConfigItem(keyName = "useHintArrow", name = "Use game hint arrow", description = "Also point the game's own hint arrow (and minimap arrow) at the target", position = 2, section = guidanceSection)
	default boolean useHintArrow()
	{
		return false;
	}

	@ConfigItem(keyName = "altarStrategy", name = "Altar choice", description = "Balanced: even out elemental/catalytic energy. Most energy: prefer 3-energy combination runes. Highest cell tier: prefer the altar with the best cell.", position = 3, section = guidanceSection)
	default AltarStrategy altarStrategy()
	{
		return AltarStrategy.BALANCED;
	}

	@ConfigItem(keyName = "craftCombinationRunes", name = "Craft combination runes", description = "Suggest combination runes at elemental altars when you carry the other element's runes, its talisman (or Magic Imbue) and wear a binding necklace", position = 4, section = guidanceSection)
	default boolean craftCombinationRunes()
	{
		return false;
	}

	@ConfigItem(keyName = "combinationChoice", name = "Combination rune", description = "Which combination rune to make. Any: whatever your carried runes allow at the active elemental guardian", position = 5, section = guidanceSection)
	default CombinationChoice combinationChoice()
	{
		return CombinationChoice.ANY;
	}

	@ConfigItem(keyName = "highlightCombinationItems", name = "Highlight combination items", description = "Highlight the runes, talisman and binding necklace needed for the chosen combination rune in your inventory", position = 6, section = guidanceSection)
	default boolean highlightCombinationItems()
	{
		return true;
	}

	@Range(max = 200)
	@ConfigItem(keyName = "fragmentTarget", name = "Fragments to mine", description = "Fragments to mine before crafting essence. 0 = enough to fill your inventory and pouches", position = 7, section = guidanceSection)
	default int fragmentTarget()
	{
		return 0;
	}

	@ConfigItem(keyName = "usePortal", name = "Suggest the portal", description = "Suggest the portal to the huge guardian remains while it is open", position = 8, section = guidanceSection)
	default boolean usePortal()
	{
		return true;
	}

	@ConfigItem(keyName = "buildGuardians", name = "Suggest building guardians", description = "Suggest building a guardian at an essence pile when you have a chisel and a charged cell", position = 9, section = guidanceSection)
	default boolean buildGuardians()
	{
		return true;
	}

	@ConfigItem(keyName = "placeCells", name = "Suggest placing cells", description = "Suggest placing charged cells on the cell tiles", position = 10, section = guidanceSection)
	default boolean placeCells()
	{
		return true;
	}

	@ConfigItem(keyName = "depositRunes", name = "Suggest depositing runes", description = "Suggest depositing crafted runes in the deposit pool", position = 11, section = guidanceSection)
	default boolean depositRunes()
	{
		return true;
	}

	@ConfigItem(keyName = "unchargedCellReminder", name = "Uncharged cell reminder", description = "Remind you to take an uncharged cell before entering an altar", position = 12, section = guidanceSection)
	default boolean unchargedCellReminder()
	{
		return true;
	}

	// --- Highlights ---

	@ConfigItem(keyName = "highlightGuardians", name = "Active guardians", description = "Highlight the active Portal Guardians", position = 0, section = highlightSection)
	default boolean highlightGuardians()
	{
		return true;
	}

	@ConfigItem(keyName = "guardianColorMode", name = "Guardian colour", description = "Colour active guardians by cell tier or by alignment", position = 1, section = highlightSection)
	default GuardianColorMode guardianColorMode()
	{
		return GuardianColorMode.CELL_TIER;
	}

	@ConfigItem(keyName = "guardianLabels", name = "Guardian labels", description = "Show name, cell tier and time left above active guardians", position = 2, section = highlightSection)
	default boolean guardianLabels()
	{
		return true;
	}

	@ConfigItem(keyName = "highlightLocked", name = "Mark locked guardians", description = "Mark active guardians you cannot enter (level or quest) in red", position = 3, section = highlightSection)
	default boolean highlightLocked()
	{
		return true;
	}

	@ConfigItem(keyName = "highlightGreatGuardian", name = "Great Guardian", description = "Highlight the Great Guardian while you carry guardian stones", position = 4, section = highlightSection)
	default boolean highlightGreatGuardian()
	{
		return true;
	}

	@ConfigItem(keyName = "highlightEssencePiles", name = "Essence piles", description = "Highlight the essence piles when you can build a guardian", position = 5, section = highlightSection)
	default boolean highlightEssencePiles()
	{
		return true;
	}

	@ConfigItem(keyName = "highlightCellTiles", name = "Cell tiles", description = "Highlight cell tiles while you carry a charged cell", position = 6, section = highlightSection)
	default boolean highlightCellTiles()
	{
		return true;
	}

	@ConfigItem(keyName = "highlightPortal", name = "Portal", description = "Highlight the portal to the huge guardian remains", position = 7, section = highlightSection)
	default boolean highlightPortal()
	{
		return true;
	}

	@ConfigItem(keyName = "highlightDepositPool", name = "Deposit pool", description = "Highlight the deposit pool while you carry runes", position = 8, section = highlightSection)
	default boolean highlightDepositPool()
	{
		return true;
	}

	@ConfigItem(keyName = "highlightWorkbench", name = "Workbench", description = "Highlight the workbench when you should craft essence", position = 9, section = highlightSection)
	default boolean highlightWorkbench()
	{
		return true;
	}

	@Range(min = 1, max = 6)
	@ConfigItem(keyName = "outlineWidth", name = "Outline width", description = "Width of the highlight outlines", position = 10, section = highlightSection)
	default int outlineWidth()
	{
		return 2;
	}

	@Alpha
	@ConfigItem(keyName = "elementalColor", name = "Elemental colour", description = "Colour for elemental highlights", position = 11, section = highlightSection)
	default Color elementalColor()
	{
		return new Color(255, 90, 60);
	}

	@Alpha
	@ConfigItem(keyName = "catalyticColor", name = "Catalytic colour", description = "Colour for catalytic highlights", position = 12, section = highlightSection)
	default Color catalyticColor()
	{
		return new Color(190, 110, 255);
	}

	@Alpha
	@ConfigItem(keyName = "targetColor", name = "Target colour", description = "Colour for the arrow and the object of the next step", position = 13, section = highlightSection)
	default Color targetColor()
	{
		return new Color(255, 230, 0);
	}

	@Alpha
	@ConfigItem(keyName = "lockedColor", name = "Locked colour", description = "Colour for guardians you cannot enter", position = 14, section = highlightSection)
	default Color lockedColor()
	{
		return new Color(200, 30, 30, 160);
	}

	// --- Info panel ---

	@ConfigItem(keyName = "showInfoPanel", name = "Show info panel", description = "Show the GOTR info panel", position = 0, section = infoSection)
	default boolean showInfoPanel()
	{
		return true;
	}

	@ConfigItem(keyName = "showEnergy", name = "Energy this game", description = "Elemental/catalytic energy this game and the 300 energy XP threshold", position = 1, section = infoSection)
	default boolean showEnergy()
	{
		return true;
	}

	@ConfigItem(keyName = "showPotential", name = "Potential points", description = "Points you will have once your stones and essence are handed in", position = 2, section = infoSection)
	default boolean showPotential()
	{
		return true;
	}

	@ConfigItem(keyName = "showTotals", name = "Total reward points", description = "Banked reward points, searches and this session's points", position = 3, section = infoSection)
	default boolean showTotals()
	{
		return true;
	}

	@ConfigItem(keyName = "showPower", name = "Guardian power", description = "Great Guardian power and guardians built", position = 4, section = infoSection)
	default boolean showPower()
	{
		return true;
	}

	@ConfigItem(keyName = "showCombination", name = "Combination runes", description = "Show whether combination runes can be crafted now", position = 5, section = infoSection)
	default boolean showCombination()
	{
		return true;
	}

	@ConfigItem(keyName = "showSpec", name = "Pickaxe special", description = "Show special attack energy when you have a dragon (or similar) pickaxe", position = 6, section = infoSection)
	default boolean showSpec()
	{
		return true;
	}

	@ConfigItem(keyName = "showEssence", name = "Essence carried", description = "Essence in your inventory and pouches against what fits", position = 7, section = infoSection)
	default boolean showEssence()
	{
		return true;
	}

	@ConfigItem(keyName = "showSessionRate", name = "Session rate", description = "Games per hour and average points per game this session", position = 8, section = infoSection)
	default boolean showSessionRate()
	{
		return true;
	}

	@ConfigItem(keyName = "gameSummary", name = "Game summary in chat", description = "Post a short summary (points, energy, time) in your chatbox after each game. Only you can see it", position = 9, section = infoSection)
	default boolean gameSummary()
	{
		return true;
	}

	// --- Timers ---

	@ConfigItem(keyName = "showGameTimer", name = "Game timer", description = "Countdown to the start and time since the rift opened", position = 0, section = timerSection)
	default boolean showGameTimer()
	{
		return true;
	}

	@ConfigItem(keyName = "showPortalTimer", name = "Portal timer", description = "Time the portal stays open, or estimate until the next one", position = 1, section = timerSection)
	default boolean showPortalTimer()
	{
		return true;
	}

	@ConfigItem(keyName = "showGuardianTimer", name = "Guardian rotation", description = "Time until the active guardians change", position = 2, section = timerSection)
	default boolean showGuardianTimer()
	{
		return true;
	}

	@Units(Units.SECONDS)
	@Range(min = 60, max = 300)
	@ConfigItem(keyName = "firstPortalSeconds", name = "First portal after", description = "Estimated seconds from the rift opening to the first portal", position = 3, section = timerSection)
	default int firstPortalSeconds()
	{
		return 160;
	}

	@Units(Units.SECONDS)
	@Range(min = 60, max = 300)
	@ConfigItem(keyName = "portalIntervalSeconds", name = "Portal interval", description = "Estimated seconds between portals", position = 4, section = timerSection)
	default int portalIntervalSeconds()
	{
		return 140;
	}

	// --- Pouches ---

	@ConfigItem(keyName = "showPouches", name = "Pouch status", description = "Show contents and wear of your essence pouches in the info panel", position = 0, section = pouchSection)
	default boolean showPouches()
	{
		return true;
	}

	@ConfigItem(keyName = "pouchItemOverlay", name = "Count on pouch icons", description = "Draw stored/capacity on the pouch items in your inventory", position = 1, section = pouchSection)
	default boolean pouchItemOverlay()
	{
		return true;
	}

	@Range(min = 1, max = 30)
	@ConfigItem(keyName = "fillsPerGame", name = "Fills per game", description = "Fills per pouch in a game, used until the plugin has measured your own games", position = 2, section = pouchSection)
	default int fillsPerGame()
	{
		return 7;
	}

	@Range(max = 10)
	@ConfigItem(keyName = "repairWarningGames", name = "Repair warning (games)", description = "Warn and highlight Apprentice Cordelia when a pouch has this many games or fewer left", position = 3, section = pouchSection)
	default int repairWarningGames()
	{
		return 1;
	}

	@ConfigItem(keyName = "repairViaNpcContact", name = "Repair via NPC Contact", description = "On the Lunar spellbook (67 Magic), suggest NPC Contact on the Dark Mage instead of Apprentice Cordelia, and highlight the spell", position = 4, section = pouchSection)
	default boolean repairViaNpcContact()
	{
		return true;
	}

	// --- Notifications ---

	@ConfigItem(keyName = "notifyGameStart", name = "Game starting", description = "Notify when the rift is about to open", position = 0, section = notificationSection)
	default Notification notifyGameStart()
	{
		return Notification.ON;
	}

	@ConfigItem(keyName = "notifyPortal", name = "Portal opened", description = "Notify when the portal to the huge remains opens", position = 1, section = notificationSection)
	default Notification notifyPortal()
	{
		return Notification.ON;
	}

	@ConfigItem(keyName = "notifyGuardianRotation", name = "Guardians changed", description = "Notify when the active guardians change", position = 2, section = notificationSection)
	default Notification notifyGuardianRotation()
	{
		return Notification.OFF;
	}

	@ConfigItem(keyName = "notifySpec", name = "Pickaxe special ready", description = "Notify when your pickaxe special attack is at 100% during a game", position = 3, section = notificationSection)
	default Notification notifySpec()
	{
		return Notification.OFF;
	}

	@ConfigItem(keyName = "notifyPouchRepair", name = "Pouch needs repair", description = "Notify when entering the temple with pouches that need repairing", position = 4, section = notificationSection)
	default Notification notifyPouchRepair()
	{
		return Notification.ON;
	}
}
