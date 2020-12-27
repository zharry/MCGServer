package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.Players.PlayerParkour;
import ca.zharry.MinecraftGamesServer.Servers.ServerParkour;
import ca.zharry.MinecraftGamesServer.Utils.Point3D;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;

public class ListenerParkour implements Listener {

    ServerParkour server;

    public ListenerParkour(ServerParkour server) {
        this.server = server;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerParkour playerParkour = new PlayerParkour(player, server);
        server.addPlayer(playerParkour);
        player.setGameMode(GameMode.ADVENTURE);
        player.setInvisible(false);
        server.setPlayerInventoryContents(player);

        if (server.state == ServerParkour.GAME_WAITING) {
            Location serverSpawn = new Location(player.getWorld(), 253.5, 134, -161.5);
            player.teleport(serverSpawn);
            player.setBedSpawnLocation(serverSpawn, true);

        } else if (server.state == ServerParkour.GAME_STARTING) {
            Point3D checkpointLoc = ServerParkour.stage1Checkpoints.get(0);
            Location firstCheckpoint = new Location(player.getWorld(), checkpointLoc.x, checkpointLoc.y, checkpointLoc.z);
            player.teleport(firstCheckpoint);
            player.setBedSpawnLocation(firstCheckpoint, true);

        } else if (server.state == ServerParkour.GAME_INPROGRESS) {
            // Teleport player to last checkpoint
            Point3D checkpointLoc = ServerParkour.stage1Checkpoints.get(0);
            switch (playerParkour.stage) {
                case 1:
                    checkpointLoc = ServerParkour.stage1Checkpoints.get(playerParkour.level);
                    break;
                case 2:
                    checkpointLoc = ServerParkour.stage2Checkpoints.get(playerParkour.level);
                    break;
                case 3:
                    checkpointLoc = ServerParkour.stage3Checkpoints.get(playerParkour.level);
                    break;
                case 4:
                    checkpointLoc = ServerParkour.stage4Checkpoints.get(playerParkour.level);
                    break;
                case 5:
                    checkpointLoc = ServerParkour.stage5Checkpoints.get(playerParkour.level);
                    break;
                case 6:
                    checkpointLoc = ServerParkour.stage6Checkpoints.get(playerParkour.level);
                    break;
                default:
            }
            Location nextStage = new Location(player.getWorld(), checkpointLoc.getX() + 0.5, checkpointLoc.getY() + 1, checkpointLoc.getZ() + 0.5);
            player.teleport(nextStage);
            player.setInvisible(true);

        } else if (server.state == ServerParkour.GAME_FINISHED) {
            Location gameSpectate = new Location(player.getWorld(), 8.5, 131, 9.5);
            player.teleport(gameSpectate);
            player.setBedSpawnLocation(gameSpectate, true);
        }
        player.setDisplayName(server.teams.get(server.teamLookup.get(player.getUniqueId())).chatColor + player.getName() + ChatColor.RESET);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        for (int i = 0; i < server.players.size(); i++) {
            PlayerParkour curPlayer = (PlayerParkour) server.players.get(i);
            if (curPlayer.bukkitPlayer.getUniqueId() == player.getUniqueId()) {
                curPlayer.commit();
                server.players.remove(i);
                break;
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.setDeathMessage(null);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (server.state == ServerParkour.GAME_STARTING) {
            Player player = event.getPlayer();
            if (event.getFrom().getX() != event.getTo().getX() ||
                    event.getFrom().getY() != event.getTo().getY() ||
                    event.getFrom().getZ() != event.getTo().getZ()) {
                Location location = new Location(player.getWorld(),
                        event.getFrom().getX(), event.getFrom().getY(), event.getFrom().getZ());
                location.setPitch(player.getLocation().getPitch());
                location.setYaw(player.getLocation().getYaw());
                event.getPlayer().teleport(location);
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent e) {
        if (e.getCause() == EntityDamageEvent.DamageCause.CONTACT) {
            e.setDamage(e.getDamage()/10);
            return;
        }
        if (e.getCause() != EntityDamageEvent.DamageCause.VOID)
            e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerMoveInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerUse(PlayerInteractEvent event){
        if(event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.ACACIA_DOOR) {
            return;
        }
        Player p = event.getPlayer();
        if(server.state == ServerParkour.GAME_INPROGRESS && event.getMaterial() == Material.PAPER) {
            p.setHealth(0);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Location location = event.getRespawnLocation();
        location.setYaw(90);
        event.setRespawnLocation(location);
    }

}
