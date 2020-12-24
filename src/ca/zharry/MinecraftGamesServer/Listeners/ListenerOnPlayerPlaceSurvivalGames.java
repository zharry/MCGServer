package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.Servers.ServerSurvivalGames;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ListenerOnPlayerPlaceSurvivalGames implements Listener {

    ServerSurvivalGames server;

    public ListenerOnPlayerPlaceSurvivalGames(ServerSurvivalGames server) {
        this.server = server;
    }

    @EventHandler
    public void onPlayerPlace(BlockPlaceEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }
        if (server.state == ServerSurvivalGames.GAME_INPROGRESS) {
            Block block = event.getBlock();
            if (block.getType() == Material.TNT) {
                block.setType(Material.AIR);
                Location location = block.getLocation();
                location = location.add(0.5, 0, 0.5);
                TNTPrimed tnt = block.getWorld().spawn(location, TNTPrimed.class);
                tnt.setSource(event.getPlayer());
            } else if (block.getType() == Material.COBWEB) {
            } else {
                event.setCancelled(true);
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerItemUse(PlayerInteractEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }
        if (server.state == ServerSurvivalGames.GAME_INPROGRESS) {
        } else {
            event.setCancelled(true);
        }
    }

        @EventHandler
    public void onPlayerDestroy(BlockBreakEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }
        if (server.state == ServerSurvivalGames.GAME_INPROGRESS) {
            Block block = event.getBlock();
            if (block.getType() == Material.COBWEB) {
            } else if (block.getType() == Material.FERN) {
            } else if (block.getType() == Material.POPPY) {
            } else if (block.getType() == Material.DANDELION) {
            } else if (block.getType() == Material.TALL_GRASS) {
            } else if (block.getType() == Material.GRASS) {
            } else {
                event.setCancelled(true);
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTntExplode(EntityExplodeEvent event) {
        event.blockList().clear();
    }

}
