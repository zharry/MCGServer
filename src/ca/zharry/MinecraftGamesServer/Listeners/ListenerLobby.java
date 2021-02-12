package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Players.PlayerLobby;
import ca.zharry.MinecraftGamesServer.Servers.ServerLobby;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class ListenerLobby extends PlayerListenerAdapter<ServerLobby, PlayerLobby> {

    public ListenerLobby(ServerLobby server) {
        super(server, PlayerLobby.class);
    }

    @Override
    public void onJoin(PlayerLobby player, PlayerJoinEvent event) {
        player.setGameMode(GameMode.SURVIVAL);

        player.teleport(server.serverSpawn);
        player.bukkitPlayer.setBedSpawnLocation(server.serverSpawn, true);
    }

    @Override
    public void onDamage(PlayerLobby player, EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID) {
            if(isInSafeZone(player)) {
                event.setCancelled(true);
            } else {
                event.setDamage(0);
            }
        }
    }

    @Override
    public void onMove(PlayerLobby player, PlayerMoveEvent event) {
        ItemStack chestplate = player.bukkitPlayer.getInventory().getChestplate();
        if(chestplate != null) {
            ItemMeta meta = chestplate.getItemMeta();
            if (meta != null) {
                if(isInSafeZone(player)) {
                    ((Damageable) meta).setDamage(432);
                } else {
                    ((Damageable) meta).setDamage(0);
                }
                chestplate.setItemMeta(meta);
            }
        }
        if(player.getLocation().getY() > 255) {
            player.teleportRelative(player.getLocation().getX(), 255, player.getLocation().getZ(), 0, 0, false, false, false, true, true);
        }
    }

    @Override
    public void onInteract(PlayerLobby player, PlayerInteractEvent event) {
        if(event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if(event.getItem() != null && event.getItem().getType() == Material.FIREWORK_ROCKET) {
                event.setCancelled(true);
            }
        }
    }

    @Override
    public void onInteractEntity(PlayerLobby player, PlayerInteractEntityEvent event) {
        if(event.getRightClicked().getType() == EntityType.ITEM_FRAME) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onInteractAtEntity(PlayerLobby player, PlayerInteractAtEntityEvent event) {
        if(event.getRightClicked().getType() == EntityType.ARMOR_STAND) {
            event.setCancelled(true);
        }
    }

    public boolean isInSafeZone(PlayerLobby player) {
        int x = player.getLocation().getBlockX();
        int y = player.getLocation().getBlockY();
        int z = player.getLocation().getBlockZ();
        return ServerLobby.SAFE_REGION.contains(player) && player.getLocation().getY() < server.world.getHighestBlockYAt(x, z) || y > 200;
    }

    @Override
    public void onBlockBreak(PlayerLobby player, BlockBreakEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onBlockPlace(PlayerLobby player, BlockPlaceEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        event.setDamage(0);
        if(event.getEntityType() == EntityType.ITEM_FRAME) {
            event.setCancelled(true);
        }
    }
}
