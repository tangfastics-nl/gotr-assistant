package com.pbjwz.gotrassistant.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import org.junit.Test;

public class EssencePouchTest
{
	@Test
	public void giantPouchDegradesAtWikiThresholds()
	{
		EssencePouch giant = EssencePouch.GIANT;
		assertEquals(12, giant.capacity(99, giant.stage(0)));
		assertEquals(12, giant.capacity(99, giant.stage(199)));
		assertEquals(9, giant.capacity(99, giant.stage(200)));
		assertEquals(3, giant.capacity(99, giant.stage(1199)));
		assertEquals(0, giant.capacity(99, giant.stage(1200)));
		assertEquals(80, giant.essenceUntilDegrade(120));
		assertEquals(0, giant.essenceUntilDegrade(250));
	}

	@Test
	public void colossalScalesWithLevel()
	{
		EssencePouch colossal = EssencePouch.COLOSSAL;
		assertEquals(40, colossal.capacity(85, 0));
		assertEquals(27, colossal.capacity(80, 0));
		assertEquals(16, colossal.capacity(60, 0));
		assertEquals(8, colossal.capacity(30, 0));
		assertEquals(0, colossal.capacity(10, 0));
	}

	@Test
	public void smallPouchNeverDegrades()
	{
		assertFalse(EssencePouch.SMALL.degrades());
		assertEquals(3, EssencePouch.SMALL.capacity(1, EssencePouch.SMALL.stage(5000)));
		assertEquals(0, EssencePouch.SMALL.essenceUntilDegrade(0));
	}

	@Test
	public void combinationRunes()
	{
		assertSame(CombinationRune.LAVA, CombinationRune.of(Guardian.FIRE, Guardian.EARTH));
		assertSame(CombinationRune.LAVA, CombinationRune.of(Guardian.EARTH, Guardian.FIRE));
		assertSame(CombinationRune.STEAM, CombinationRune.of(Guardian.FIRE, Guardian.WATER));
		assertNull(CombinationRune.of(Guardian.FIRE, Guardian.FIRE));
		assertNull(CombinationRune.of(Guardian.NATURE, Guardian.FIRE));
	}

	@Test
	public void hudIndices()
	{
		assertSame(Guardian.FIRE, Guardian.fromHud(Alignment.ELEMENTAL, 4));
		assertSame(Guardian.BLOOD, Guardian.fromHud(Alignment.CATALYTIC, 8));
		assertNull(Guardian.fromHud(Alignment.ELEMENTAL, 5));
	}
}
