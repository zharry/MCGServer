package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Players.PlayerSurvivalGames;
import ca.zharry.MinecraftGamesServer.Servers.ServerSpleef;
import ca.zharry.MinecraftGamesServer.Servers.ServerSurvivalGames;
import ca.zharry.MinecraftGamesServer.Utils.Coord3D;
import ca.zharry.MinecraftGamesServer.Utils.PlayerUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
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
        PlayerSurvivalGames playerSurvivalGames = (PlayerSurvivalGames) server.playerLookup.get(player.getUniqueId());

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
            player.bukkitPlayer.sendTitle(ChatColor.RED + "You died!", "", 10, 70, 20);

            // Award all non-dead players some score
            for (PlayerInterface p : server.players) {
                PlayerSurvivalGames playerSurvivalGames = (PlayerSurvivalGames) p;
                if (!playerSurvivalGames.dead) {
                    playerSurvivalGames.addScore(100, deadPlayer.getName() + " has died!");
                    playerSurvivalGames.bukkitPlayer.sendMessage(ChatColor.RESET + "" + ChatColor.BOLD + " [+100]" +
                            ChatColor.RESET + " survival points, " + deadPlayer.getDisplayName() + " has died!");
                }
            }

            // Award killer some score
            Player k = event.getEntity().getPlayer().getKiller();
            if (k != null) {
                PlayerSurvivalGames killer = (PlayerSurvivalGames) server.playerLookup.get(k.getUniqueId());
                killer.kills += 1;
                killer.addScore(200, "killed " + deadPlayer.getName());
                killer.bukkitPlayer.sendMessage(ChatColor.RESET + "" + ChatColor.BOLD + " [+200]" +
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
        if(event.getAction() == Action.PHYSICAL) {
            return;
        }

        if (server.state == ServerSurvivalGames.GAME_INPROGRESS || server.state == ServerSurvivalGames.GAME_FINISHED) {
        }
        // Practice Mode
        else if (server.state == ServerSurvivalGames.GAME_WAITING) {
            Player p = event.getPlayer();
            PlayerSurvivalGames playerSurvivalGames = (PlayerSurvivalGames) server.getPlayerFromUUID(p.getUniqueId());
            Block block = event.getClickedBlock();

            // Practice Mode Signs
            if (block != null && block.getType() == Material.BIRCH_WALL_SIGN) {
                String signLine1 = ChatColor.stripColor(((Sign) block.getState()).getLine(1));
                String signLine2 = ChatColor.stripColor(((Sign) block.getState()).getLine(2));

                // Send to Arena
                if (signLine1.equals("Enter") && signLine2.equals("Arena")) {
                    PlayerUtils.resetPlayer(playerSurvivalGames.bukkitPlayer, GameMode.ADVENTURE);
                    playerSurvivalGames.bukkitPlayer.teleport(new Location(server.world, 0.5, 75.5, 0.5, -90, 0));
                    playerSurvivalGames.bukkitPlayer.setBedSpawnLocation(new Location(server.world, 0.5, 75.5, 0.5, -90, 0), true);
                }
            }
            event.setCancelled(true);
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
                    if (!server.openedChests.contains(coord)) {
                        server.openedChests.add(coord);
                        ServerSurvivalGames.SpecialItem specialItem = server.specialChests.get(coord);
                        if(specialItem != null) {
                            server.sendMessageAll(server.playerLookup.get(player.getUniqueId()).bukkitPlayer.getDisplayName() + " has opened the chest " + specialItem.location + " containing " + specialItem.item.getItemMeta().getDisplayName());
                        }
                    }
                } else if (player.getGameMode() == GameMode.SPECTATOR) {
                    if (!server.openedChests.contains(coord)) {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("Chest has not yet been opened"));
                        event.setCancelled(true);
                    }
                }
            }
        }

        // Practice Mode
        if (server.state == ServerSurvivalGames.GAME_WAITING) {
            PlayerInterface player = server.playerLookup.get(event.getPlayer().getUniqueId());
            // Override for tutorial cutscene
            if(player.cutscene != null) {
                return;
            }

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

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent e) {
        if (server.state == ServerSurvivalGames.GAME_WAITING) {
            if (e.getFoodLevel() < 20) {
                e.setFoodLevel(20);
            }
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

}
