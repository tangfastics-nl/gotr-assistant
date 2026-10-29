package com.pbjwz.gotrassistant;

import com.pbjwz.gotrassistant.model.EssencePouch;
import lombok.Value;

@Value
public class PouchStatus
{
	EssencePouch pouch;
	int stored;
	int capacity;
	boolean degraded;
	/**
	 * Essence that can still be added before the pouch shrinks; 0 once degraded.
	 */
	int essenceUntilDegrade;
	/**
	 * Estimated games until a repair is needed, or -1 for pouches that never degrade.
	 */
	int gamesLeft;

	boolean needsRepair(int warningGames)
	{
		return pouch.degrades() && (degraded || gamesLeft <= warningGames);
	}
}
