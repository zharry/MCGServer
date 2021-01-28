package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Servers.ServerInterface;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.PluginManager;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class PlayerListenerAdapter<S extends ServerInterface, T extends PlayerInterface> implements Listener {

	protected final S server;
	private final Class<T> type;
	public final boolean ignoreCreative;
	private final PluginManager pluginManager;

	public PlayerListenerAdapter(S server, Class<T> type, boolean ignoreCreative) {
		this.server = server;
		this.type = type;
		this.ignoreCreative = ignoreCreative;
		this.pluginManager = server.plugin.getServer().getPluginManager();
		registerEvents();
	}

	public PlayerListenerAdapter(S server, Class<T> type) {
		this(server, type, true);
	}

	public <E extends Event> void registerEvent(Class<E> eventClass, Function<E, Entity> getPlayer, BiConsumer<T, E> dispatch) {
		pluginManager.registerEvent(eventClass, this, EventPriority.NORMAL, (l, e) -> {
			T player = getPlayerInterfaceIgnoreCreative(getPlayer.apply((E) e), ignoreCreative);
			if(player != null) {
				dispatch.accept(player, (E) e);
			}
		}, server.plugin);
	}

	public T getPlayerFromUUID(UUID uuid) {
		PlayerInterface player = server.getPlayerFromUUID(uuid);
		if(type.isInstance(player)) {
			return (T) player;
		}
		return null;
	}

	public T getPlayerInterfaceIgnoreCreative(Entity e, boolean ignoreCreative) {
		if(!(e instanceof Player)) {
			return null;
		}
		if(ignoreCreative && MCGMain.gameModeManager.getGameMode((Player) e) == GameMode.CREATIVE) {
			return null;
		}
		return getPlayerFromUUID(e.getUniqueId());
	}

	public T getPlayerInterface(Entity p) {
		return getPlayerInterfaceIgnoreCreative(p, false);
	}

	public void registerEvents() {
		registerEvent(PlayerJoinEvent.class, PlayerJoinEvent::getPlayer, this::onJoin);
		registerEvent(PlayerQuitEvent.class, PlayerQuitEvent::getPlayer, this::onQuit);
		registerEvent(PlayerMoveEvent.class, PlayerMoveEvent::getPlayer, this::onMove);
		registerEvent(BlockBreakEvent.class, BlockBreakEvent::getPlayer, this::onBlockBreak);
		registerEvent(BlockPlaceEvent.class, BlockPlaceEvent::getPlayer, this::onBlockPlace);
		registerEvent(PlayerDeathEvent.class, PlayerDeathEvent::getEntity, this::onDeath);
		registerEvent(EntityDamageEvent.class, EntityDamageEvent::getEntity, this::onDamage);
		registerEvent(PlayerDropItemEvent.class, PlayerDropItemEvent::getPlayer, this::onDropItem);
		registerEvent(PlayerRespawnEvent.class, PlayerRespawnEvent::getPlayer, this::onRespawn);
		registerEvent(PlayerInteractEvent.class, PlayerInteractEvent::getPlayer, this::onInteract);
		registerEvent(EntityShootBowEvent.class, EntityShootBowEvent::getEntity, this::onShootBow);
		registerEvent(InventoryClickEvent.class, InventoryClickEvent::getWhoClicked, this::onInventoryClick);
		registerEvent(InventoryOpenEvent.class, InventoryOpenEvent::getPlayer, this::onInventoryOpen);
		registerEvent(FoodLevelChangeEvent.class, FoodLevelChangeEvent::getEntity, this::onFoodLevelChange);
	}

	protected void onJoin(T player, PlayerJoinEvent event) {}
	protected void onQuit(T player, PlayerQuitEvent event) {}
	protected void onMove(T player, PlayerMoveEvent event) {}
	protected void onBlockBreak(T player, BlockBreakEvent event) {}
	protected void onBlockPlace(T player, BlockPlaceEvent event) {}
	protected void onDeath(T player, PlayerDeathEvent event) {}
	protected void onDamage(T player, EntityDamageEvent event) {}
	protected void onDropItem(T player, PlayerDropItemEvent event) {}
	protected void onRespawn(T player, PlayerRespawnEvent event) {}
	protected void onInteract(T player, PlayerInteractEvent event) {}
	protected void onShootBow(T player, EntityShootBowEvent event) {}
	protected void onInventoryClick(T player, InventoryClickEvent event) {}
	protected void onInventoryOpen(T player, InventoryOpenEvent event) {}
	protected void onFoodLevelChange(T player, FoodLevelChangeEvent event) {}
}
