package com.pbjwz.gotrassistant;

import javax.annotation.Nullable;
import lombok.Value;
import net.runelite.api.NPC;
import net.runelite.api.TileObject;

/**
 * One suggested action. Purely informational: the player performs every action themselves.
 */
@Value
public class Step
{
	enum Kind
	{
		WAIT,
		REPAIR_POUCHES,
		POWER_UP,
		DEPOSIT_RUNES,
		BUILD_GUARDIAN,
		PLACE_CELL,
		FILL_POUCHES,
		EMPTY_POUCHES,
		WORKBENCH,
		ENTER_GUARDIAN,
		ENTER_PORTAL,
		MINE,
		CRAFT,
		LEAVE
	}

	Kind kind;
	String text;
	@Nullable
	TileObject object;
	@Nullable
	NPC npc;

	static Step of(Kind kind, String text)
	{
		return new Step(kind, text, null, null);
	}

	static Step at(Kind kind, String text, @Nullable TileObject object)
	{
		return new Step(kind, text, object, null);
	}

	static Step at(Kind kind, String text, @Nullable NPC npc)
	{
		return new Step(kind, text, null, npc);
	}

	boolean highlightsPouches()
	{
		return kind == Kind.FILL_POUCHES || kind == Kind.EMPTY_POUCHES;
	}
}
