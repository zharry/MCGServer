package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Players.PlayerDodgeball;
import ca.zharry.MinecraftGamesServer.Servers.ServerDodgeball;
import ca.zharry.MinecraftGamesServer.Servers.ServerParkour;
import ca.zharry.MinecraftGamesServer.Utils.PlayerUtils;
import ca.zharry.MinecraftGamesServer.Utils.Point3D;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

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

        PlayerDodgeball playerDodgeball = (PlayerDodgeball) server.getPlayerFromUUID(player.getUniqueId());
        playerDodgeball.inSpawn = false;
        playerDodgeball.lives = 0;

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
            String deathMessage = "";

            // Find the person who died and their team
            Player d = event.getEntity().getPlayer();
            PlayerDodgeball dead = (PlayerDodgeball) server.playerLookup.get(d.getUniqueId());
            MCGTeam myTeam = dead.myTeam;
            MCGTeam opponentTeam = dead.opponentTeam;

            // If the dead is was already dead
            if (dead.lives <= 0)
                return;

            Player k = event.getEntity().getPlayer().getKiller();
            try {
                if (k.getUniqueId() == d.getUniqueId())
                    return;
                // Killer is a player
                PlayerDodgeball killer = (PlayerDodgeball) server.playerLookup.get(k.getUniqueId());

                // If the killer is dead
                if (killer.lives <= 0)
                    return;

                // Award the killer
                killer.kills++;
                killer.totalKills++;
                killer.addScore(125, "killed " + dead.name);

                // Set the death message
                deathMessage = ChatColor.RESET + "" + ChatColor.BOLD + " [+125] " +
                        killer.bukkitPlayer.getDisplayName() + "" + ChatColor.RESET + " killed " + dead.bukkitPlayer.getDisplayName();

            } catch (NullPointerException e) {
                // Killer does not exist
                deathMessage = ChatColor.RESET + "" + dead.bukkitPlayer.getDisplayName() + " died!";
            }

            // Take a life away from the dead
            dead.lives--;
            dead.lastDeathLocation = dead.bukkitPlayer.getLocation();

            // Give everyone who in this arena user feedback
            ArrayList<UUID> gamePlayers = new ArrayList<>();
            gamePlayers.addAll(myTeam.players);
            gamePlayers.addAll(opponentTeam.players);
            for (UUID uuid : gamePlayers) {
                PlayerDodgeball playerDodgeball = (PlayerDodgeball) server.playerLookup.get(uuid);
                if (playerDodgeball != null) {
                    playerDodgeball.bukkitPlayer.sendMessage(deathMessage);
                }
            }

            // Check if my entire team is dead
            boolean myTeamIsAllDead = true;
            if (dead.lives <= 0) {
                for (UUID uuid : myTeam.players) {
                    PlayerDodgeball teammate = (PlayerDodgeball) server.playerLookup.get(uuid);
                    if (teammate != null && teammate.lives > 0) {
                        myTeamIsAllDead = false;
                    }
                }
            } else {
                return;
            }

            // Tell the dead guy, he's dead
            if (!myTeamIsAllDead) {
                dead.bukkitPlayer.sendTitle("You died!", "You have " + dead.lives + " lives remaining!", 0, 40, 20);
            }

            // Arena victory logic
            if (myTeamIsAllDead) {
                // Give all players a notice that a team has won
                server.sendMessageAll(" \n" + opponentTeam.chatColor + "" + opponentTeam.teamname + "" + ChatColor.RESET + " " +
                        "has defeated " + myTeam.chatColor + "" + myTeam.teamname + ChatColor.RESET + ", each team member still alive has received an additional 250 points!\n ");

                // Award Enemy Team victory scores and tell them that they won
                for (UUID uuid : opponentTeam.players) {
                    PlayerDodgeball playerDodgeball = (PlayerDodgeball) server.playerLookup.get(uuid);
                    if (playerDodgeball != null) {
                        if (playerDodgeball.lives > 0) {
                            playerDodgeball.addScore(250, opponentTeam.teamname + "defeated " + myTeam.teamname);
                            playerDodgeball.bukkitPlayer.setGameMode(GameMode.SPECTATOR);
                            playerDodgeball.bukkitPlayer.sendTitle(ChatColor.GREEN + "Victory!", "You have received 250 additional points!", 10, 60, 10);
                        } else {
                            playerDodgeball.addScore(0, opponentTeam.teamname + "defeated " + myTeam.teamname + " (dead)");
                            playerDodgeball.bukkitPlayer.setGameMode(GameMode.SPECTATOR);
                            playerDodgeball.bukkitPlayer.sendTitle(ChatColor.GREEN + "Victory!", "Your team has won the round!", 10, 60, 10);
                        }
                    }
                }

                // Tell the losing team that they lost
                for (UUID uuid : myTeam.players) {
                    PlayerDodgeball playerDodgeball = (PlayerDodgeball) server.playerLookup.get(uuid);
                    if (playerDodgeball != null) {
                        playerDodgeball.bukkitPlayer.setGameMode(GameMode.SPECTATOR);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                playerDodgeball.bukkitPlayer.sendTitle(ChatColor.RED + "Defeat!", "Your team has been eliminated by " + opponentTeam.chatColor + "" + opponentTeam.teamname, 0, 60, 10);
                            }
                        }.runTaskLater(server.plugin, 1);
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
            PlayerDodgeball playerDodgeball = ((PlayerDodgeball) server.playerLookup.get(event.getPlayer().getUniqueId()));

            // Once a player enters the play area, they will no longer be invincible
            if (7 < event.getTo().getZ() && event.getTo().getZ() < 41 && playerDodgeball.bukkitPlayer.getGameMode() == GameMode.ADVENTURE) {
                playerDodgeball.invulnerable = false;
                playerDodgeball.inSpawn = false;
                playerDodgeball.spawnTimer = ServerDodgeball.SPAWN_TIMER;
                playerDodgeball.bukkitPlayer.setInvisible(false);
            }

            // If a player re-enters spawn,
            if ((7 > event.getTo().getZ() || event.getTo().getZ() > 41) && playerDodgeball.bukkitPlayer.getGameMode() == GameMode.ADVENTURE) {
                playerDodgeball.inSpawn = true;
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        PlayerDodgeball playerDodgeball = (PlayerDodgeball) server.playerLookup.get(((Player) event.getEntity()).getUniqueId());
        if (server.state == ServerDodgeball.GAME_INPROGRESS) {
            if (event.getEntityType() == EntityType.PLAYER && playerDodgeball.invulnerable && event.getCause() != EntityDamageEvent.DamageCause.CUSTOM) {
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
                    //player.bukkitPlayer.setInvisible(true);
                    new BukkitRunnable() {
                        public void run() {
                            //player.bukkitPlayer.setInvisible(false);
                            player.bukkitPlayer.setGameMode(GameMode.SPECTATOR);
                        }
                    }.runTaskLater(server.plugin, 1);
                }
            } else {
                server.giveBow(player);
                player.invulnerable = true;
                player.inSpawn = true;
                player.spawnTimer = ServerDodgeball.SPAWN_TIMER;
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
                server.playerColor.put(player.bukkitPlayer.getUniqueId(), true);
            } else if (signLine.startsWith("Join ") && signLine.endsWith(" Blue")) {
                arenaLabel = server.world.getBlockAt(block.getX(), block.getY() + 2, block.getZ());
                server.playerColor.put(player.bukkitPlayer.getUniqueId(), false);
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

    private Map<Projectile, BukkitTask> tasks = new HashMap<>();
    private Random random = new Random();

    @EventHandler
    public void onShoot(EntityShootBowEvent e){
        if(!(e.getEntity() instanceof Player)){
            return;
        }
        Arrow a = (Arrow) e.getProjectile();
        a.setCritical(false);
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent e) {
        if (e.getEntity().getShooter() instanceof Player) {
            tasks.put(e.getEntity(), new BukkitRunnable() {
                @Override
                public void run() {
                    Projectile entity = e.getEntity();
                    World w = entity.getWorld();
                    Location l = entity.getLocation();
                    Player p = (Player) e.getEntity().getShooter();
                    boolean isRed = server.playerColor.containsKey(p.getUniqueId()) && server.playerColor.get(p.getUniqueId());
                    Particle particle = isRed ? Particle.LAVA : Particle.WATER_SPLASH;
                    int iterations = isRed ? 2 : 8;
                    for(int k = 0; k < iterations; ++k) {
                        l.subtract(entity.getVelocity().multiply(random.nextDouble() * 0.05));
                        w.spawnParticle(particle, l, 1);
                        if (!isRed) {
                            if (k == 0) {
                                w.spawnParticle(Particle.WATER_WAKE, l, 2);
                            }
                        }
                    }
                }
            }.runTaskTimer(this.server.plugin, 0L, 1L));
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        if (e.getEntity().getShooter() instanceof Player) {
            BukkitTask task = tasks.get(e.getEntity());
            if (task != null) {
                task.cancel();
                tasks.remove(e.getEntity());
            }
        }
    }
}
