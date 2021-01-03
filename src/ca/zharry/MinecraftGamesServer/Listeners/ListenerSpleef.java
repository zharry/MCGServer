package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Players.PlayerSpleef;
import ca.zharry.MinecraftGamesServer.Servers.ServerParkour;
import ca.zharry.MinecraftGamesServer.Servers.ServerSpleef;
import ca.zharry.MinecraftGamesServer.Utils.PlayerUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;

public class ListenerSpleef implements Listener {
    ServerSpleef server;

    public ListenerSpleef(ServerSpleef server) {
        this.server = server;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerSpleef playerSpleef = (PlayerSpleef) server.playerLookup.get(player.getUniqueId());

        if (server.state == ServerSpleef.GAME_WAITING ||
                server.state == ServerSpleef.GAME_STARTING ||
                server.state == ServerSpleef.GAME_FINISHED) {
            player.teleport(server.serverSpawn);
            player.setGameMode(GameMode.ADVENTURE);
        }
        if (server.state == ServerSpleef.GAME_INPROGRESS) {
            if (player.getLocation().getY() >= ServerSpleef.COMPETITION_MAX_HEIGHT) {
                playerSpleef.dead = true;
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (server.state == ServerSpleef.GAME_INPROGRESS) {
            // Find the dead player
            Player deadPlayer = event.getEntity();
            PlayerSpleef player = (PlayerSpleef) server.playerLookup.get(deadPlayer.getUniqueId());

            // Make sure that this player isn't giving duplicate scores
            if (deadPlayer.getGameMode() != GameMode.SURVIVAL)
                return;

            // Mark them as dead and upload their score
            player.dead = true;
            player.commit();

            // Award all non-dead players some score
            player.bukkitPlayer.sendTitle("You died!", "", 0, 30, 20);
            for (PlayerInterface p : server.players) {
                PlayerSpleef playerSpleef = (PlayerSpleef) p;
                if (!playerSpleef.dead) {
                    playerSpleef.currentScore += 100;
                    playerSpleef.bukkitPlayer.sendMessage(ChatColor.RESET + "" + ChatColor.BOLD + " [+100]" +
                            ChatColor.RESET + " survival points, " + deadPlayer.getDisplayName() + " has died!");
                } else {
                    playerSpleef.bukkitPlayer.sendMessage(ChatColor.RESET + "" + deadPlayer.getDisplayName() + " has died!");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        new BukkitRunnable() {
            public void run() {
                Player player = event.getPlayer();
                player.teleport(server.serverSpawn);
                if(server.state == ServerSpleef.GAME_INPROGRESS) {
                    PlayerUtils.resetPlayer(player, GameMode.SPECTATOR);
                } else {
                    PlayerUtils.resetPlayer(player, GameMode.ADVENTURE);
                }
            }
        }.runTaskLater(server.plugin, 1);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if(player.getLocation().getY() < ServerSpleef.COMPETITION_MIN_HEIGHT) {
            player.setHealth(0);
        }

        if (server.state == ServerSpleef.GAME_BEGIN) {
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
    public void onPlayerDestroy(BlockBreakEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }
        // Disallow players to destroy spawn platform if they can reach it
        if (event.getBlock().getLocation().getY() >= ServerSpleef.COMPETITION_MAX_HEIGHT) {
            event.setCancelled(true);
        }
        // Disallow players to break the arena before the game starts
        if (server.state == ServerSpleef.GAME_BEGIN) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> block.getLocation().getY() > ServerSpleef.COMPETITION_MAX_HEIGHT);
        server.world.spawnParticle(Particle.EXPLOSION_LARGE, event.getLocation(), 0);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if(event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if(event.getWhoClicked().getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        PlayerSpleef playerSpleef = (PlayerSpleef) server.getPlayerFromUUID(p.getUniqueId());
        Block block = event.getClickedBlock();

        // Practice Mode Signs
        if (server.state == ServerParkour.GAME_WAITING && block != null && block.getType() == Material.BIRCH_WALL_SIGN) {
            String signLine1 = ChatColor.stripColor(((Sign) block.getState()).getLine(1));
            String signLine2 = ChatColor.stripColor(((Sign) block.getState()).getLine(2));

            // Send to Arena
            if (signLine1.equals("Send to") && signLine2.equals("Arena")) {
                server.sendToArena(playerSpleef);
                event.setCancelled(true);
            }
            // Reset World
            if (signLine1.equals("Reset") && signLine2.equals("World")) {
                server.spleefRestore(server.world);
                event.setCancelled(true);
            }
        }
    }
}

