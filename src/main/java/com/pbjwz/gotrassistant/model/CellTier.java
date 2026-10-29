package com.pbjwz.gotrassistant.model;

import java.awt.Color;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.gameval.ItemID;

/**
 * Cell tier given by an altar. Energy values are from the OSRS wiki (placing / creating a guardian).
 */
@Getter
@AllArgsConstructor
public enum CellTier
{
	WEAK("Weak", ItemID.GOTR_CELL_TIER1, new Color(190, 190, 190), 17, 6),
	MEDIUM("Medium", ItemID.GOTR_CELL_TIER2, new Color(80, 170, 255), 20, 15),
	STRONG("Strong", ItemID.GOTR_CELL_TIER3, new Color(80, 220, 90), 24, 27),
	OVERCHARGED("Overcharged", ItemID.GOTR_CELL_TIER4, new Color(255, 140, 40), 30, 45);

	private final String label;
	private final int cellItemId;
	private final Color color;
	private final int placeEnergy;
	private final int guardianEnergy;

	@Nullable
	public static CellTier fromCellItem(int itemId)
	{
		for (CellTier tier : values())
		{
			if (tier.cellItemId == itemId)
			{
				return tier;
			}
		}
		return null;
	}
}
