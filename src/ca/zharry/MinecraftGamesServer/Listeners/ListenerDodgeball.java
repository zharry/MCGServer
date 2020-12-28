package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Players.PlayerDodgeball;
import ca.zharry.MinecraftGamesServer.Servers.ServerDodgeball;
import ca.zharry.MinecraftGamesServer.Utils.PlayerUtils;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.UUID;

public class ListenerDodgeball implements Listener {

    ServerDodgeball server;

    public ListenerDodgeball(ServerDodgeball server) {
        this.server = server;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerDodgeball playerDodgeball = new PlayerDodgeball(player, server);
        server.addPlayer(playerDodgeball);

        PlayerUtils.resetPlayer(player, GameMode.ADVENTURE);
        player.teleport(server.serverSpawn);
        player.setBedSpawnLocation(server.serverSpawn, true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        for (int i = 0; i < server.players.size(); i++) {
            PlayerDodgeball curPlayer = (PlayerDodgeball) server.players.get(i);
            if (curPlayer.bukkitPlayer.getUniqueId() == player.getUniqueId()) {
                curPlayer.commit();
                server.players.remove(i);
                break;
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (server.state == ServerDodgeball.GAME_INPROGRESS) {
            event.getDrops().removeIf(drop -> drop.getType() != Material.ARROW);

            Player k = event.getEntity().getPlayer().getKiller();
            Player d = event.getEntity().getPlayer();
            if (k.getUniqueId() == d.getUniqueId())
                return;
            PlayerDodgeball killer = (PlayerDodgeball) server.playerLookup.get(k.getUniqueId());
            PlayerDodgeball dead = (PlayerDodgeball) server.playerLookup.get(d.getUniqueId());
            dead.lastDeathLocation = dead.bukkitPlayer.getLocation();

            // If the killer is dead or dead is already dead
            if (killer.lives <= 0 || dead.lives <= 0)
                return;

            // Award the killer
            killer.kills++;
            killer.totalKills++;
            killer.currentScore += 50;
            MCGTeam killerTeam = killer.myTeam;
            MCGTeam deadTeam = dead.myTeam;
            boolean myTeamIsAllDead = true;

            // Take a life away from the dead
            dead.lives--;

            // Give everyone some user feedback
            ArrayList<UUID> gamePlayers = new ArrayList<>();
            gamePlayers.addAll(deadTeam.players);
            gamePlayers.addAll(killerTeam.players);
            for (UUID uuid : gamePlayers) {
                PlayerDodgeball playerDodgeball = (PlayerDodgeball) server.playerLookup.get(uuid);
                if (playerDodgeball != null) {
                    playerDodgeball.bukkitPlayer.sendMessage(ChatColor.RESET + "" + ChatColor.BOLD + " [+50] " +
                            killer.bukkitPlayer.getDisplayName() + "" + ChatColor.RESET + " killed " + dead.bukkitPlayer.getDisplayName());
                }
            }
            dead.bukkitPlayer.sendTitle("You died!", "You have " + dead.lives + " lives remaining!", 0, 40, 20);


            // Check if my entire team is dead
            if (dead.lives <= 0) {
                for (UUID uuid : deadTeam.players) {
                    PlayerDodgeball teammate = (PlayerDodgeball) server.playerLookup.get(uuid);
                    if (teammate != null && teammate.lives > 0) {
                        myTeamIsAllDead = false;
                    }
                }
            } else {
                return;
            }

            // Round victory logic
            if (myTeamIsAllDead) {
                // Give all players a notice that a team has won
                server.sendMessageAll(" \n" + killerTeam.chatColor + "" + killerTeam.teamname + "" + ChatColor.RESET + " " +
                        "has defeated " + deadTeam.chatColor + "" + deadTeam.teamname + " \n ");

                // Award Enemy Team victory scores
                for (UUID uuid : killerTeam.players) {
                    PlayerDodgeball playerDodgeball = (PlayerDodgeball) server.playerLookup.get(uuid);
                    if (playerDodgeball != null) {
                        playerDodgeball.currentScore += 250;
                        playerDodgeball.bukkitPlayer.setGameMode(GameMode.SPECTATOR);
                        playerDodgeball.bukkitPlayer.sendTitle(ChatColor.GREEN + "Victory!", "You and your teammates have received 250 points each!", 10, 60, 10);
                    }
                }

                // Teleport losing team to spawn
                for (UUID uuid : deadTeam.players) {
                    PlayerDodgeball playerDodgeball = (PlayerDodgeball) server.playerLookup.get(uuid);
                    if (playerDodgeball != null) {
                        playerDodgeball.bukkitPlayer.setGameMode(GameMode.SPECTATOR);
                        playerDodgeball.bukkitPlayer.sendTitle(ChatColor.RED + "Defeat!", "Your team has been eliminated by " + killerTeam.chatColor + "" + killerTeam.teamname, 0, 60, 10);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if(server.state == ServerDodgeball.GAME_INPROGRESS) {
            if(7 < event.getTo().getZ() && event.getTo().getZ() < 41) {
                PlayerDodgeball playerDodgeball = ((PlayerDodgeball) server.playerLookup.get(event.getPlayer().getUniqueId()));
                playerDodgeball.invulnerable = false;
                playerDodgeball.bukkitPlayer.setInvisible(false);
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (server.state == ServerDodgeball.GAME_INPROGRESS) {
            if (event.getEntityType() == EntityType.PLAYER && ((PlayerDodgeball) server.playerLookup.get(((Player) event.getEntity()).getUniqueId())).invulnerable) {
                event.setCancelled(true);
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        if (event.getItemDrop().getItemStack().getType() == Material.BOW) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (server.state == ServerDodgeball.GAME_INPROGRESS) {
            PlayerDodgeball player = (PlayerDodgeball) server.playerLookup.get(event.getPlayer().getUniqueId());
            if (player.lives <= 0) {
                if(player.lastDeathLocation != null) {
                    event.setRespawnLocation(player.lastDeathLocation);
                    player.bukkitPlayer.setInvisible(true);
                    new BukkitRunnable() {
                        public void run() {
                            player.bukkitPlayer.setInvisible(false);
                            player.bukkitPlayer.setGameMode(GameMode.SPECTATOR);
                        }
                    }.runTaskLater(server.plugin, 1);
                }
            } else {
                server.giveBow(player);
                player.invulnerable = true;
                player.bukkitPlayer.setInvisible(true);
            }
        }
    }
}
