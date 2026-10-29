package com.pbjwz.gotrassistant.model;

import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.Quest;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;

/**
 * The twelve Portal Guardians in the Temple of the Eye and the altars behind them.
 * hudIndex is the index the GOTR HUD script uses for the active guardian of each alignment.
 */
@Getter
@AllArgsConstructor
public enum Guardian
{
	AIR("Air", Alignment.ELEMENTAL, 1, 1, CellTier.WEAK, ObjectID.GOTR_PORTAL_AIR, ObjectID.AIR_ALTAR, ObjectID.AIRTEMPLE_EXIT_PORTAL, ItemID.AIRRUNE, ItemID.AIR_TALISMAN, null),
	WATER("Water", Alignment.ELEMENTAL, 2, 5, CellTier.MEDIUM, ObjectID.GOTR_PORTAL_WATER, ObjectID.WATER_ALTAR, ObjectID.WATERTEMPLE_EXIT_PORTAL, ItemID.WATERRUNE, ItemID.WATER_TALISMAN, null),
	EARTH("Earth", Alignment.ELEMENTAL, 3, 9, CellTier.STRONG, ObjectID.GOTR_PORTAL_EARTH, ObjectID.EARTH_ALTAR, ObjectID.EARTHTEMPLE_EXIT_PORTAL, ItemID.EARTHRUNE, ItemID.EARTH_TALISMAN, null),
	FIRE("Fire", Alignment.ELEMENTAL, 4, 14, CellTier.OVERCHARGED, ObjectID.GOTR_PORTAL_FIRE, ObjectID.FIRE_ALTAR, ObjectID.FIRETEMPLE_EXIT_PORTAL, ItemID.FIRERUNE, ItemID.FIRE_TALISMAN, null),
	MIND("Mind", Alignment.CATALYTIC, 1, 2, CellTier.WEAK, ObjectID.GOTR_PORTAL_MIND, ObjectID.MIND_ALTAR, ObjectID.MINDTEMPLE_EXIT_PORTAL, ItemID.MINDRUNE, -1, null),
	BODY("Body", Alignment.CATALYTIC, 2, 20, CellTier.WEAK, ObjectID.GOTR_PORTAL_BODY, ObjectID.BODY_ALTAR, ObjectID.BODYTEMPLE_EXIT_PORTAL, ItemID.BODYRUNE, -1, null),
	COSMIC("Cosmic", Alignment.CATALYTIC, 3, 27, CellTier.MEDIUM, ObjectID.GOTR_PORTAL_COSMIC, ObjectID.COSMIC_ALTAR, ObjectID.COSMICTEMPLE_EXIT_PORTAL, ItemID.COSMICRUNE, -1, Quest.LOST_CITY),
	CHAOS("Chaos", Alignment.CATALYTIC, 4, 35, CellTier.MEDIUM, ObjectID.GOTR_PORTAL_CHAOS, ObjectID.CHAOS_ALTAR, ObjectID.CHAOSTEMPLE_EXIT_PORTAL, ItemID.CHAOSRUNE, -1, null),
	NATURE("Nature", Alignment.CATALYTIC, 5, 44, CellTier.STRONG, ObjectID.GOTR_PORTAL_NATURE, ObjectID.NATURE_ALTAR, ObjectID.NATURETEMPLE_EXIT_PORTAL, ItemID.NATURERUNE, -1, null),
	LAW("Law", Alignment.CATALYTIC, 6, 54, CellTier.STRONG, ObjectID.GOTR_PORTAL_LAW, ObjectID.LAW_ALTAR, ObjectID.LAWTEMPLE_EXIT_PORTAL, ItemID.LAWRUNE, -1, Quest.TROLL_STRONGHOLD),
	DEATH("Death", Alignment.CATALYTIC, 7, 65, CellTier.OVERCHARGED, ObjectID.GOTR_PORTAL_DEATH, ObjectID.DEATH_ALTAR, ObjectID.DEATHTEMPLE_EXIT_PORTAL, ItemID.DEATHRUNE, -1, Quest.MOURNINGS_END_PART_II),
	BLOOD("Blood", Alignment.CATALYTIC, 8, 77, CellTier.OVERCHARGED, ObjectID.GOTR_PORTAL_BLOOD, ObjectID.BLOOD_ALTAR, ObjectID.BLOODTEMPLE_EXIT_PORTAL, ItemID.BLOODRUNE, -1, Quest.SINS_OF_THE_FATHER);

	private final String label;
	private final Alignment alignment;
	private final int hudIndex;
	private final int levelRequired;
	private final CellTier cellTier;
	private final int portalObjectId;
	private final int altarObjectId;
	private final int exitPortalObjectId;
	private final int runeItemId;
	/**
	 * Regular talisman, only used for combination runes (elemental guardians).
	 */
	private final int talismanItemId;
	@Nullable
	private final Quest requiredQuest;

	@Nullable
	public static Guardian fromHud(Alignment alignment, int hudIndex)
	{
		for (Guardian guardian : values())
		{
			if (guardian.alignment == alignment && guardian.hudIndex == hudIndex)
			{
				return guardian;
			}
		}
		return null;
	}

	@Nullable
	public static Guardian fromPortalObject(int objectId)
	{
		for (Guardian guardian : values())
		{
			if (guardian.portalObjectId == objectId)
			{
				return guardian;
			}
		}
		return null;
	}

	@Nullable
	public static Guardian fromAltarObject(int objectId)
	{
		for (Guardian guardian : values())
		{
			if (guardian.altarObjectId == objectId)
			{
				return guardian;
			}
		}
		return null;
	}

	@Nullable
	public static Guardian fromExitPortal(int objectId)
	{
		for (Guardian guardian : values())
		{
			if (guardian.exitPortalObjectId == objectId)
			{
				return guardian;
			}
		}
		return null;
	}

	public static boolean isRune(int itemId)
	{
		for (Guardian guardian : values())
		{
			if (guardian.runeItemId == itemId)
			{
				return true;
			}
		}
		return CombinationRune.fromRune(itemId) != null;
	}
}
