package com.pbjwz.gotrassistant;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/**
 * Outlines the inventory items needed for the chosen combination rune.
 */
class GotrRuneOverlay extends WidgetItemOverlay
{
	private final GotrAssistantPlugin plugin;
	private final GotrAssistantConfig config;
	private final ItemManager itemManager;

	@Inject
	GotrRuneOverlay(GotrAssistantPlugin plugin, GotrAssistantConfig config, ItemManager itemManager)
	{
		this.plugin = plugin;
		this.config = config;
		this.itemManager = itemManager;
		showOnInventory();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		if (!plugin.isVisible() || !plugin.getCombinationItems().contains(itemId))
		{
			return;
		}
		BufferedImage outline = itemManager.getItemOutline(itemId, widgetItem.getQuantity(), config.targetColor());
		Rectangle bounds = widgetItem.getCanvasBounds();
		graphics.drawImage(outline, (int) bounds.getX(), (int) bounds.getY(), null);
	}
}
