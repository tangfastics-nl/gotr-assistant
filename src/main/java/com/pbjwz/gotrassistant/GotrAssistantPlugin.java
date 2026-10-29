package com.pbjwz.gotrassistant;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import com.pbjwz.gotrassistant.model.Alignment;
import com.pbjwz.gotrassistant.model.CellTier;
import com.pbjwz.gotrassistant.model.CombinationRune;
import com.pbjwz.gotrassistant.model.EssencePouch;
import com.pbjwz.gotrassistant.model.Guardian;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Animation;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.DynamicObject;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.QuestState;
import net.runelite.api.Renderable;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "GOTR aio",
	description = "Guardians of the Rift: step guidance, guardian highlights, points, timers, pouch wear and more",
	tags = {"gotr", "guardians", "rift", "runecraft", "minigame", "pouch", "portal"}
)
public class GotrAssistantPlugin extends Plugin
{
	enum Location
	{
		OUTSIDE,
		TEMPLE,
		HUGE_REMAINS,
		ALTAR
	}

	enum Phase
	{
		IDLE,
		COUNTDOWN,
		ACTIVE,
		ENDING
	}

	static final double TICK_SECONDS = 0.6;
	private static final int TEMPLE_REGION = 14484;
	private static final int HUGE_REMAINS_WEST = 3587;
	private static final int HUGE_REMAINS_EAST = 3594;
	private static final int HUGE_REMAINS_SOUTH = 9496;
	private static final int HUGE_REMAINS_NORTH = 9516;
	// Client script that draws the GOTR HUD. Its arguments carry energy, power, portal and guardian state.
	private static final int GOTR_HUD_SCRIPT = 5980;
	private static final int QUEST_CHECK_INTERVAL = 100;
	private static final int XP_ENERGY_THRESHOLD = 300;
	private static final int LUNAR_SPELLBOOK = 2;
	private static final int NPC_CONTACT_LEVEL = 67;
	// Ticks away from the temple before the game state is dropped; covers scene loads when entering altars.
	private static final int OUTSIDE_GRACE_TICKS = 5;
	static final String HINT_UNCHARGED_CELL = "Take an uncharged cell first";

	private static final Pattern COUNTDOWN = Pattern.compile("The rift will become active in (\\d+) seconds");
	private static final Pattern REWARD_TOTAL = Pattern.compile("Total elemental energy:\\s*([\\d,]+).*?Total catalytic energy:\\s*([\\d,]+)");
	private static final Pattern CHECK_POINTS = Pattern.compile("You have (\\d+) catalytic energy and (\\d+) elemental energy");

	private static final Set<Integer> SPEC_PICKAXES = ImmutableSet.of(
		ItemID.DRAGON_PICKAXE, ItemID.DRAGON_PICKAXE_PRETTY, ItemID.INFERNAL_PICKAXE, ItemID.INFERNAL_PICKAXE_EMPTY,
		ItemID._3A_PICKAXE, ItemID.CRYSTAL_PICKAXE, ItemID.TRAILBLAZER_PICKAXE, ItemID.TRAILBLAZER_PICKAXE_EMPTY,
		ItemID.TRAILBLAZER_PICKAXE_NO_INFERNAL, ItemID.TRAILBLAZER_RELOADED_PICKAXE,
		ItemID.TRAILBLAZER_RELOADED_PICKAXE_EMPTY, ItemID.TRAILBLAZER_RELOADED_PICKAXE_NO_INFERNAL);
	private static final Set<Integer> PARTS_REMAINS = ImmutableSet.of(
		ObjectID.GOTR_ESSENCE_TIER_1, ObjectID.GOTR_ESSENCE_TIER_1B,
		ObjectID.GOTR_ESSENCE_TIER_1_LARGE, ObjectID.GOTR_ESSENCE_TIER_1_LARGE_B);
	private static final Set<Integer> HUGE_REMAINS = ImmutableSet.of(ObjectID.GOTR_ESSENCE_TIER_3, ObjectID.GOTR_FALLEN_GUARDIAN);
	private static final Set<Integer> HUGE_EXITS = ImmutableSet.of(ObjectID.GOTR_AGILITY_PORTAL_BOTTOM_PARENT, ObjectID.GOTR_AGILITY_PORTAL_BOTTOM_CHILD);
	private static final Set<Integer> CELL_TILES = ImmutableSet.of(
		ObjectID.GOTR_CELL_TILE_INACTIVE, ObjectID.GOTR_CELL_TILE_TIER1, ObjectID.GOTR_CELL_TILE_TIER2,
		ObjectID.GOTR_CELL_TILE_TIER3, ObjectID.GOTR_CELL_TILE_TIER4);
	private static final Set<Integer> CORDELIA = ImmutableSet.of(NpcID.GOTR_CORDELIA, NpcID.GOTR_CORDELIA_1OP, NpcID.GOTR_CORDELIA_2OPS);

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	@Getter(AccessLevel.PACKAGE)
	private GotrAssistantConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private Notifier notifier;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private GotrInfoOverlay infoOverlay;

	@Inject
	private GotrSceneOverlay sceneOverlay;

	@Inject
	private GotrPouchOverlay pouchOverlay;

	@Inject
	private GotrSpellOverlay spellOverlay;

	@Inject
	private GotrRuneOverlay runeOverlay;

	// --- scene ---
	@Getter(AccessLevel.PACKAGE)
	private final Map<Guardian, GameObject> portalGuardians = new EnumMap<>(Guardian.class);
	@Getter(AccessLevel.PACKAGE)
	private final Set<TileObject> cellTiles = new HashSet<>();
	private final Set<TileObject> partsRemains = new HashSet<>();
	private final Set<TileObject> largeRemains = new HashSet<>();
	private final Set<TileObject> hugeRemains = new HashSet<>();
	@Getter(AccessLevel.PACKAGE)
	private GameObject workbench;
	@Getter(AccessLevel.PACKAGE)
	private GameObject depositPool;
	@Getter(AccessLevel.PACKAGE)
	private GameObject unchargedCellTable;
	@Getter(AccessLevel.PACKAGE)
	private GameObject portal;
	private GameObject hugeExit;
	@Getter(AccessLevel.PACKAGE)
	private GameObject elementalPile;
	@Getter(AccessLevel.PACKAGE)
	private GameObject catalyticPile;
	private GameObject altar;
	private GameObject altarExit;
	@Getter(AccessLevel.PACKAGE)
	private NPC greatGuardian;
	@Getter(AccessLevel.PACKAGE)
	private NPC cordelia;

	// --- game state ---
	@Getter(AccessLevel.PACKAGE)
	private Location location = Location.OUTSIDE;
	@Getter(AccessLevel.PACKAGE)
	private Phase phase = Phase.IDLE;
	private int startAtTick = -1;
	private int gameStartTick = -1;
	private boolean startNotified;
	private int hudTick = -1;
	private int hudElemental;
	private int hudCatalytic;
	@Getter(AccessLevel.PACKAGE)
	private int power;
	@Getter(AccessLevel.PACKAGE)
	private int maxPower;
	private int hudPortalLocation;
	private int hudElementalIndex;
	private int hudCatalyticIndex;
	@Getter(AccessLevel.PACKAGE)
	private int guardiansBuilt;
	@Getter(AccessLevel.PACKAGE)
	private int guardiansMax;
	private int hudGuardianTicks = -1;
	private int hudPortalTicks = -1;
	private int lastPortalTick = -1;
	private int portalsThisGame;
	@Getter(AccessLevel.PACKAGE)
	private String portalDirection;
	@Getter(AccessLevel.PACKAGE)
	private Guardian activeElemental;
	@Getter(AccessLevel.PACKAGE)
	private Guardian activeCatalytic;
	private final Set<Guardian> questLocked = EnumSet.noneOf(Guardian.class);
	private int lastQuestCheck = -QUEST_CHECK_INTERVAL;

	// --- inventory ---
	@Getter(AccessLevel.PACKAGE)
	private int fragments;
	@Getter(AccessLevel.PACKAGE)
	private int essence;
	@Getter(AccessLevel.PACKAGE)
	private int freeSlots;
	private int elementalStones;
	private int catalyticStones;
	private int polyElementalStones;
	private int polyCatalyticStones;
	private boolean hasChisel;
	@Getter(AccessLevel.PACKAGE)
	private CellTier chargedCell;
	private boolean hasUnchargedCell;
	private int depositableRunes;
	private final Map<Guardian, Integer> elementalRunes = new EnumMap<>(Guardian.class);
	private final Set<Guardian> talismans = EnumSet.noneOf(Guardian.class);
	private final Map<EssencePouch, Boolean> pouchesHeld = new EnumMap<>(EssencePouch.class);
	private boolean specPickaxeHeld;
	private boolean bindingWorn;

	// --- derived each tick ---
	@Getter(AccessLevel.PACKAGE)
	private Step step;
	@Getter(AccessLevel.PACKAGE)
	private final List<String> hints = new ArrayList<>();
	@Getter(AccessLevel.PACKAGE)
	private final List<PouchStatus> pouches = new ArrayList<>();
	@Getter(AccessLevel.PACKAGE)
	private Guardian plannedGuardian;
	@Getter(AccessLevel.PACKAGE)
	private CombinationRune plannedCombination;
	@Getter(AccessLevel.PACKAGE)
	private String combinationStatus;
	@Getter(AccessLevel.PACKAGE)
	private CombinationRune activeCombination;
	@Getter(AccessLevel.PACKAGE)
	private final Set<Integer> combinationItems = new HashSet<>();
	private int pouchFree;
	private int pouchStored;
	private boolean converting;
	private boolean specWasReady;
	private WorldPoint hintArrowPoint;
	private int outsideTicks;
	// Set between a game's end and the next countdown, so the HUD does not revive the finished game.
	private boolean gameOver;
	private boolean repairChecked;

	// --- points ---
	@Getter(AccessLevel.PACKAGE)
	private int bankedElemental = -1;
	@Getter(AccessLevel.PACKAGE)
	private int bankedCatalytic = -1;
	@Getter(AccessLevel.PACKAGE)
	private int sessionElemental;
	@Getter(AccessLevel.PACKAGE)
	private int sessionCatalytic;
	@Getter(AccessLevel.PACKAGE)
	private int gamesPlayed;
	private long sessionStartMillis = -1;

	// --- pouch wear measurement ---
	private final Map<EssencePouch, Integer> counterAtStart = new EnumMap<>(EssencePouch.class);
	private final Map<EssencePouch, int[]> wearPerGame = new EnumMap<>(EssencePouch.class);

	@Provides
	GotrAssistantConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GotrAssistantConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(infoOverlay);
		overlayManager.add(sceneOverlay);
		overlayManager.add(pouchOverlay);
		overlayManager.add(spellOverlay);
		overlayManager.add(runeOverlay);
		loadBankedPoints();
		clientThread.invokeLater(() -> readWorn(client.getItemContainer(InventoryID.WORN)));
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(infoOverlay);
		overlayManager.remove(sceneOverlay);
		overlayManager.remove(pouchOverlay);
		overlayManager.remove(spellOverlay);
		overlayManager.remove(runeOverlay);
		clearHintArrow();
		clearScene();
		resetGame();
		phase = Phase.IDLE;
		location = Location.OUTSIDE;
		step = null;
		hints.clear();
		pouches.clear();
	}

	// ------------------------------------------------------------------ events

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING || event.getGameState() == GameState.LOGIN_SCREEN
			|| event.getGameState() == GameState.HOPPING)
		{
			clearScene();
		}
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			loadBankedPoints();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (GotrAssistantConfig.GROUP.equals(event.getGroup()) && !config.useHintArrow())
		{
			clearHintArrow();
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject object = event.getGameObject();
		int id = object.getId();
		Guardian guardian = Guardian.fromPortalObject(id);
		if (guardian != null)
		{
			portalGuardians.put(guardian, object);
			return;
		}
		if (Guardian.fromAltarObject(id) != null)
		{
			altar = object;
			return;
		}
		if (Guardian.fromExitPortal(id) != null)
		{
			altarExit = object;
			return;
		}
		if (HUGE_EXITS.contains(id))
		{
			hugeExit = object;
			return;
		}
		addTileObject(object);
		switch (id)
		{
			case ObjectID.GOTR_WORKBENCH:
				workbench = object;
				break;
			case ObjectID.GOTR_DEPOSITCHEST:
				depositPool = object;
				break;
			case ObjectID.GOTR_UNCHARGED_CELLS:
				unchargedCellTable = object;
				break;
			case ObjectID.GOTR_AGILITY_PORTAL_TOP:
				portal = object;
				break;
			case ObjectID.GOTR_GUARDIAN_SPAWN_EAST:
				elementalPile = object;
				break;
			case ObjectID.GOTR_GUARDIAN_SPAWN_WEST:
				catalyticPile = object;
				break;
			default:
				break;
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		GameObject object = event.getGameObject();
		portalGuardians.values().remove(object);
		removeTileObject(object);
		workbench = object == workbench ? null : workbench;
		depositPool = object == depositPool ? null : depositPool;
		unchargedCellTable = object == unchargedCellTable ? null : unchargedCellTable;
		portal = object == portal ? null : portal;
		hugeExit = object == hugeExit ? null : hugeExit;
		elementalPile = object == elementalPile ? null : elementalPile;
		catalyticPile = object == catalyticPile ? null : catalyticPile;
		altar = object == altar ? null : altar;
		altarExit = object == altarExit ? null : altarExit;
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		addTileObject(event.getGroundObject());
	}

	@Subscribe
	public void onGroundObjectDespawned(GroundObjectDespawned event)
	{
		removeTileObject(event.getGroundObject());
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();
		if (npc.getId() == NpcID.GOTR_GREAT_GUARDIAN)
		{
			greatGuardian = npc;
		}
		else if (CORDELIA.contains(npc.getId()))
		{
			cordelia = npc;
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();
		greatGuardian = npc == greatGuardian ? null : greatGuardian;
		cordelia = npc == cordelia ? null : cordelia;
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event)
	{
		if (event.getScriptId() != GOTR_HUD_SCRIPT || event.getScriptEvent() == null)
		{
			return;
		}
		Object[] args = event.getScriptEvent().getArguments();
		if (args == null || args.length < 12)
		{
			return;
		}
		try
		{
			hudElemental = toInt(args[1]);
			hudCatalytic = toInt(args[2]);
			power = toInt(args[3]);
			maxPower = toInt(args[4]);
			int portalLocation = toInt(args[5]);
			int elementalIndex = toInt(args[6]);
			int catalyticIndex = toInt(args[7]);
			guardiansBuilt = toInt(args[8]);
			guardiansMax = toInt(args[9]);
			hudGuardianTicks = toInt(args[10]);
			hudPortalTicks = toInt(args[11]);
			hudTick = client.getTickCount();

			if (portalLocation > 0 && hudPortalLocation <= 0)
			{
				onPortalOpened();
			}
			hudPortalLocation = portalLocation;

			boolean rotated = (elementalIndex > 0 && elementalIndex != hudElementalIndex)
				|| (catalyticIndex > 0 && catalyticIndex != hudCatalyticIndex);
			hudElementalIndex = elementalIndex;
			hudCatalyticIndex = catalyticIndex;
			if (rotated && phase == Phase.ACTIVE)
			{
				Guardian e = Guardian.fromHud(Alignment.ELEMENTAL, elementalIndex);
				Guardian c = Guardian.fromHud(Alignment.CATALYTIC, catalyticIndex);
				notifier.notify(config.notifyGuardianRotation(), "GOTR: guardians changed to "
					+ (e == null ? "?" : e.getLabel()) + " and " + (c == null ? "?" : c.getLabel()));
			}

			// Plugin enabled or logged in mid-game.
			if (phase == Phase.IDLE && !gameOver && maxPower > 0 && power > 0)
			{
				phase = Phase.ACTIVE;
			}
			// The Great Guardian was defeated.
			else if (phase == Phase.ACTIVE && maxPower > 0 && power == 0 && portalLocation == -1)
			{
				phase = Phase.ENDING;
			}
		}
		catch (NumberFormatException ex)
		{
			log.debug("Unparseable GOTR HUD arguments", ex);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM
			&& event.getType() != ChatMessageType.MESBOX && event.getType() != ChatMessageType.ENGINE)
		{
			return;
		}
		String msg = Text.removeTags(event.getMessage());

		Matcher countdown = COUNTDOWN.matcher(msg);
		if (countdown.find())
		{
			int seconds = Integer.parseInt(countdown.group(1));
			phase = Phase.COUNTDOWN;
			gameOver = false;
			startAtTick = client.getTickCount() + (int) Math.round(seconds / TICK_SECONDS);
			if (seconds <= 10 && !startNotified)
			{
				startNotified = true;
				notifier.notify(config.notifyGameStart(), "GOTR: the rift opens in " + seconds + " seconds");
			}
			return;
		}
		if (msg.contains("The rift becomes active!"))
		{
			startGame();
			return;
		}
		if (msg.contains("The Portal Guardians will keep their rifts open for another 30 seconds")
			|| msg.contains("successfully closed the rift"))
		{
			phase = Phase.ENDING;
			return;
		}
		Matcher reward = REWARD_TOTAL.matcher(msg);
		if (reward.find())
		{
			onRewardTotals(parse(reward.group(1)), parse(reward.group(2)));
			return;
		}
		Matcher check = CHECK_POINTS.matcher(msg);
		if (check.find())
		{
			setBankedPoints(Integer.parseInt(check.group(2)), Integer.parseInt(check.group(1)));
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.INV)
		{
			readInventory(event.getItemContainer());
		}
		else if (event.getContainerId() == InventoryID.WORN)
		{
			readWorn(event.getItemContainer());
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		updateLocation();
		if (location == Location.OUTSIDE)
		{
			if (phase != Phase.IDLE && ++outsideTicks > OUTSIDE_GRACE_TICKS)
			{
				resetGame();
				phase = Phase.IDLE;
				gameOver = false;
			}
			step = null;
			hints.clear();
			pouches.clear();
			clearHintArrow();
			repairChecked = false;
			return;
		}
		outsideTicks = 0;

		int now = client.getTickCount();
		if (now - lastQuestCheck >= QUEST_CHECK_INTERVAL)
		{
			lastQuestCheck = now;
			checkQuests();
		}
		if (phase == Phase.COUNTDOWN && startAtTick >= 0 && now > startAtTick + 10)
		{
			// Missed the start message; the HUD keeps the game alive from here.
			phase = maxPower > 0 && power > 0 ? Phase.ACTIVE : Phase.IDLE;
		}

		updateActiveGuardians();
		updatePouches();
		if (!repairChecked && !pouches.isEmpty())
		{
			repairChecked = true;
			if (pouchRepairNeeded())
			{
				notifier.notify(config.notifyPouchRepair(), "GOTR: your essence pouches need repairing"
					+ (repairViaNpcContact() ? " (NPC Contact)" : ""));
			}
		}
		updatePortalDirection();
		readCheckPointsDialog();
		updateSpec();
		updatePlan();
		step = computeStep();
		updateHintArrow();
	}

	// ------------------------------------------------------------------ helpers: scene

	private void addTileObject(TileObject object)
	{
		int id = object.getId();
		if (CELL_TILES.contains(id))
		{
			cellTiles.add(object);
		}
		else if (PARTS_REMAINS.contains(id))
		{
			partsRemains.add(object);
		}
		else if (id == ObjectID.GOTR_ESSENCE_TIER_2)
		{
			largeRemains.add(object);
		}
		else if (HUGE_REMAINS.contains(id))
		{
			hugeRemains.add(object);
		}
	}

	private void removeTileObject(TileObject object)
	{
		cellTiles.remove(object);
		partsRemains.remove(object);
		largeRemains.remove(object);
		hugeRemains.remove(object);
	}

	private void clearScene()
	{
		portalGuardians.clear();
		cellTiles.clear();
		partsRemains.clear();
		largeRemains.clear();
		hugeRemains.clear();
		workbench = null;
		depositPool = null;
		unchargedCellTable = null;
		portal = null;
		hugeExit = null;
		elementalPile = null;
		catalyticPile = null;
		altar = null;
		altarExit = null;
		greatGuardian = null;
		cordelia = null;
	}

	private void updateLocation()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			location = Location.OUTSIDE;
			return;
		}
		WorldPoint wp = WorldPoint.fromLocalInstance(client, player.getLocalLocation());
		if (wp.getRegionID() == TEMPLE_REGION)
		{
			boolean huge = wp.getX() >= HUGE_REMAINS_WEST && wp.getX() <= HUGE_REMAINS_EAST
				&& wp.getY() >= HUGE_REMAINS_SOUTH && wp.getY() <= HUGE_REMAINS_NORTH;
			location = huge ? Location.HUGE_REMAINS : Location.TEMPLE;
		}
		else if (altar != null && phase != Phase.IDLE)
		{
			location = Location.ALTAR;
		}
		else
		{
			location = Location.OUTSIDE;
		}
	}

	private void updateActiveGuardians()
	{
		Guardian e = hudElementalIndex > 0 ? Guardian.fromHud(Alignment.ELEMENTAL, hudElementalIndex) : null;
		Guardian c = hudCatalyticIndex > 0 ? Guardian.fromHud(Alignment.CATALYTIC, hudCatalyticIndex) : null;
		if (e == null || c == null)
		{
			// Fall back to the guardians' "active" animation.
			for (Map.Entry<Guardian, GameObject> entry : portalGuardians.entrySet())
			{
				Renderable renderable = entry.getValue().getRenderable();
				if (!(renderable instanceof DynamicObject))
				{
					continue;
				}
				Animation anim = ((DynamicObject) renderable).getAnimation();
				if (anim == null || anim.getId() != AnimationID.STATUE_RUNESTONE01_ACTIVE)
				{
					continue;
				}
				if (entry.getKey().getAlignment() == Alignment.ELEMENTAL && e == null)
				{
					e = entry.getKey();
				}
				else if (entry.getKey().getAlignment() == Alignment.CATALYTIC && c == null)
				{
					c = entry.getKey();
				}
			}
		}
		boolean gameRunning = phase == Phase.ACTIVE || phase == Phase.ENDING;
		activeElemental = gameRunning ? e : null;
		activeCatalytic = gameRunning ? c : null;
	}

	private void checkQuests()
	{
		questLocked.clear();
		for (Guardian guardian : Guardian.values())
		{
			if (guardian.getRequiredQuest() != null && guardian.getRequiredQuest().getState(client) != QuestState.FINISHED)
			{
				questLocked.add(guardian);
			}
		}
	}

	// ------------------------------------------------------------------ helpers: inventory

	private void readInventory(@Nullable ItemContainer container)
	{
		fragments = 0;
		essence = 0;
		freeSlots = 0;
		elementalStones = 0;
		catalyticStones = 0;
		polyElementalStones = 0;
		polyCatalyticStones = 0;
		hasChisel = false;
		chargedCell = null;
		hasUnchargedCell = false;
		depositableRunes = 0;
		elementalRunes.clear();
		talismans.clear();
		pouchesHeld.clear();
		specPickaxeHeld = specPickaxeWorn();
		if (container == null)
		{
			return;
		}

		Item[] items = container.getItems();
		int used = 0;
		for (Item item : items)
		{
			int id = item.getId();
			if (id <= 0)
			{
				continue;
			}
			used++;
			int qty = item.getQuantity();
			switch (id)
			{
				case ItemID.GOTR_GUARDIAN_FRAGMENT:
					fragments += qty;
					continue;
				case ItemID.GOTR_GUARDIAN_ESSENCE:
					essence += qty;
					continue;
				case ItemID.GOTR_GUARDIAN_STONE_ELEMENTAL:
					elementalStones += qty;
					continue;
				case ItemID.GOTR_GUARDIAN_STONE_CATALYTIC:
					catalyticStones += qty;
					continue;
				case ItemID.GOTR_GUARDIAN_STONE_POLYELEMENTAL:
					polyElementalStones += qty;
					continue;
				case ItemID.GOTR_GUARDIAN_STONE_POLYCATALYTIC:
					polyCatalyticStones += qty;
					continue;
				case ItemID.CHISEL:
					hasChisel = true;
					continue;
				case ItemID.GOTR_CELL_UNCHARGED:
					hasUnchargedCell = true;
					continue;
				default:
					break;
			}
			CellTier tier = CellTier.fromCellItem(id);
			if (tier != null)
			{
				if (chargedCell == null || tier.ordinal() > chargedCell.ordinal())
				{
					chargedCell = tier;
				}
				continue;
			}
			EssencePouch pouch = EssencePouch.fromItem(id);
			if (pouch != null)
			{
				pouchesHeld.put(pouch, id == pouch.getDegradedItemId());
				continue;
			}
			if (SPEC_PICKAXES.contains(id))
			{
				specPickaxeHeld = true;
				continue;
			}
			for (Guardian guardian : Guardian.values())
			{
				if (guardian.getAlignment() != Alignment.ELEMENTAL)
				{
					continue;
				}
				if (guardian.getRuneItemId() == id)
				{
					elementalRunes.merge(guardian, qty, Integer::sum);
				}
				if (guardian.getTalismanItemId() == id)
				{
					talismans.add(guardian);
				}
			}
			if (Guardian.isRune(id))
			{
				depositableRunes += qty;
			}
		}
		freeSlots = Math.max(0, 28 - used);

		if (config.craftCombinationRunes())
		{
			// Keep the runes used for combination crafting out of the deposit suggestion.
			Set<Guardian> keep = EnumSet.noneOf(Guardian.class);
			keep.addAll(talismans);
			CombinationRune preferred = config.combinationChoice().rune();
			if (preferred != null)
			{
				keep.add(preferred.getFirst());
				keep.add(preferred.getSecond());
			}
			for (Guardian guardian : keep)
			{
				depositableRunes -= elementalRunes.getOrDefault(guardian, 0);
			}
			depositableRunes = Math.max(0, depositableRunes);
		}
	}

	private void readWorn(@Nullable ItemContainer container)
	{
		bindingWorn = false;
		if (container != null)
		{
			Item amulet = container.getItem(EquipmentInventorySlot.AMULET.getSlotIdx());
			bindingWorn = amulet != null && amulet.getId() == ItemID.MAGIC_EMERALD_NECKLACE;
		}
		readInventory(client.getItemContainer(InventoryID.INV));
	}

	private boolean specPickaxeWorn()
	{
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		if (worn == null)
		{
			return false;
		}
		Item weapon = worn.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
		return weapon != null && SPEC_PICKAXES.contains(weapon.getId());
	}

	private int runecraftLevel()
	{
		return client.getBoostedSkillLevel(Skill.RUNECRAFT);
	}

	private int pouchCounter(EssencePouch pouch)
	{
		if (pouch.getDegradationVarp() >= 0)
		{
			return client.getVarpValue(pouch.getDegradationVarp());
		}
		if (pouch.getDegradationVarbit() >= 0)
		{
			return client.getVarbitValue(pouch.getDegradationVarbit());
		}
		return 0;
	}

	private void updatePouches()
	{
		pouches.clear();
		pouchFree = 0;
		pouchStored = 0;
		int level = runecraftLevel();
		for (Map.Entry<EssencePouch, Boolean> entry : pouchesHeld.entrySet())
		{
			EssencePouch pouch = entry.getKey();
			int counter = pouchCounter(pouch);
			int stage = pouch.stage(counter);
			int capacity = pouch.capacity(level, stage);
			int stored = client.getVarbitValue(pouch.getStoredVarbit());
			int untilDegrade = pouch.essenceUntilDegrade(counter);
			int gamesLeft = -1;
			if (pouch.degrades())
			{
				int perGame = wearPerGame(pouch, Math.max(1, capacity));
				gamesLeft = perGame <= 0 ? 0 : untilDegrade / perGame;
			}
			boolean degraded = entry.getValue() || stage > 0;
			pouches.add(new PouchStatus(pouch, stored, capacity, degraded, untilDegrade, gamesLeft));
			if (capacity > 0)
			{
				pouchFree += Math.max(0, capacity - stored);
			}
			pouchStored += stored;
		}
	}

	private int wearPerGame(EssencePouch pouch, int capacity)
	{
		int[] measured = wearPerGame.get(pouch);
		if (measured != null && measured[1] > 0)
		{
			return Math.max(1, measured[0] / measured[1]);
		}
		return config.fillsPerGame() * capacity;
	}

	/**
	 * Lunar spellbook with the Magic level for NPC Contact: the Dark Mage repairs pouches from anywhere.
	 */
	boolean repairViaNpcContact()
	{
		return config.repairViaNpcContact()
			&& client.getVarbitValue(VarbitID.SPELLBOOK) == LUNAR_SPELLBOOK
			&& client.getBoostedSkillLevel(Skill.MAGIC) >= NPC_CONTACT_LEVEL;
	}

	boolean pouchRepairNeeded()
	{
		for (PouchStatus status : pouches)
		{
			if (status.needsRepair(config.repairWarningGames()))
			{
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------ helpers: game flow

	private void startGame()
	{
		resetGame();
		phase = Phase.ACTIVE;
		gameOver = false;
		gameStartTick = client.getTickCount();
		if (sessionStartMillis < 0)
		{
			sessionStartMillis = System.currentTimeMillis();
		}
		counterAtStart.clear();
		for (EssencePouch pouch : pouchesHeld.keySet())
		{
			counterAtStart.put(pouch, pouchCounter(pouch));
		}
	}

	private void resetGame()
	{
		startAtTick = -1;
		gameStartTick = -1;
		startNotified = false;
		lastPortalTick = -1;
		portalsThisGame = 0;
		hudPortalLocation = 0;
		hudElementalIndex = 0;
		hudCatalyticIndex = 0;
		hudGuardianTicks = -1;
		hudPortalTicks = -1;
		hudTick = -1;
		power = 0;
		maxPower = 0;
		guardiansBuilt = 0;
		guardiansMax = 0;
		converting = false;
		specWasReady = false;
	}

	private void onPortalOpened()
	{
		lastPortalTick = client.getTickCount();
		portalsThisGame++;
		if (phase == Phase.ACTIVE)
		{
			hudPortalLocation = 1;
			updatePortalDirection();
			notifier.notify(config.notifyPortal(), "GOTR: the portal to the huge remains is open" + directionSuffix());
		}
	}

	private void onRewardTotals(int elemental, int catalytic)
	{
		int energyE = energyElemental();
		int energyC = energyCatalytic();
		int duration = secondsSinceStart();
		boolean exact = bankedElemental >= 0 && elemental >= bankedElemental && catalytic >= bankedCatalytic;
		int gainedE = exact ? elemental - bankedElemental : energyE / 100;
		int gainedC = exact ? catalytic - bankedCatalytic : energyC / 100;
		sessionElemental += gainedE;
		sessionCatalytic += gainedC;
		gamesPlayed++;
		setBankedPoints(elemental, catalytic);
		if (config.gameSummary())
		{
			postSummary(gainedE, gainedC, exact, energyE, energyC, duration);
		}

		for (Map.Entry<EssencePouch, Integer> entry : counterAtStart.entrySet())
		{
			int delta = pouchCounter(entry.getKey()) - entry.getValue();
			if (delta > 0)
			{
				int[] measured = wearPerGame.computeIfAbsent(entry.getKey(), k -> new int[2]);
				measured[0] += delta;
				measured[1]++;
			}
		}
		counterAtStart.clear();
		resetGame();
		phase = Phase.IDLE;
		gameOver = true;
	}

	private void postSummary(int gainedE, int gainedC, boolean exact, int energyE, int energyC, int seconds)
	{
		ChatMessageBuilder message = new ChatMessageBuilder()
			.append(ChatColorType.HIGHLIGHT)
			.append("GOTR game " + gamesPlayed + ": ")
			.append(ChatColorType.NORMAL)
			.append((exact ? "+" : "~+") + gainedE + " elemental / +" + gainedC + " catalytic points, energy "
				+ energyE + " / " + energyC);
		if (seconds > 0)
		{
			message.append(", " + seconds / 60 + "m " + seconds % 60 + "s");
		}
		message.append(". Banked " + bankedElemental + " / " + bankedCatalytic + ".");
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(message.build())
			.build());
	}

	private void readCheckPointsDialog()
	{
		Widget text = client.getWidget(InterfaceID.Messagebox.TEXT);
		if (text == null || text.isHidden() || text.getText() == null)
		{
			return;
		}
		Matcher m = CHECK_POINTS.matcher(Text.removeTags(text.getText()));
		if (m.find())
		{
			setBankedPoints(Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)));
		}
	}

	private void setBankedPoints(int elemental, int catalytic)
	{
		if (elemental == bankedElemental && catalytic == bankedCatalytic)
		{
			return;
		}
		bankedElemental = elemental;
		bankedCatalytic = catalytic;
		configManager.setRSProfileConfiguration(GotrAssistantConfig.GROUP, "bankedElemental", elemental);
		configManager.setRSProfileConfiguration(GotrAssistantConfig.GROUP, "bankedCatalytic", catalytic);
	}

	private void loadBankedPoints()
	{
		Integer e = configManager.getRSProfileConfiguration(GotrAssistantConfig.GROUP, "bankedElemental", Integer.class);
		Integer c = configManager.getRSProfileConfiguration(GotrAssistantConfig.GROUP, "bankedCatalytic", Integer.class);
		bankedElemental = e == null ? -1 : e;
		bankedCatalytic = c == null ? -1 : c;
	}

	private void updateSpec()
	{
		boolean ready = specPickaxeHeld && specPercent() >= 100;
		if (ready && !specWasReady && phase == Phase.ACTIVE)
		{
			notifier.notify(config.notifySpec(), "GOTR: pickaxe special attack is ready");
		}
		specWasReady = ready;
	}

	// ------------------------------------------------------------------ planning

	private boolean canEnter(Guardian guardian)
	{
		return runecraftLevel() >= guardian.getLevelRequired() && !questLocked.contains(guardian);
	}

	boolean isLocked(Guardian guardian)
	{
		return !canEnter(guardian);
	}

	/**
	 * Combination rune that can be made at this elemental guardian with what is carried, or null.
	 * With describe set, the reason is stored in {@link #combinationStatus}.
	 */
	@Nullable
	private CombinationRune combinationAt(@Nullable Guardian guardian, boolean describe)
	{
		if (!config.craftCombinationRunes() || guardian == null || guardian.getAlignment() != Alignment.ELEMENTAL)
		{
			return null;
		}
		CombinationRune preferred = config.combinationChoice().rune();
		String reason;
		CombinationRune found = null;
		if (preferred != null)
		{
			Guardian carried = otherElement(preferred, guardian);
			reason = carried == null
				? preferred.getLabel() + " is made at " + preferred.getFirst().getLabel() + " or " + preferred.getSecond().getLabel()
				: missingFor(preferred, carried);
			found = reason == null ? preferred : null;
		}
		else
		{
			reason = "carry runes of another element";
			for (Guardian carried : Guardian.values())
			{
				if (carried == guardian || carried.getAlignment() != Alignment.ELEMENTAL || elementalRunes.getOrDefault(carried, 0) <= 0)
				{
					continue;
				}
				CombinationRune rune = CombinationRune.of(guardian, carried);
				if (rune == null)
				{
					continue;
				}
				String missing = missingFor(rune, carried);
				if (missing == null)
				{
					found = rune;
					break;
				}
				reason = missing;
			}
		}
		if (found != null && !bindingWorn)
		{
			reason = "wear a binding necklace";
			found = null;
		}
		if (describe)
		{
			combinationStatus = found != null
				? "YES - " + found.getLabel() + " runes at " + guardian.getLabel()
				: "NO - " + reason;
		}
		return found;
	}

	/**
	 * What is missing to craft {@code rune} while carrying runes of {@code carried}, or null if nothing.
	 */
	@Nullable
	private String missingFor(CombinationRune rune, Guardian carried)
	{
		if (rune.getLevelRequired() > runecraftLevel())
		{
			return rune.getLabel() + " needs level " + rune.getLevelRequired();
		}
		if (elementalRunes.getOrDefault(carried, 0) <= 0)
		{
			return "carry " + carried.getLabel().toLowerCase() + " runes";
		}
		if (client.getVarbitValue(VarbitID.MAGIC_IMBUE_ACTIVE) <= 0 && !talismans.contains(carried))
		{
			return "bring a " + carried.getLabel().toLowerCase() + " talisman or Magic Imbue";
		}
		return null;
	}

	@Nullable
	private static Guardian otherElement(CombinationRune rune, Guardian altar)
	{
		if (rune.getFirst() == altar)
		{
			return rune.getSecond();
		}
		return rune.getSecond() == altar ? rune.getFirst() : null;
	}

	/**
	 * Inventory items needed for the chosen combination rune: the carried element's runes and talisman,
	 * plus a binding necklace when none is worn.
	 */
	private void updateCombinationItems(@Nullable CombinationRune possible)
	{
		combinationItems.clear();
		if (!config.craftCombinationRunes() || !config.highlightCombinationItems())
		{
			return;
		}
		CombinationRune target = config.combinationChoice().rune();
		if (target == null)
		{
			target = possible;
		}
		List<Guardian> carried = new ArrayList<>(2);
		if (target != null)
		{
			Guardian other = activeElemental == null ? null : otherElement(target, activeElemental);
			if (other != null)
			{
				carried.add(other);
			}
			else
			{
				carried.add(target.getFirst());
				carried.add(target.getSecond());
			}
		}
		for (Guardian guardian : carried)
		{
			combinationItems.add(guardian.getRuneItemId());
			combinationItems.add(guardian.getTalismanItemId());
		}
		if (!bindingWorn)
		{
			combinationItems.add(ItemID.MAGIC_EMERALD_NECKLACE);
		}
	}

	private int energyPerEssence(Guardian guardian)
	{
		return combinationAt(guardian, false) != null ? 3 : 2;
	}

	private void updatePlan()
	{
		combinationStatus = config.craftCombinationRunes()
			? (activeElemental == null ? "NO - no active elemental guardian" : null)
			: "OFF";
		CombinationRune combo = combinationAt(activeElemental, true);
		activeCombination = combo;
		updateCombinationItems(combo);
		if (combinationStatus == null)
		{
			combinationStatus = "NO";
		}

		List<Guardian> options = new ArrayList<>();
		if (activeElemental != null && canEnter(activeElemental))
		{
			options.add(activeElemental);
		}
		if (activeCatalytic != null && canEnter(activeCatalytic))
		{
			options.add(activeCatalytic);
		}
		if (options.isEmpty())
		{
			plannedGuardian = null;
			plannedCombination = null;
			return;
		}

		int projectedE = energyElemental() + 2 * elementalStones + 3 * polyElementalStones;
		int projectedC = energyCatalytic() + 2 * catalyticStones + 3 * polyCatalyticStones;
		GotrAssistantConfig.AltarStrategy strategy = config.altarStrategy();
		options.sort((a, b) ->
		{
			int cmp = 0;
			switch (strategy)
			{
				case BALANCED:
					cmp = Integer.compare(projected(a, projectedE, projectedC), projected(b, projectedE, projectedC));
					if (cmp == 0)
					{
						cmp = Integer.compare(energyPerEssence(b), energyPerEssence(a));
					}
					break;
				case MOST_ENERGY:
					cmp = Integer.compare(energyPerEssence(b), energyPerEssence(a));
					break;
				case HIGHEST_CELL_TIER:
					cmp = Integer.compare(b.getCellTier().ordinal(), a.getCellTier().ordinal());
					break;
				default:
					break;
			}
			if (cmp == 0)
			{
				cmp = Integer.compare(b.getCellTier().ordinal(), a.getCellTier().ordinal());
			}
			return cmp;
		});
		plannedGuardian = options.get(0);
		plannedCombination = plannedGuardian == activeElemental ? combo : null;
	}

	private static int projected(Guardian guardian, int elemental, int catalytic)
	{
		return guardian.getAlignment() == Alignment.ELEMENTAL ? elemental : catalytic;
	}

	@Nullable
	private Step computeStep()
	{
		hints.clear();
		if (location == Location.ALTAR)
		{
			return altarStep();
		}
		if (location == Location.HUGE_REMAINS)
		{
			return hugeRemainsStep();
		}

		boolean repair = pouchRepairNeeded();
		if (phase == Phase.IDLE || phase == Phase.COUNTDOWN)
		{
			if (repair)
			{
				if (repairViaNpcContact())
				{
					return Step.of(Step.Kind.REPAIR_POUCHES, "Cast NPC Contact on the Dark Mage to repair your pouches");
				}
				return Step.at(Step.Kind.REPAIR_POUCHES, "Repair your pouches at Apprentice Cordelia", cordelia);
			}
			if (phase == Phase.COUNTDOWN)
			{
				addSpecHint();
				return Step.at(Step.Kind.MINE, "Get ready: the rift opens soon. Head to the remains", nearest(largeRemains.isEmpty() ? partsRemains : largeRemains));
			}
			return Step.of(Step.Kind.WAIT, "Wait for the next game to start");
		}

		if (repair)
		{
			hints.add(repairViaNpcContact()
				? "A pouch needs repair - cast NPC Contact on the Dark Mage"
				: "A pouch needs repair soon (Apprentice Cordelia)");
		}
		if (config.craftCombinationRunes() && !bindingWorn)
		{
			hints.add("Wear a binding necklace for combination runes");
		}

		if (stoneCount() > 0)
		{
			return Step.at(Step.Kind.POWER_UP, "Power up the Great Guardian (" + stoneCount() + " stones)", greatGuardian);
		}
		if (phase == Phase.ENDING)
		{
			return Step.of(Step.Kind.WAIT, "Game is ending - wait for the next one");
		}
		if (config.depositRunes() && depositableRunes > 0)
		{
			return Step.at(Step.Kind.DEPOSIT_RUNES, "Deposit your runes in the deposit pool", depositPool);
		}
		if (chargedCell != null)
		{
			if (canBuildGuardian())
			{
				boolean elemental = energyElemental() <= energyCatalytic();
				GameObject pile = elemental ? elementalPile : catalyticPile;
				if (pile == null)
				{
					pile = elemental ? catalyticPile : elementalPile;
				}
				return Step.at(Step.Kind.BUILD_GUARDIAN, "Build a guardian at the " + (elemental ? "elemental" : "catalytic")
					+ " essence pile (" + chargedCell.getLabel().toLowerCase() + " cell)", pile);
			}
			if (config.placeCells())
			{
				return Step.at(Step.Kind.PLACE_CELL, "Place your " + chargedCell.getLabel().toLowerCase() + " cell on a cell tile", nearest(cellTiles));
			}
		}

		int capacityLeft = freeSlots + pouchFree;
		if (converting && (fragments == 0 || capacityLeft == 0))
		{
			converting = false;
		}
		if (converting)
		{
			if (freeSlots == 0)
			{
				return Step.of(Step.Kind.FILL_POUCHES, "Fill your essence pouches");
			}
			return Step.at(Step.Kind.WORKBENCH, "Craft guardian essence at the workbench", workbench);
		}
		if (essence > 0 && (capacityLeft == 0 || fragments == 0))
		{
			if (freeSlots == 0 && pouchFree > 0)
			{
				return Step.of(Step.Kind.FILL_POUCHES, "Fill your essence pouches");
			}
			return enterGuardianStep();
		}

		int target = config.fragmentTarget() > 0 ? config.fragmentTarget() : Math.max(1, capacityLeft);
		if (fragments >= target && capacityLeft > 0)
		{
			converting = true;
			return Step.at(Step.Kind.WORKBENCH, "Craft guardian essence at the workbench", workbench);
		}
		if (config.usePortal() && isPortalOpen() && portal != null && capacityLeft > 0)
		{
			return Step.at(Step.Kind.ENTER_PORTAL, "Enter the portal to the huge remains" + directionSuffix(), portal);
		}
		addSpecHint();
		TileObject rock = nearest(largeRemains.isEmpty() ? partsRemains : largeRemains);
		if (largeRemains.isEmpty() && partsRemains.isEmpty())
		{
			rock = null;
		}
		return Step.at(Step.Kind.MINE, "Mine guardian fragments (" + fragments + "/" + target + ")", rock);
	}

	private Step altarStep()
	{
		Guardian at = altar == null ? null : Guardian.fromAltarObject(altar.getId());
		String name = at == null ? "" : at.getLabel() + " ";
		if (essence > 0)
		{
			CombinationRune combo = combinationAt(at, false);
			String extra = combo != null ? " - use your runes for " + combo.getLabel().toLowerCase() + " runes" : "";
			return Step.at(Step.Kind.CRAFT, "Craft runes on the " + name + "altar" + extra, altar);
		}
		if (pouchStored > 0)
		{
			return Step.at(Step.Kind.EMPTY_POUCHES, "Empty your pouches and craft again", altar);
		}
		return Step.at(Step.Kind.LEAVE, "Leave through the portal", altarExit);
	}

	private Step hugeRemainsStep()
	{
		if (freeSlots == 0)
		{
			if (pouchFree > 0)
			{
				return Step.of(Step.Kind.FILL_POUCHES, "Fill your essence pouches");
			}
			return Step.at(Step.Kind.LEAVE, "Inventory full - leave through the portal", hugeExit);
		}
		addSpecHint();
		return Step.at(Step.Kind.MINE, "Mine the huge guardian remains", nearest(hugeRemains));
	}

	private Step enterGuardianStep()
	{
		if (config.unchargedCellReminder() && !hasUnchargedCell && chargedCell == null && unchargedCellTable != null)
		{
			hints.add(HINT_UNCHARGED_CELL);
		}
		if (plannedGuardian == null)
		{
			return Step.of(Step.Kind.WAIT, "No guardian you can enter is active - wait for the rotation");
		}
		GameObject object = portalGuardians.get(plannedGuardian);
		String combo = plannedCombination != null ? " (" + plannedCombination.getLabel().toLowerCase() + " runes)" : "";
		return Step.at(Step.Kind.ENTER_GUARDIAN, "Enter the " + plannedGuardian.getLabel() + " guardian" + combo, object);
	}

	private void addSpecHint()
	{
		if (specPickaxeHeld && specPercent() >= 100)
		{
			hints.add("Pickaxe special attack ready - use it while mining");
		}
	}

	@Nullable
	private TileObject nearest(Set<TileObject> objects)
	{
		Player player = client.getLocalPlayer();
		if (player == null || objects.isEmpty())
		{
			return null;
		}
		WorldPoint me = player.getWorldLocation();
		TileObject best = null;
		int bestDistance = Integer.MAX_VALUE;
		for (TileObject object : objects)
		{
			int distance = object.getWorldLocation().distanceTo(me);
			if (distance < bestDistance)
			{
				bestDistance = distance;
				best = object;
			}
		}
		return best;
	}

	private void updateHintArrow()
	{
		if (!config.useHintArrow())
		{
			return;
		}
		WorldPoint target = null;
		if (step != null)
		{
			if (step.getObject() != null)
			{
				target = step.getObject().getWorldLocation();
			}
			else if (step.getNpc() != null)
			{
				target = step.getNpc().getWorldLocation();
			}
		}
		if (target == null)
		{
			clearHintArrow();
		}
		else if (!target.equals(hintArrowPoint))
		{
			client.setHintArrow(target);
			hintArrowPoint = target;
		}
	}

	private void clearHintArrow()
	{
		if (hintArrowPoint != null)
		{
			client.clearHintArrow();
			hintArrowPoint = null;
		}
	}

	// ------------------------------------------------------------------ getters for overlays

	boolean isVisible()
	{
		return location != Location.OUTSIDE;
	}

	boolean isGameRunning()
	{
		return phase == Phase.ACTIVE || phase == Phase.ENDING;
	}

	boolean isPortalOpen()
	{
		return hudPortalLocation > 0;
	}

	boolean hasStones()
	{
		return stoneCount() > 0;
	}

	int stoneCount()
	{
		return elementalStones + catalyticStones + polyElementalStones + polyCatalyticStones;
	}

	boolean hasDepositableRunes()
	{
		return depositableRunes > 0;
	}

	boolean canBuildGuardian()
	{
		return config.buildGuardians() && phase == Phase.ACTIVE && hasChisel && chargedCell != null
			&& guardiansMax > 0 && guardiansBuilt < guardiansMax;
	}

	boolean showUnchargedCellTable()
	{
		return hints.contains(HINT_UNCHARGED_CELL);
	}

	int energyElemental()
	{
		return hudTick >= 0 ? hudElemental : Math.max(0, client.getVarbitValue(VarbitID.GOTR_ELEMENTAL_EARNED_THIS_GAME));
	}

	int energyCatalytic()
	{
		return hudTick >= 0 ? hudCatalytic : Math.max(0, client.getVarbitValue(VarbitID.GOTR_CATALYTIC_EARNED_THIS_GAME));
	}

	int xpThreshold()
	{
		return XP_ENERGY_THRESHOLD;
	}

	/**
	 * Energy once all stones and carried essence (inventory and pouches) are handed in.
	 */
	int potentialElemental()
	{
		int fromEssence = plannedGuardian != null && plannedGuardian.getAlignment() == Alignment.ELEMENTAL
			? (essence + pouchStored) * energyPerEssence(plannedGuardian) : 0;
		return energyElemental() + 2 * elementalStones + 3 * polyElementalStones + fromEssence;
	}

	int potentialCatalytic()
	{
		int fromEssence = plannedGuardian != null && plannedGuardian.getAlignment() == Alignment.CATALYTIC
			? (essence + pouchStored) * 2 : 0;
		return energyCatalytic() + 2 * catalyticStones + 3 * polyCatalyticStones + fromEssence;
	}

	int essenceCarried()
	{
		return essence + pouchStored;
	}

	/**
	 * Essence that fits in total: what is carried plus free inventory slots and free pouch space.
	 */
	int essenceCapacity()
	{
		return essence + pouchStored + freeSlots + pouchFree;
	}

	int pouchEssence()
	{
		return pouchStored;
	}

	/**
	 * Games per hour since the first game this session, or -1 before the first finished game.
	 */
	double gamesPerHour()
	{
		if (gamesPlayed == 0 || sessionStartMillis < 0)
		{
			return -1;
		}
		double hours = (System.currentTimeMillis() - sessionStartMillis) / 3_600_000.0;
		return hours <= 0 ? -1 : gamesPlayed / hours;
	}

	boolean hasSpecPickaxe()
	{
		return specPickaxeHeld;
	}

	int specPercent()
	{
		return client.getVarpValue(VarPlayerID.SA_ENERGY) / 10;
	}

	/**
	 * Seconds until the rift opens, or -1.
	 */
	int secondsToStart()
	{
		if (phase != Phase.COUNTDOWN || startAtTick < 0)
		{
			return -1;
		}
		return ticksToSeconds(Math.max(0, startAtTick - client.getTickCount()));
	}

	int secondsSinceStart()
	{
		return gameStartTick < 0 || !isGameRunning() ? -1 : ticksToSeconds(client.getTickCount() - gameStartTick);
	}

	/**
	 * Seconds the open portal stays open, or -1.
	 */
	int portalSecondsLeft()
	{
		if (!isPortalOpen() || hudPortalTicks < 0)
		{
			return -1;
		}
		return ticksToSeconds(Math.max(0, hudPortalTicks - (client.getTickCount() - hudTick)));
	}

	/**
	 * Estimated seconds until the next portal (negative when overdue), or {@link Integer#MIN_VALUE}
	 * when unknown. Portal spawns are not exact.
	 */
	int secondsToNextPortal()
	{
		if (phase != Phase.ACTIVE || isPortalOpen())
		{
			return Integer.MIN_VALUE;
		}
		if (portalsThisGame == 0)
		{
			int since = secondsSinceStart();
			return since < 0 ? Integer.MIN_VALUE : config.firstPortalSeconds() - since;
		}
		int sinceLast = ticksToSeconds(client.getTickCount() - lastPortalTick);
		return config.portalIntervalSeconds() - sinceLast;
	}

	/**
	 * Seconds until the active guardians change, or -1.
	 */
	int secondsToRotation()
	{
		if (!isGameRunning() || hudGuardianTicks < 0 || hudTick < 0)
		{
			return -1;
		}
		return ticksToSeconds(Math.max(0, hudGuardianTicks - (client.getTickCount() - hudTick)));
	}

	List<Guardian> activeGuardians()
	{
		List<Guardian> active = new ArrayList<>(2);
		if (activeElemental != null)
		{
			active.add(activeElemental);
		}
		if (activeCatalytic != null)
		{
			active.add(activeCatalytic);
		}
		return Collections.unmodifiableList(active);
	}

	private void updatePortalDirection()
	{
		Widget direction = isPortalOpen() ? client.getWidget(InterfaceID.GotrHud.PORTAL_POSITION) : null;
		portalDirection = direction != null && !direction.isHidden() && direction.getText() != null
			? Text.removeTags(direction.getText()).trim() : null;
	}

	private String directionSuffix()
	{
		return portalDirection == null || portalDirection.isEmpty() ? "" : " (" + portalDirection + ")";
	}

	static int ticksToSeconds(int ticks)
	{
		return (int) Math.round(ticks * TICK_SECONDS);
	}

	private static int toInt(Object o)
	{
		return o instanceof Integer ? (Integer) o : Integer.parseInt(String.valueOf(o));
	}

	private static int parse(String number)
	{
		return Integer.parseInt(number.replace(",", ""));
	}

	// Exposed for the pouch overlay.
	Map<Integer, PouchStatus> pouchStatusByItem()
	{
		Map<Integer, PouchStatus> map = new HashMap<>();
		for (PouchStatus status : pouches)
		{
			map.put(status.getPouch().getItemId(), status);
			if (status.getPouch().getDegradedItemId() > 0)
			{
				map.put(status.getPouch().getDegradedItemId(), status);
			}
		}
		return map;
	}
}
