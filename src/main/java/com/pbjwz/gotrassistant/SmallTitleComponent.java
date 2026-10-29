package com.pbjwz.gotrassistant;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * A centred title line drawn in the small RuneScape font.
 */
class SmallTitleComponent implements LayoutableRenderableEntity
{
	private final TitleComponent title;

	SmallTitleComponent(String text, Color color)
	{
		title = TitleComponent.builder().text(text).color(color).build();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Font previous = graphics.getFont();
		graphics.setFont(FontManager.getRunescapeSmallFont());
		Dimension size = title.render(graphics);
		graphics.setFont(previous);
		return size;
	}

	@Override
	public Rectangle getBounds()
	{
		return title.getBounds();
	}

	@Override
	public void setPreferredLocation(Point position)
	{
		title.setPreferredLocation(position);
	}

	@Override
	public void setPreferredSize(Dimension dimension)
	{
		title.setPreferredSize(dimension);
	}
}
