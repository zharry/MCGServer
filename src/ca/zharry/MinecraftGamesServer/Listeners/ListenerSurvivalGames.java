package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Players.PlayerSurvivalGames;
import ca.zharry.MinecraftGamesServer.Servers.ServerSpleef;
import ca.zharry.MinecraftGamesServer.Servers.ServerSurvivalGames;
import ca.zharry.MinecraftGamesServer.Utils.Coord3D;
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

public class ListenerSurvivalGames extends PlayerListenerAdapter<ServerSurvivalGames, PlayerSurvivalGames> {

    public ListenerSurvivalGames(ServerSurvivalGames server) {
        super(server, PlayerSurvivalGames.class);
    }

    @Override
    public void onJoin(PlayerSurvivalGames player, PlayerJoinEvent event) {
        if (server.state == ServerSpleef.GAME_INPROGRESS) {
            if (player.getGameMode() == GameMode.SPECTATOR) {
                player.dead = true;
            } else {
                player.setGameMode(GameMode.SURVIVAL);
            }
        } else {
            player.teleport(server.serverSpawn);
            player.bukkitPlayer.setBedSpawnLocation(server.serverSpawn, true);
            player.reset(GameMode.SURVIVAL);
        }
    }

    @Override
    public void onDeath(PlayerSurvivalGames player, PlayerDeathEvent event) {
        if (server.state == ServerSurvivalGames.GAME_INPROGRESS) {
            // Find the dead player
            if (player.dead)
                return;

            // Mark them as dead and upload their score
            player.dead = true;
            player.commit();
            player.bukkitPlayer.sendTitle(ChatColor.RED + "You died!", "", 10, 70, 20);

            // Award all non-dead players some score
            for (PlayerSurvivalGames playerSurvivalGames : server.players) {
                if (!playerSurvivalGames.dead) {
                    playerSurvivalGames.addScore(150, player.getDisplayName() + " has died!");
                    playerSurvivalGames.bukkitPlayer.sendMessage(ChatColor.RESET + "" + ChatColor.BOLD + " [+150]" +
                            ChatColor.RESET + " survival points, " + player.getDisplayName() + " has died!");
                }
            }

            // Award killer some score
            PlayerSurvivalGames killer = getPlayerInterface(player.bukkitPlayer.getKiller());
            if (killer != null) {
                killer.kills += 1;
                killer.addScore(400, "killed " + player.getDisplayName());
                killer.bukkitPlayer.sendMessage(ChatColor.RESET + "" + ChatColor.BOLD + " [+400]" +
                        ChatColor.RESET + " for killing " + player.getDisplayName());
                player.bukkitPlayer.sendTitle(ChatColor.RED + "You died!", "Killed by " + killer.getDisplayName(), 10, 70, 20);
            }
            player.deathLocation = player.getLocation();
        }
    }

    @Override
    public void onRespawn(PlayerSurvivalGames player, PlayerRespawnEvent event) {
        player.bukkitPlayer.getInventory().clear();
        if (player.deathLocation != null) {
            event.setRespawnLocation(player.deathLocation);
        }

        player.setGameMode(GameMode.SPECTATOR);
    }

    @Override
    public void onBlockPlace(PlayerSurvivalGames player, BlockPlaceEvent event) {
        if (server.state == ServerSurvivalGames.GAME_INPROGRESS) {
            Block block = event.getBlock();
            if (block.getType() == Material.TNT) {
                block.setType(Material.AIR);
                Location location = block.getLocation();
                location = location.add(0.5, 0, 0.5);
                TNTPrimed tnt = server.world.spawn(location, TNTPrimed.class);
                tnt.setSource(player.bukkitPlayer);
            } else if (block.getType() == Material.COBWEB) {
            } else if (block.getType() == Material.FIRE) {
            } else {
                event.setCancelled(true);
            }
        } else {
            event.setCancelled(true);
        }
    }

    @Override
    public void onInteract(PlayerSurvivalGames players, PlayerInteractEvent event) {
        if(event.getAction() == Action.PHYSICAL) {
            return;
        }

        if (server.state == ServerSurvivalGames.GAME_INPROGRESS || server.state == ServerSurvivalGames.GAME_FINISHED) {
        } else if (server.state == ServerSurvivalGames.GAME_WAITING) {
            // Practice Mode
            Player p = event.getPlayer();
            PlayerSurvivalGames playerSurvivalGames = server.getPlayerFromUUID(p.getUniqueId());
            Block block = event.getClickedBlock();

            // Practice Mode Signs
            if (block != null && block.getType() == Material.BIRCH_WALL_SIGN) {
                String signLine1 = ChatColor.stripColor(((Sign) block.getState()).getLine(1));
                String signLine2 = ChatColor.stripColor(((Sign) block.getState()).getLine(2));

                // Send to Arena
                if (signLine1.equals("Enter") && signLine2.equals("Arena")) {
                    playerSurvivalGames.reset(GameMode.ADVENTURE);
                    playerSurvivalGames.teleport(new Location(server.world, 0.5, 75.5, 0.5, -90, 0));
                    playerSurvivalGames.bukkitPlayer.setBedSpawnLocation(new Location(server.world, 0.5, 75.5, 0.5, -90, 0), true);
                }
            }
            event.setCancelled(true);
        } else {
            event.setCancelled(true);
        }
    }

    @Override
    public void onInventoryOpen(PlayerSurvivalGames player, InventoryOpenEvent event) {
        if (server.state == ServerSurvivalGames.GAME_INPROGRESS) {
            Inventory inventory = event.getInventory();
            if (inventory.getType() == InventoryType.CHEST) {
                Location location = inventory.getLocation();
                Coord3D coord = new Coord3D((int) location.getX(), (int) location.getY(), (int) location.getZ());
                if (player.getGameMode() == GameMode.SURVIVAL) {
                    if (!server.openedChests.contains(coord)) {
                        server.openedChests.add(coord);
                        ServerSurvivalGames.SpecialItem specialItem = server.specialChests.get(coord);
                        if(specialItem != null) {
                            //server.sendMessageAll(server.playerLookup.get(player.getUniqueId()).bukkitPlayer.getDisplayName() + " has opened the chest " + specialItem.location + " containing " + specialItem.item.getItemMeta().getDisplayName());
                        }
                    }
                } else if (player.getGameMode() == GameMode.SPECTATOR) {
                    if (!server.openedChests.contains(coord)) {
                        player.bukkitPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("Chest has not yet been opened"));
                        event.setCancelled(true);
                    }
                }
            }
        }

        // Practice Mode
        if (server.state == ServerSurvivalGames.GAME_WAITING) {
            // Override for tutorial cutscene
            if(player.cutscene != null) {
                return;
            }

            event.setCancelled(true);
        }
    }

    @Override
    public void onBlockBreak(PlayerSurvivalGames player, BlockBreakEvent event) {
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

    @Override
    public void onDamage(PlayerSurvivalGames player, EntityDamageEvent event) {
        if (server.state != ServerSurvivalGames.GAME_INPROGRESS) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onFoodLevelChange(PlayerSurvivalGames player, FoodLevelChangeEvent e) {
        if (server.state == ServerSurvivalGames.GAME_WAITING) {
            if (e.getFoodLevel() < 20) {
                e.setFoodLevel(20);
            }
        }
    }

    // Prevent player from moving
    @Override
    public void onMove(PlayerSurvivalGames player, PlayerMoveEvent event) {
        if (server.state == ServerSurvivalGames.GAME_BEGIN) {
            player.teleportPositionOnly(event.getFrom());
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().clear();
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
