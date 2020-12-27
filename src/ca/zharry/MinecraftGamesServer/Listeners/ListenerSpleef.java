package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Players.PlayerSpleef;
import ca.zharry.MinecraftGamesServer.Servers.ServerSpleef;
import ca.zharry.MinecraftGamesServer.Utils.PlayerUtils;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
        PlayerSpleef playerSpleef = new PlayerSpleef(player, server);
        server.addPlayer(playerSpleef);

        if (server.state == ServerSpleef.GAME_WAITING ||
                server.state == ServerSpleef.GAME_STARTING ||
                server.state == ServerSpleef.GAME_FINISHED) {
            Location gameSpectate = new Location(player.getWorld(), 14.5, 75, 17.5);
            player.teleport(gameSpectate);
            player.setGameMode(GameMode.ADVENTURE);
        }
        if (server.state == ServerSpleef.GAME_INPROGRESS) {
            if (player.getLocation().getY() >= ServerSpleef.COMPETITION_MAX_HEIGHT) {
                playerSpleef.dead = true;
            }
        }
        player.setDisplayName(server.teams.get(server.teamLookup.get(player.getUniqueId())).chatColor + player.getName() + ChatColor.RESET);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        for (int i = 0; i < server.players.size(); i++) {
            PlayerSpleef curPlayer = (PlayerSpleef) server.players.get(i);
            if (curPlayer.bukkitPlayer.getUniqueId() == player.getUniqueId()) {
                curPlayer.commit();
                server.players.remove(i);
                break;
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (server.state == ServerSpleef.GAME_INPROGRESS) {
            // Find the dead player
            Player deadPlayer = event.getEntity();
            PlayerSpleef player = (PlayerSpleef) server.playerLookup.get(deadPlayer.getUniqueId());

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
                Location serverSpawn = new Location(player.getWorld(), 14.5, 75, 17.5);
                player.teleport(serverSpawn);
                if(server.state == ServerSpleef.GAME_INPROGRESS) {
                    PlayerUtils.resetPlayer(player, GameMode.SPECTATOR);
                } else {
                    PlayerUtils.resetPlayer(player, GameMode.ADVENTURE);
                }
            }
        }.runTaskLater(server.javaPlugin, 1);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if(event.getPlayer().getLocation().getY() < 36) {
            event.getPlayer().setHealth(0);
        }
    }

    // Prevent blocks above COMPETITION_MAX_HEIGHT from being destroyed by explosions
    @EventHandler
    public void onTntExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> block.getLocation().getY() > ServerSpleef.COMPETITION_MAX_HEIGHT);

        event.getLocation().getWorld().spawnParticle(Particle.EXPLOSION_LARGE, event.getLocation(), 0);
    }

    @EventHandler
    public void playerAccidentallyDroppedPickaxe(PlayerDropItemEvent event) {
        if(event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void playerTriesToClickInInventory(InventoryClickEvent event) {
        if(event.getWhoClicked().getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

}

