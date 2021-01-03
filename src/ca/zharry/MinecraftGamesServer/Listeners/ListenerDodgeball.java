package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Players.PlayerDodgeball;
import ca.zharry.MinecraftGamesServer.Servers.ServerDodgeball;
import ca.zharry.MinecraftGamesServer.Servers.ServerParkour;
import ca.zharry.MinecraftGamesServer.Utils.PlayerUtils;
import ca.zharry.MinecraftGamesServer.Utils.Point3D;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ListenerDodgeball implements Listener {

    ServerDodgeball server;

    public ListenerDodgeball(ServerDodgeball server) {
        this.server = server;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerUtils.resetPlayer(player, GameMode.ADVENTURE);
        player.teleport(server.serverSpawn);
        player.setBedSpawnLocation(server.serverSpawn, true);

        // Practice Mode
        if (server.state == ServerDodgeball.GAME_WAITING) {
            server.givePracticeModeSelect(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        PlayerDodgeball player = (PlayerDodgeball) server.getPlayerFromUUID(p.getUniqueId());
        // Practice Mode
        if (server.state == ServerParkour.GAME_WAITING) {
            if (player.arena != -1) {
                server.practiceArenaNum[player.arena]--;
                player.arena = -1;
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

        // Practice Mode
        if (server.state == ServerDodgeball.GAME_WAITING) {
            event.getDrops().removeIf(drop -> drop.getType() != Material.ARROW);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (server.state == ServerDodgeball.GAME_INPROGRESS) {
            if (7 < event.getTo().getZ() && event.getTo().getZ() < 41) {
                PlayerDodgeball playerDodgeball = ((PlayerDodgeball) server.playerLookup.get(event.getPlayer().getUniqueId()));
                playerDodgeball.invulnerable = false;
                playerDodgeball.bukkitPlayer.setInvisible(false);
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        PlayerDodgeball playerDodgeball = (PlayerDodgeball) server.playerLookup.get(((Player) event.getEntity()).getUniqueId());
        if (server.state == ServerDodgeball.GAME_INPROGRESS) {
            if (event.getEntityType() == EntityType.PLAYER && playerDodgeball.invulnerable) {
                event.setCancelled(true);
            }
        }
        // Practice Mode
        else if (server.state == ServerDodgeball.GAME_WAITING) {
            if (playerDodgeball.arena == -1) {
                event.setCancelled(true);
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        if (event.getItemDrop().getItemStack().getType() == Material.BOW ||
                event.getItemDrop().getItemStack().getType() == Material.TARGET) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        PlayerDodgeball player = (PlayerDodgeball) server.playerLookup.get(event.getPlayer().getUniqueId());
        if (server.state == ServerDodgeball.GAME_INPROGRESS) {
            if (player.lives <= 0) {
                if (player.lastDeathLocation != null) {
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

        // Practice Mode
        if (server.state == ServerDodgeball.GAME_WAITING) {
            server.giveBow(player);
            server.givePracticeModeSelect(player.bukkitPlayer);
        }
    }


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        PlayerDodgeball player = (PlayerDodgeball) server.getPlayerFromUUID(p.getUniqueId());
        Block block = event.getClickedBlock();

        // Practice Mode - Arena Select Signs
        if (server.state == ServerParkour.GAME_WAITING && block != null && block.getType() == Material.BIRCH_WALL_SIGN) {
            String signLine = ChatColor.stripColor(((Sign) block.getState()).getLine(1));
            if (!signLine.startsWith("Join "))
                return;

            // Find arena
            Block arenaLabel = null;
            if (signLine.startsWith("Join ") && signLine.endsWith(" Red")) {
                arenaLabel = server.world.getBlockAt(block.getX(), block.getY() + 1, block.getZ());
            } else if (signLine.startsWith("Join ") && signLine.endsWith(" Blue")) {
                arenaLabel = server.world.getBlockAt(block.getX(), block.getY() + 2, block.getZ());
            }

            // Join arena
            if (arenaLabel != null) {
                int arenaNo = -1;
                String arenaLine = ChatColor.stripColor(((Sign) arenaLabel.getState()).getLine(0));
                if (arenaLine.startsWith("Arena ") && arenaLine.endsWith(":")) {
                    arenaNo = Integer.parseInt(arenaLine.split(":")[0].split(" ")[1]);
                }

                // Teleport to arena
                server.practiceArenaNum[arenaNo]++;
                Point3D redSpawnLocation = server.arenaSpawns.get(arenaNo - 1);
                Location redSpawn = new Location(server.world,
                        redSpawnLocation.getX(), redSpawnLocation.getY(), redSpawnLocation.getZ());
                Point3D blueSpawnLocation = server.arenaSpawns.get(arenaNo - 1).add(0, 0, 45);
                Location blueSpawn = new Location(server.world,
                        blueSpawnLocation.getX(), blueSpawnLocation.getY(), blueSpawnLocation.getZ());

                player.arena = arenaNo;
                if (signLine.startsWith("Join ") && signLine.endsWith(" Red")) {
                    PlayerUtils.resetPlayer(player.bukkitPlayer, GameMode.ADVENTURE);
                    server.giveBow(player);
                    server.givePracticeModeSelect(player.bukkitPlayer);
                    player.bukkitPlayer.teleport(redSpawn);
                    player.bukkitPlayer.setBedSpawnLocation(redSpawn, true);

                } else if (signLine.startsWith("Join ") && signLine.endsWith(" Blue")) {
                    PlayerUtils.resetPlayer(player.bukkitPlayer, GameMode.ADVENTURE);
                    server.giveBow(player);
                    server.givePracticeModeSelect(player.bukkitPlayer);
                    player.bukkitPlayer.teleport(blueSpawn);
                    player.bukkitPlayer.setBedSpawnLocation(blueSpawn, true);
                }
            }

            event.setCancelled(true);
            return;
        }

        // Practice Mode - Arena Select
        if (server.state == ServerParkour.GAME_WAITING && event.getMaterial() == Material.TARGET) {
            if (player.arena != -1) {
                server.practiceArenaNum[player.arena]--;
                player.arena = -1;

                // Remove all lingering item drops
                List<Entity> entList = server.world.getEntities();
                for (Entity current : entList) {
                    if (current instanceof Item || current instanceof Arrow) {
                        current.remove();
                    }
                }
            }

            PlayerUtils.resetPlayer(player.bukkitPlayer, GameMode.ADVENTURE);
            server.givePracticeModeSelect(player.bukkitPlayer);
            p.teleport(server.practiceArenaSelect);
            p.setBedSpawnLocation(server.practiceArenaSelect, true);
            event.setCancelled(true);
            return;
        }
    }
}
