package com.pbjwz.gotrassistant;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Outlines NPC Contact in the Lunar spellbook while a pouch needs repairing.
 */
class GotrSpellOverlay extends Overlay
{
	private final Client client;
	private final GotrAssistantPlugin plugin;
	private final GotrAssistantConfig config;

	@Inject
	GotrSpellOverlay(Client client, GotrAssistantPlugin plugin, GotrAssistantConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isVisible() || !plugin.pouchRepairNeeded() || !plugin.repairViaNpcContact())
		{
			return null;
		}
		Widget spell = client.getWidget(InterfaceID.MagicSpellbook.NPC_CONTACT);
		if (spell == null || spell.isHidden())
		{
			return null;
		}
		Rectangle bounds = spell.getBounds();
		graphics.setStroke(new BasicStroke(2));
		graphics.setColor(config.targetColor());
		graphics.drawRect(bounds.x - 1, bounds.y - 1, bounds.width + 1, bounds.height + 1);
		return null;
	}
}
