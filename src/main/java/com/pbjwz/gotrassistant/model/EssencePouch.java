package com.pbjwz.gotrassistant.model;

import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/**
 * Essence pouches and their degradation. The degradation counter counts essence put in the pouch;
 * capacity drops at each threshold (OSRS wiki, "Essence pouch#Degrading").
 */
@Getter
@AllArgsConstructor
public enum EssencePouch
{
	SMALL("Small", VarbitID.SMALL_ESSENCE_POUCH, ItemID.RCU_POUCH_SMALL, -1, 1, -1, -1,
		new int[]{3}, new int[0]),
	MEDIUM("Medium", VarbitID.MEDIUM_ESSENCE_POUCH, ItemID.RCU_POUCH_MEDIUM, ItemID.RCU_POUCH_MEDIUM_DEGRADE, 25,
		VarPlayerID.RCU_POUCH_DEGRADATION_MED, -1,
		new int[]{6, 3}, new int[]{400, 800}),
	LARGE("Large", VarbitID.LARGE_ESSENCE_POUCH, ItemID.RCU_POUCH_LARGE, ItemID.RCU_POUCH_LARGE_DEGRADE, 50,
		VarPlayerID.RCU_POUCH_DEGRADATION_LARGE, -1,
		new int[]{9, 7, 5, 3}, new int[]{400, 600, 800, 1000}),
	GIANT("Giant", VarbitID.GIANT_ESSENCE_POUCH, ItemID.RCU_POUCH_GIANT, ItemID.RCU_POUCH_GIANT_DEGRADE, 75,
		VarPlayerID.RCU_POUCH_DEGRADATION_GIANT, -1,
		new int[]{12, 9, 8, 7, 6, 5, 3}, new int[]{200, 300, 400, 600, 800, 1000, 1200}),
	COLOSSAL("Colossal", VarbitID.COLOSSAL_ESSENCE_POUCH, ItemID.RCU_POUCH_COLOSSAL, ItemID.RCU_POUCH_COLOSSAL_DEGRADE, 25,
		-1, VarbitID.RCU_POUCH_DEGRADATION_COLOSSAL,
		new int[]{40, 35, 30, 25, 20, 15, 10, 5}, new int[]{320, 565, 745, 870, 950, 995, 1015, 1020});

	private final String label;
	private final int storedVarbit;
	private final int itemId;
	private final int degradedItemId;
	private final int levelRequired;
	private final int degradationVarp;
	private final int degradationVarbit;
	private final int[] stageCapacities;
	/**
	 * Counter values at which the pouch degrades one more step; the last one destroys it.
	 */
	private final int[] stageThresholds;

	public boolean degrades()
	{
		return stageThresholds.length > 0;
	}

	public int stage(int counter)
	{
		int stage = 0;
		while (stage < stageThresholds.length && counter >= stageThresholds[stage])
		{
			stage++;
		}
		return stage;
	}

	/**
	 * Essence the pouch holds at a stage. The colossal pouch scales with Runecraft level.
	 */
	public int capacity(int runecraftLevel, int stage)
	{
		if (runecraftLevel < levelRequired)
		{
			return 0;
		}
		if (stage >= stageCapacities.length)
		{
			return 0;
		}
		int at = stageCapacities[stage];
		if (this != COLOSSAL)
		{
			return at;
		}
		int full;
		if (runecraftLevel >= 85)
		{
			full = 40;
		}
		else if (runecraftLevel >= 75)
		{
			full = 27;
		}
		else if (runecraftLevel >= 50)
		{
			full = 16;
		}
		else
		{
			full = 8;
		}
		return Math.max(1, at * full / stageCapacities[0]);
	}

	/**
	 * Essence that can still go in before the pouch first degrades (shrinks), or 0 if it already has.
	 */
	public int essenceUntilDegrade(int counter)
	{
		if (!degrades() || counter >= stageThresholds[0])
		{
			return 0;
		}
		return stageThresholds[0] - counter;
	}

	@Nullable
	public static EssencePouch fromItem(int itemId)
	{
		for (EssencePouch pouch : values())
		{
			if (pouch.itemId == itemId || pouch.degradedItemId == itemId)
			{
				return pouch;
			}
		}
		return null;
	}
}
