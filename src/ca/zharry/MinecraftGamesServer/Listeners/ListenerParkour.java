package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.Players.PlayerParkour;
import ca.zharry.MinecraftGamesServer.Servers.ServerParkour;
import ca.zharry.MinecraftGamesServer.Utils.Point3D;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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
        PlayerParkour playerParkour = (PlayerParkour) server.playerLookup.get(player.getUniqueId());
        System.out.println(server.playerLookup);
        player.setGameMode(GameMode.ADVENTURE);
        player.setInvisible(false);
        server.setPlayerInventoryContents(playerParkour);

        if (server.state == ServerParkour.GAME_WAITING) {
            player.teleport(server.serverSpawn);
            player.setBedSpawnLocation(server.serverSpawn, true);

        } else if (server.state == ServerParkour.GAME_STARTING) {
            Point3D checkpointLoc = server.stage1Checkpoints.get(0);
            Location firstCheckpoint = new Location(server.world, checkpointLoc.x, checkpointLoc.y, checkpointLoc.z);
            player.teleport(firstCheckpoint);
            player.setBedSpawnLocation(firstCheckpoint, true);

        } else if (server.state == ServerParkour.GAME_INPROGRESS) {
            // Teleport player to last checkpoint
            Point3D checkpointLoc = server.stage1Checkpoints.get(0);
            switch (playerParkour.stage) {
                case 1:
                    checkpointLoc = server.stage1Checkpoints.get(playerParkour.level);
                    break;
                case 2:
                    checkpointLoc = server.stage2Checkpoints.get(playerParkour.level);
                    break;
                case 3:
                    checkpointLoc = server.stage3Checkpoints.get(playerParkour.level);
                    break;
                case 4:
                    checkpointLoc = server.stage4Checkpoints.get(playerParkour.level);
                    break;
                case 5:
                    checkpointLoc = server.stage5Checkpoints.get(playerParkour.level);
                    break;
                case 6:
                    checkpointLoc = server.stage6Checkpoints.get(playerParkour.level);
                    break;
                default:
            }
            Location nextStage = new Location(server.world, checkpointLoc.getX() + 0.5, checkpointLoc.getY() + 1, checkpointLoc.getZ() + 0.5, 90, 0);
            player.teleport(nextStage);
            player.setInvisible(true);

        } else if (server.state == ServerParkour.GAME_FINISHED) {
            player.teleport(server.mapEnd);
            player.setBedSpawnLocation(server.mapEnd, true);
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
                Location location = new Location(server.world,
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
            e.setDamage(e.getDamage() / 10);
            return;
        }
        if (e.getCause() != EntityDamageEvent.DamageCause.VOID)
            e.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerItemUse(PlayerInteractEvent event) {
        // Disable farmland trampling
        if (event.getAction() == Action.PHYSICAL) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.ACACIA_DOOR) {
            return;
        }
        Player p = event.getPlayer();
        Block block = event.getClickedBlock();

        // Test for right click on sign
        if (server.state == ServerParkour.GAME_WAITING && block != null && block.getType() == Material.BIRCH_WALL_SIGN) {
            String level = ((Sign) block.getState()).getLine(1);
            try {
                String[] s = level.split("-");
                int stage = Integer.parseInt(s[0]);
                int checkpoint = Integer.parseInt(s[1]);
                Point3D p3d = server.allCheckpoints.get(stage - 1).get(checkpoint - 1);
                event.getPlayer().teleport(new Location(event.getClickedBlock().getWorld(), p3d.x + 0.5, p3d.y + 1, p3d.z + 0.5, 90, 0));
            } catch(Exception e) {
            }
            event.setCancelled(true);
            return;
        }

        if(server.state == ServerParkour.GAME_WAITING && event.getMaterial() == Material.TARGET) {
            p.teleport(new Location(p.getWorld(), -10000.5, 64, 0.5, 90, 0));
            event.setCancelled(true);
            return;
        }

        // Test for Checkpoint reset
        if ((server.state == ServerParkour.GAME_INPROGRESS || server.state == ServerParkour.GAME_WAITING) && event.getMaterial() == Material.PAPER) {
            p.setHealth(0);
            event.setCancelled(true);
            return;
        }

        // Test for disable Waypoints
        if (event.getMaterial() == Material.END_ROD) {
            PlayerParkour playerParkour = (PlayerParkour) server.getPlayerFromUUID(p.getUniqueId());
            if (playerParkour.waypointsEnabled) {
                server.disableWaypoints(playerParkour);
            } else {
                server.enableWaypoints(playerParkour);
            }
            playerParkour.waypointsEnabled = !playerParkour.waypointsEnabled;
            p.sendMessage("Waypoints have been " + (playerParkour.waypointsEnabled ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Location location = event.getRespawnLocation();
        location.setYaw(90);
        event.setRespawnLocation(location);
    }

}
