package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.Players.PlayerLobby;
import ca.zharry.MinecraftGamesServer.Players.PlayerParkour;
import ca.zharry.MinecraftGamesServer.Servers.ServerParkour;
import ca.zharry.MinecraftGamesServer.Utils.LocationUtils;
import ca.zharry.MinecraftGamesServer.Utils.Point3D;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;

public class ListenerParkour extends PlayerListenerAdapter<ServerParkour, PlayerParkour> {

    public ListenerParkour(ServerParkour server) {
        super(server, PlayerParkour.class);
    }

    @Override
    public void onJoin(PlayerParkour player, PlayerJoinEvent event) {
        player.setGameMode(GameMode.ADVENTURE);
        player.bukkitPlayer.setInvisible(false);
        server.setPlayerInventoryContents(player);

        if (server.state == ServerParkour.GAME_WAITING) {
            player.teleport(server.serverSpawn);
            player.bukkitPlayer.setBedSpawnLocation(server.serverSpawn, true);

        } else if (server.state == ServerParkour.GAME_STARTING) {
            Point3D checkpointLoc = server.stage1Checkpoints.get(0);
            Location firstCheckpoint = new Location(server.world, checkpointLoc.x, checkpointLoc.y, checkpointLoc.z);
            player.teleport(firstCheckpoint);
            player.bukkitPlayer.setBedSpawnLocation(firstCheckpoint, true);

        } else if (server.state == ServerParkour.GAME_INPROGRESS) {
            // Teleport player to last checkpoint
            Point3D checkpointLoc = server.stage1Checkpoints.get(0);
            switch (player.stage) {
                case 1:
                    checkpointLoc = server.stage1Checkpoints.get(player.level);
                    break;
                case 2:
                    checkpointLoc = server.stage2Checkpoints.get(player.level);
                    break;
                case 3:
                    checkpointLoc = server.stage3Checkpoints.get(player.level);
                    break;
                case 4:
                    checkpointLoc = server.stage4Checkpoints.get(player.level);
                    break;
                case 5:
                    checkpointLoc = server.stage5Checkpoints.get(player.level);
                    break;
                case 6:
                    checkpointLoc = server.stage6Checkpoints.get(player.level);
                    break;
                default:
            }
            Location nextStage = new Location(server.world, checkpointLoc.getX() + 0.5, checkpointLoc.getY() + 1, checkpointLoc.getZ() + 0.5, 90, 0);
            player.teleport(nextStage);
            player.bukkitPlayer.setInvisible(true);

        } else if (server.state == ServerParkour.GAME_FINISHED) {
            player.teleport(server.mapEnd);
            player.bukkitPlayer.setBedSpawnLocation(server.mapEnd, true);
        }
    }

    @Override
    public void onDeath(PlayerParkour player, PlayerDeathEvent event) {
        event.setDeathMessage(null);
    }

    @Override
    public void onMove(PlayerParkour player, PlayerMoveEvent event) {
        // Disable player movement in during countdown and pre-game
        if (server.state == ServerParkour.GAME_STARTING) {
            if(!LocationUtils.positionEquals(event.getFrom(), event.getTo())) {
                player.teleportPositionOnly(event.getFrom());
            }
            return;
        }

        // Check if the player is on a checkpoint
        if (server.state == ServerParkour.GAME_INPROGRESS) {
            if (event.getTo().getBlock().getRelative(BlockFace.DOWN).getType() == Material.BEACON) {
                Location blockLocation = player.getLocation().getBlock().getLocation();
                Point3D blockPoint = new Point3D(blockLocation.getX(), blockLocation.getY() - 1, blockLocation.getZ());

                // Find out which stage the beacon block is on
                int stage1Index = server.stage1Checkpoints.indexOf(blockPoint);
                int stage2Index = server.stage2Checkpoints.indexOf(blockPoint);
                int stage3Index = server.stage3Checkpoints.indexOf(blockPoint);
                int stage4Index = server.stage4Checkpoints.indexOf(blockPoint);
                int stage5Index = server.stage5Checkpoints.indexOf(blockPoint);
                int stage6Index = server.stage6Checkpoints.indexOf(blockPoint);
                int stage = -1;
                if (stage1Index != -1) stage = 1;
                if (stage2Index != -1) stage = 2;
                if (stage3Index != -1) stage = 3;
                if (stage4Index != -1) stage = 4;
                if (stage5Index != -1) stage = 5;
                if (stage6Index != -1) stage = 6;
                // This represents what level the block is on, for the stage determined above
                int[] levels = {-1, stage1Index, stage2Index, stage3Index, stage4Index, stage5Index, stage6Index};
                // This represents how many levels the player would have completed upon reaching the (i + 1)th stage
                int[] stageCompletedLevels = {0,
                        server.stage1Checkpoints.size() - 1,
                        server.stage1Checkpoints.size() + server.stage2Checkpoints.size() - 2,
                        server.stage1Checkpoints.size() + server.stage2Checkpoints.size() + server.stage3Checkpoints.size() - 3,
                        server.stage1Checkpoints.size() + server.stage2Checkpoints.size() + server.stage3Checkpoints.size() + server.stage4Checkpoints.size() - 4,
                        server.stage1Checkpoints.size() + server.stage2Checkpoints.size() + server.stage3Checkpoints.size() + server.stage4Checkpoints.size() + server.stage5Checkpoints.size() - 5,
                        server.stage1Checkpoints.size() + server.stage2Checkpoints.size() + server.stage3Checkpoints.size() + server.stage4Checkpoints.size() + server.stage5Checkpoints.size() + server.stage6Checkpoints.size() - 6
                };

                // Award points if this is new
                if (stage == player.stage && levels[stage] > player.level ||
                        stage > player.stage) {
                    if (levels[stage] != 0) {
                        player.stage = stage;
                        player.level = levels[stage];
                        int newScore = stageCompletedLevels[stage - 1] * 150 + levels[stage] * 150;
                        player.addScore(newScore - player.getCurrentScore(), "completed " + player.stage + "-" + player.level);
                        player.bukkitPlayer.sendTitle("Stage " + stage + "-" + levels[stage], "Checkpoint Completed", 10, 30, 10);
                        server.sendMessageAll(player.bukkitPlayer.getDisplayName() +
                                ChatColor.RESET + "" + ChatColor.BOLD + " [" + player.getCurrentScore() + "] " +
                                ChatColor.RESET + "has completed Stage " + stage + "-" + levels[stage]);

                        player.bukkitPlayer.setBedSpawnLocation(blockLocation.add(0.5, 1, 0.5), true);
                    }
                }

                // Check if we finished the stages
                boolean finishedStage = false;
                Point3D nextStart = server.stage1Checkpoints.get(0);
                if (stage1Index == server.stage1Checkpoints.size() - 1) {
                    finishedStage = true;
                    nextStart = server.stage2Checkpoints.get(0);
                } else if (stage2Index == server.stage2Checkpoints.size() - 1) {
                    finishedStage = true;
                    nextStart = server.stage3Checkpoints.get(0);
                } else if (stage3Index == server.stage3Checkpoints.size() - 1) {
                    finishedStage = true;
                    nextStart = server.stage4Checkpoints.get(0);
                } else if (stage4Index == server.stage4Checkpoints.size() - 1) {
                    finishedStage = true;
                    nextStart = server.stage5Checkpoints.get(0);
                } else if (stage5Index == server.stage5Checkpoints.size() - 1) {
                    finishedStage = true;
                    nextStart = server.stage6Checkpoints.get(0);
                } else if (stage6Index == server.stage6Checkpoints.size() - 1) {
                    player.teleport(server.mapEnd);
                    return;
                }
                if (finishedStage) {
                    Location nextStage = new Location(server.world, nextStart.getX() + 0.5, nextStart.getY() + 1, nextStart.getZ() + 0.5, 90, 0);
                    player.teleport(nextStage);
                    player.bukkitPlayer.setBedSpawnLocation(nextStage, true);
                }
            }
        }
    }

    @Override
    public void onDamage(PlayerParkour player, EntityDamageEvent e) {
        if (e.getCause() == EntityDamageEvent.DamageCause.CONTACT) {
            e.setDamage(0);
            return;
        }
        if (e.getCause() != EntityDamageEvent.DamageCause.VOID) {
            e.setCancelled(true);
        }
    }

    @Override
    public void onInventoryClick(PlayerParkour player, InventoryClickEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onDropItem(PlayerParkour player, PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onInteract(PlayerParkour player, PlayerInteractEvent event) {
        // Disable farmland trampling
        if (event.getAction() == Action.PHYSICAL) {
            event.setCancelled(true);
        }

        if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.ACACIA_DOOR) {
            return;
        }
        Block block = event.getClickedBlock();

        // Practice Mode - Level Select
        if (server.state == ServerParkour.GAME_WAITING && block != null && block.getType() == Material.BIRCH_WALL_SIGN) {
            String level = ((Sign) block.getState()).getLine(1);
            try {
                String[] s = level.split("-");
                int stage = Integer.parseInt(s[0]);
                int checkpoint = Integer.parseInt(s[1]);
                Point3D p3d = server.allCheckpoints.get(stage - 1).get(checkpoint - 1);
                event.getPlayer().teleport(new Location(event.getClickedBlock().getWorld(), p3d.x + 0.5, p3d.y + 1, p3d.z + 0.5, 90, 0));
            } catch (Exception e) {
            }
            event.setCancelled(true);
            return;
        }
        if (server.state == ServerParkour.GAME_WAITING && block != null && event.getMaterial() == Material.TARGET) {
            player.teleport(new Location(server.world, -10000.5, 64, 0.5, 90, 0));
            event.setCancelled(true);
            return;
        }

        // Test for Checkpoint reset
        if ((server.state == ServerParkour.GAME_INPROGRESS || server.state == ServerParkour.GAME_WAITING) && event.getMaterial() == Material.PAPER) {
            player.setHealth(0);
            event.setCancelled(true);
            return;
        }

        // Test for disable Waypoints
        if (event.getMaterial() == Material.END_ROD) {
            if (player.waypointsEnabled) {
                server.disableWaypoints(player);
            } else {
                server.enableWaypoints(player);
            }
            player.waypointsEnabled = !player.waypointsEnabled;
            player.bukkitPlayer.sendMessage("Waypoints have been " + (player.waypointsEnabled ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
            event.setCancelled(true);
        }
    }

    @Override
    public void onRespawn(PlayerParkour player, PlayerRespawnEvent event) {
        Location location = event.getRespawnLocation();
        location.setYaw(90);
        event.setRespawnLocation(location);
    }

    @Override
    public void onBlockBreak(PlayerParkour player, BlockBreakEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onBlockPlace(PlayerParkour player, BlockPlaceEvent event) {
        event.setCancelled(true);
    }

}
