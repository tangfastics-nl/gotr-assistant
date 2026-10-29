package com.pbjwz.gotrassistant;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.components.TextComponent;
import net.runelite.api.widgets.WidgetItem;

/**
 * Draws stored/capacity on essence pouches and outlines them when the next step is to fill or empty them.
 */
class GotrPouchOverlay extends WidgetItemOverlay
{
	private final GotrAssistantPlugin plugin;
	private final GotrAssistantConfig config;

	@Inject
	GotrPouchOverlay(GotrAssistantPlugin plugin, GotrAssistantConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		showOnInventory();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		if (!plugin.isVisible())
		{
			return;
		}
		PouchStatus status = plugin.pouchStatusByItem().get(itemId);
		if (status == null)
		{
			return;
		}
		Rectangle bounds = widgetItem.getCanvasBounds();

		Step step = plugin.getStep();
		if (step != null && step.highlightsPouches())
		{
			graphics.setColor(config.targetColor());
			graphics.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
		}

		if (!config.pouchItemOverlay())
		{
			return;
		}
		Color color = status.isDegraded() ? Color.RED
			: status.needsRepair(config.repairWarningGames()) ? Color.ORANGE : Color.WHITE;
		graphics.setFont(FontManager.getRunescapeSmallFont());
		TextComponent text = new TextComponent();
		text.setText(status.getStored() + "/" + status.getCapacity());
		text.setColor(color);
		text.setPosition(new java.awt.Point(bounds.x, bounds.y + bounds.height));
		text.render(graphics);
	}
}
