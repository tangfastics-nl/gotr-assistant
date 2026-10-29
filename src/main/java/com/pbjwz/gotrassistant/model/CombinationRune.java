package com.pbjwz.gotrassistant.model;

import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.gameval.ItemID;

@Getter
@AllArgsConstructor
public enum CombinationRune
{
	MIST("Mist", 6, Guardian.AIR, Guardian.WATER, ItemID.MISTRUNE),
	DUST("Dust", 10, Guardian.AIR, Guardian.EARTH, ItemID.DUSTRUNE),
	MUD("Mud", 13, Guardian.WATER, Guardian.EARTH, ItemID.MUDRUNE),
	SMOKE("Smoke", 15, Guardian.AIR, Guardian.FIRE, ItemID.SMOKERUNE),
	STEAM("Steam", 19, Guardian.WATER, Guardian.FIRE, ItemID.STEAMRUNE),
	LAVA("Lava", 23, Guardian.EARTH, Guardian.FIRE, ItemID.LAVARUNE);

	private final String label;
	private final int levelRequired;
	private final Guardian first;
	private final Guardian second;
	private final int runeItemId;

	/**
	 * The combination rune made at {@code altar} while carrying runes of {@code carried}.
	 */
	@Nullable
	public static CombinationRune of(Guardian altar, Guardian carried)
	{
		for (CombinationRune rune : values())
		{
			if ((rune.first == altar && rune.second == carried) || (rune.second == altar && rune.first == carried))
			{
				return rune;
			}
		}
		return null;
	}

	@Nullable
	public static CombinationRune fromRune(int itemId)
	{
		for (CombinationRune rune : values())
		{
			if (rune.runeItemId == itemId)
			{
				return rune;
			}
		}
		return null;
	}
}
