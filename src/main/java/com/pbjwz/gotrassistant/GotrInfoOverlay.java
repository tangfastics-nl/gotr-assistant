package com.pbjwz.gotrassistant;

import com.pbjwz.gotrassistant.model.Guardian;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Locale;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

class GotrInfoOverlay extends OverlayPanel
{
	private static final Color GOOD = new Color(90, 220, 90);
	private static final Color WARN = new Color(255, 170, 40);
	private static final Color BAD = new Color(240, 70, 70);
	private static final Color MUTED = new Color(170, 170, 170);

	private final GotrAssistantPlugin plugin;
	private final GotrAssistantConfig config;

	@Inject
	GotrInfoOverlay(GotrAssistantPlugin plugin, GotrAssistantConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		panelComponent.setPreferredSize(new Dimension(230, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showInfoPanel() || !plugin.isVisible())
		{
			return null;
		}

		panelComponent.getChildren().add(TitleComponent.builder().text("GOTR Assistant").color(config.targetColor()).build());
		panelComponent.getChildren().add(TitleComponent.builder().text("by POWERBASSIE").color(MUTED).build());

		Step step = plugin.getStep();
		if (config.showStep() && step != null)
		{
			panelComponent.getChildren().add(LineComponent.builder().left("> " + step.getText()).leftColor(config.targetColor()).build());
			for (String hint : plugin.getHints())
			{
				panelComponent.getChildren().add(LineComponent.builder().left("! " + hint).leftColor(WARN).build());
			}
		}

		if (plugin.isGameRunning())
		{
			guardianLine("Elemental", plugin.getActiveElemental());
			guardianLine("Catalytic", plugin.getActiveCatalytic());
		}

		if (config.showPower() && plugin.isGameRunning() && plugin.getMaxPower() > 0)
		{
			int percent = plugin.getPower() * 100 / plugin.getMaxPower();
			line("Guardian power", percent + "%", percent >= 100 ? GOOD : Color.WHITE);
			if (plugin.getGuardiansMax() > 0)
			{
				line("Guardians built", plugin.getGuardiansBuilt() + "/" + plugin.getGuardiansMax(), Color.WHITE);
			}
		}

		if (config.showEnergy() && plugin.isGameRunning())
		{
			int e = plugin.energyElemental();
			int c = plugin.energyCatalytic();
			line("Energy (E / C)", e + " / " + c, Color.WHITE);
			int total = e + c;
			line("XP threshold", Math.min(total, plugin.xpThreshold()) + "/" + plugin.xpThreshold(), total >= plugin.xpThreshold() ? GOOD : WARN);
		}

		if (config.showPotential() && plugin.isGameRunning())
		{
			line("Potential points", points(plugin.potentialElemental()) + " / " + points(plugin.potentialCatalytic()), Color.WHITE);
		}

		if (config.showTotals())
		{
			if (plugin.getBankedElemental() >= 0)
			{
				int e = plugin.getBankedElemental();
				int c = plugin.getBankedCatalytic();
				line("Reward points", e + " / " + c, Color.WHITE);
				line("Searches", String.valueOf(Math.min(e, c)), Color.WHITE);
			}
			else
			{
				line("Reward points", "check at Rewards Guardian", MUTED);
			}
			if (plugin.getGamesPlayed() > 0)
			{
				line("Session (" + plugin.getGamesPlayed() + " games)", "+" + plugin.getSessionElemental() + " / +" + plugin.getSessionCatalytic(), GOOD);
			}
		}

		if (config.showCombination() && config.craftCombinationRunes() && plugin.getCombinationStatus() != null)
		{
			String status = plugin.getCombinationStatus();
			line("Combination", status, status.startsWith("YES") ? GOOD : MUTED);
		}

		if (config.showSpec() && plugin.hasSpecPickaxe())
		{
			int spec = plugin.specPercent();
			line("Pickaxe special", spec >= 100 ? "READY" : spec + "%", spec >= 100 ? GOOD : MUTED);
		}

		timers();

		if (config.showPouches())
		{
			for (PouchStatus pouch : plugin.getPouches())
			{
				String right = pouch.getStored() + "/" + pouch.getCapacity();
				Color color = Color.WHITE;
				if (pouch.getPouch().degrades())
				{
					if (pouch.isDegraded())
					{
						right += " REPAIR";
						color = BAD;
					}
					else
					{
						right += " ~" + pouch.getGamesLeft() + " games";
						color = pouch.needsRepair(config.repairWarningGames()) ? WARN : Color.WHITE;
					}
				}
				line(pouch.getPouch().getLabel() + " pouch", right, color);
			}
		}

		return super.render(graphics);
	}

	private void timers()
	{
		if (config.showGameTimer())
		{
			int toStart = plugin.secondsToStart();
			int since = plugin.secondsSinceStart();
			if (toStart >= 0)
			{
				line("Rift opens in", time(toStart), WARN);
			}
			else if (since >= 0)
			{
				line("Game time", time(since), Color.WHITE);
			}
		}
		if (config.showPortalTimer() && plugin.isGameRunning())
		{
			int open = plugin.portalSecondsLeft();
			if (plugin.isPortalOpen())
			{
				String dir = plugin.getPortalDirection();
				line("Portal OPEN" + (dir == null || dir.isEmpty() ? "" : " (" + dir + ")"), open >= 0 ? time(open) : "", GOOD);
			}
			else
			{
				int next = plugin.secondsToNextPortal();
				if (next >= 0)
				{
					line("Next portal", "~" + time(next), Color.WHITE);
				}
				else if (next != Integer.MIN_VALUE)
				{
					line("Next portal", "any moment", WARN);
				}
			}
		}
		if (config.showGuardianTimer())
		{
			int rotation = plugin.secondsToRotation();
			if (rotation >= 0)
			{
				line("Guardians change", time(rotation), rotation <= 10 ? WARN : Color.WHITE);
			}
		}
	}

	private void guardianLine(String label, Guardian guardian)
	{
		if (guardian == null)
		{
			return;
		}
		boolean locked = plugin.isLocked(guardian);
		String right = guardian.getLabel() + " (" + guardian.getCellTier().getLabel() + ")" + (locked ? " locked" : "");
		Color color = locked ? BAD : guardian.getCellTier().getColor();
		if (guardian == plugin.getPlannedGuardian())
		{
			right = "> " + right;
		}
		line(label, right, color);
	}

	private void line(String left, String right, Color rightColor)
	{
		panelComponent.getChildren().add(LineComponent.builder().left(left).right(right).rightColor(rightColor).build());
	}

	private static String points(int energy)
	{
		return String.format(Locale.ROOT, "%.1f", energy / 100.0);
	}

	private static String time(int seconds)
	{
		return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
	}
}
