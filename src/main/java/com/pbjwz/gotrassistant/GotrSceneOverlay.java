package com.pbjwz.gotrassistant;

import com.pbjwz.gotrassistant.model.Alignment;
import com.pbjwz.gotrassistant.model.Guardian;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

class GotrSceneOverlay extends Overlay
{
	private static final int FEATHER = 4;
	private static final int OBJECT_ARROW_HEIGHT = 280;
	private static final int NPC_ARROW_EXTRA = 60;

	private final Client client;
	private final GotrAssistantPlugin plugin;
	private final GotrAssistantConfig config;
	private final ModelOutlineRenderer outlines;

	@Inject
	GotrSceneOverlay(Client client, GotrAssistantPlugin plugin, GotrAssistantConfig config, ModelOutlineRenderer outlines)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.outlines = outlines;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isVisible())
		{
			return null;
		}
		int width = config.outlineWidth();

		if (config.highlightGuardians() && plugin.isGameRunning())
		{
			renderGuardians(graphics, width);
		}
		if (config.highlightGreatGuardian() && plugin.hasStones() && plugin.getGreatGuardian() != null)
		{
			outlines.drawOutline(plugin.getGreatGuardian(), width, config.elementalColor(), FEATHER);
		}
		if (config.highlightEssencePiles() && plugin.canBuildGuardian())
		{
			outline(graphics, plugin.getElementalPile(), width, config.elementalColor());
			outline(graphics, plugin.getCatalyticPile(), width, config.catalyticColor());
		}
		if (config.highlightCellTiles() && plugin.getChargedCell() != null && plugin.isGameRunning())
		{
			Color color = plugin.getChargedCell().getColor();
			for (TileObject tile : plugin.getCellTiles())
			{
				Polygon poly = tile.getCanvasTilePoly();
				if (poly != null)
				{
					OverlayUtil.renderPolygon(graphics, poly, color);
				}
			}
		}
		if (config.highlightPortal() && plugin.isPortalOpen())
		{
			outline(graphics, plugin.getPortal(), width, config.targetColor());
		}
		if (config.highlightDepositPool() && plugin.hasDepositableRunes())
		{
			outline(graphics, plugin.getDepositPool(), width, config.catalyticColor());
		}
		if (config.highlightWorkbench() && plugin.getFragments() > 0 && plugin.isGameRunning())
		{
			outline(graphics, plugin.getWorkbench(), width, config.elementalColor());
		}
		if (plugin.showUnchargedCellTable())
		{
			outline(graphics, plugin.getUnchargedCellTable(), width, Color.WHITE);
		}
		if (plugin.pouchRepairNeeded() && plugin.getCordelia() != null && !plugin.isGameRunning() && !plugin.repairViaNpcContact())
		{
			outlines.drawOutline(plugin.getCordelia(), width, Color.ORANGE, FEATHER);
		}

		Step step = plugin.getStep();
		if (step != null)
		{
			renderTarget(graphics, step, width);
		}
		return null;
	}

	private void renderGuardians(Graphics2D graphics, int width)
	{
		int rotation = plugin.secondsToRotation();
		for (Map.Entry<Guardian, GameObject> entry : plugin.getPortalGuardians().entrySet())
		{
			Guardian guardian = entry.getKey();
			if (!plugin.activeGuardians().contains(guardian))
			{
				continue;
			}
			GameObject object = entry.getValue();
			boolean locked = plugin.isLocked(guardian);
			Color color;
			if (locked && config.highlightLocked())
			{
				color = config.lockedColor();
			}
			else if (config.guardianColorMode() == GotrAssistantConfig.GuardianColorMode.ALIGNMENT)
			{
				color = guardian.getAlignment() == Alignment.ELEMENTAL ? config.elementalColor() : config.catalyticColor();
			}
			else
			{
				color = guardian.getCellTier().getColor();
			}
			outlines.drawOutline(object, width, color, FEATHER);

			if (config.guardianLabels())
			{
				String text = guardian.getLabel() + " - " + guardian.getCellTier().getLabel()
					+ (rotation >= 0 ? " " + String.format(Locale.ROOT, "%d:%02d", rotation / 60, rotation % 60) : "")
					+ (locked ? " (locked)" : "")
					+ (guardian == plugin.getActiveElemental() && plugin.getActiveCombination() != null
						? " + " + plugin.getActiveCombination().getLabel() : "");
				Point at = object.getCanvasTextLocation(graphics, text, 200);
				if (at != null)
				{
					OverlayUtil.renderTextLocation(graphics, at, text, color);
				}
			}
		}
	}

	private void renderTarget(Graphics2D graphics, Step step, int width)
	{
		Color color = config.targetColor();
		TileObject object = step.getObject();
		NPC npc = step.getNpc();
		Point anchor = null;
		if (object != null)
		{
			outline(graphics, object, width + 1, color);
			LocalPoint lp = object.getLocalLocation();
			anchor = Perspective.localToCanvas(client, lp, object.getPlane(), OBJECT_ARROW_HEIGHT);
		}
		else if (npc != null)
		{
			outlines.drawOutline(npc, width + 1, color, FEATHER);
			anchor = Perspective.localToCanvas(client, npc.getLocalLocation(), npc.getWorldLocation().getPlane(), npc.getLogicalHeight() + NPC_ARROW_EXTRA);
		}
		if (config.showArrow() && anchor != null)
		{
			drawArrow(graphics, anchor, color);
		}
	}

	private void outline(Graphics2D graphics, @Nullable TileObject object, int width, Color color)
	{
		if (object == null)
		{
			return;
		}
		if (object instanceof GameObject)
		{
			outlines.drawOutline((GameObject) object, width, color, FEATHER);
			return;
		}
		// Ground objects have no model outline here; draw their clickbox instead.
		Shape hull = object.getClickbox();
		if (hull != null)
		{
			OverlayUtil.renderPolygon(graphics, hull, color);
		}
	}

	private static void drawArrow(Graphics2D graphics, Point anchor, Color color)
	{
		int bounce = (int) Math.round(Math.sin(System.currentTimeMillis() / 160.0) * 6);
		int x = anchor.getX();
		int y = anchor.getY() + bounce;
		Polygon head = new Polygon(new int[]{x - 14, x + 14, x}, new int[]{y - 8, y - 8, y + 10}, 3);
		graphics.setStroke(new BasicStroke(2));
		graphics.setColor(Color.BLACK);
		graphics.drawPolygon(head);
		graphics.drawRect(x - 5, y - 26, 10, 19);
		graphics.setColor(color);
		graphics.fillPolygon(head);
		graphics.fillRect(x - 5, y - 26, 10, 19);
	}
}
