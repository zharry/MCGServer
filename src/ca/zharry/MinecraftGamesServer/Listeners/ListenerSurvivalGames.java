package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Players.PlayerSurvivalGames;
import ca.zharry.MinecraftGamesServer.Servers.ServerSpleef;
import ca.zharry.MinecraftGamesServer.Servers.ServerSurvivalGames;
import ca.zharry.MinecraftGamesServer.Utils.Coord3D;
import ca.zharry.MinecraftGamesServer.Utils.PlayerUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

public class ListenerSurvivalGames implements Listener {

    ServerSurvivalGames server;

    public ListenerSurvivalGames(ServerSurvivalGames server) {
        this.server = server;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerSurvivalGames playerSurvivalGames = new PlayerSurvivalGames(player, server);
        server.addPlayer(playerSurvivalGames);

        if (server.state == ServerSpleef.GAME_INPROGRESS) {
            if (player.getGameMode() == GameMode.SPECTATOR) {
                playerSurvivalGames.dead = true;
            } else {
                player.setGameMode(GameMode.SURVIVAL);
            }
        } else {
            player.teleport(server.serverSpawn);
            player.setBedSpawnLocation(server.serverSpawn, true);
            PlayerUtils.resetPlayer(player, GameMode.SURVIVAL);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (server.state == ServerSurvivalGames.GAME_INPROGRESS) {
            // Find the dead player
            Player deadPlayer = event.getEntity();
            PlayerSurvivalGames player = (PlayerSurvivalGames) server.playerLookup.get(deadPlayer.getUniqueId());

            // Mark them as dead and upload their score
            player.dead = true;
            player.commit();
            MCGMain.logger.info(player.bukkitPlayer.getName() + " DEAD");
            player.bukkitPlayer.sendTitle(ChatColor.RED + "You died!", "", 10, 70, 20);

            // Award all non-dead players some score
            for (PlayerInterface p : server.players) {
                PlayerSurvivalGames playerSurvivalGames = (PlayerSurvivalGames) p;
                if (!playerSurvivalGames.dead) {
                    playerSurvivalGames.currentScore += 25;
                    playerSurvivalGames.bukkitPlayer.sendMessage(ChatColor.RESET + "" + ChatColor.BOLD + " [+25]" +
                            ChatColor.RESET + " survival points, " + deadPlayer.getDisplayName() + " has died!");
                }
            }

            // Award killer some score
            Player k = event.getEntity().getPlayer().getKiller();
            if (k != null) {
                PlayerSurvivalGames killer = (PlayerSurvivalGames) server.playerLookup.get(k.getUniqueId());
                killer.kills += 1;
                killer.currentScore += 50;
                killer.bukkitPlayer.sendMessage(ChatColor.RESET + "" + ChatColor.BOLD + " [+50]" +
                        ChatColor.RESET + " for killing " + deadPlayer.getDisplayName());
                player.bukkitPlayer.sendTitle(ChatColor.RED + "You died!", "Killed by " + killer.bukkitPlayer.getDisplayName(), 10, 70, 20);
            }
            player.deathLocation = deadPlayer.getLocation();
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        player.getInventory().clear();
        PlayerSurvivalGames survivalGamesPlayer = (PlayerSurvivalGames) server.playerLookup.get(player.getUniqueId());
        if (survivalGamesPlayer != null && survivalGamesPlayer.deathLocation != null) {
            event.setRespawnLocation(survivalGamesPlayer.deathLocation);
        }

        player.setInvisible(true);

        new BukkitRunnable() {
            public void run() {
                player.setGameMode(GameMode.SPECTATOR);
                player.setInvisible(false);
            }
        }.runTaskLater(server.plugin, 1);
    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        for (int i = 0; i < server.players.size(); i++) {
            PlayerSurvivalGames curPlayer = (PlayerSurvivalGames) server.players.get(i);
            if (curPlayer.bukkitPlayer.getUniqueId() == player.getUniqueId()) {
                curPlayer.commit();
                server.players.remove(i);
                break;
            }
        }
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
                TNTPrimed tnt = server.world.spawn(location, TNTPrimed.class);
                tnt.setSource(event.getPlayer());
            } else if (block.getType() == Material.COBWEB) {
            } else if (block.getType() == Material.FIRE) {
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
        if (server.state == ServerSurvivalGames.GAME_INPROGRESS || server.state == ServerSurvivalGames.GAME_FINISHED) {
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerOpenInventory(InventoryOpenEvent event) {
        if (server.state == ServerSurvivalGames.GAME_INPROGRESS) {
            Inventory inventory = event.getInventory();
            if (inventory.getType() == InventoryType.CHEST) {
                Location location = inventory.getLocation();
                Player player = ((Player) event.getPlayer());
                Coord3D coord = new Coord3D((int) location.getX(), (int) location.getY(), (int) location.getZ());
                if (player.getGameMode() == GameMode.SURVIVAL) {
                    server.openedChests.add(coord);
                } else if (player.getGameMode() == GameMode.SPECTATOR) {
                    if (!server.openedChests.contains(coord)) {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("Chest has not yet been opened"));
                        event.setCancelled(true);
                    }
                }
            }
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
            } else if (block.getType() == Material.FIRE) {
            } else {
                event.setCancelled(true);
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().clear();
    }

    @EventHandler
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (server.state != ServerSurvivalGames.GAME_INPROGRESS) {
            event.setCancelled(true);
        }
    }

    // Prevent player from moving
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (server.state == ServerSurvivalGames.GAME_BEGIN) {
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

    // Store chest locations on load
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        BlockState[] states = event.getChunk().getTileEntities();
        for (BlockState state : states) {
            if (state.getType() == Material.CHEST) {
                server.addChest(new Coord3D(state.getX(), state.getY(), state.getZ()));
            }
        }
    }

    // Force load all chunks
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        for (int x = -16; x < 16; ++x) {
            for (int z = -16; z < 16; ++z) {
                server.world.setChunkForceLoaded(x, z, true);
            }
        }
    }

}
