package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Players.PlayerElytraRun;
import ca.zharry.MinecraftGamesServer.Players.PlayerParkour;
import ca.zharry.MinecraftGamesServer.Servers.ServerElytraRun;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class ListenerElytraRun extends PlayerListenerAdapter<ServerElytraRun, PlayerElytraRun> {

//    FileOutputStream output;

    public ListenerElytraRun(ServerElytraRun server) {
        super(server, PlayerElytraRun.class);

//        try {
//            output = new FileOutputStream("coords.txt");
//        } catch(Exception e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void onJoin(PlayerElytraRun player, PlayerJoinEvent event) {
        player.reset(GameMode.ADVENTURE);
        server.giveInventory(player);

        if(server.state == ServerElytraRun.GAME_INPROGRESS || server.state == ServerElytraRun.GAME_STARTING) {
            player.teleport(server.getPlayerStartLocation(player));
        } else {
            player.teleport(server.serverSpawn);
            player.bukkitPlayer.setBedSpawnLocation(server.serverSpawn, true);
        }
        player.dead = false;
    }

    public static String timeToString(long nanoseconds) {
        return String.format("%.1f", nanoseconds / 1e9);
    }
    public static String distanceToString(double distance) {
        return String.format("%.1f", distance);
    }

    @Override
    public void onMove(PlayerElytraRun player, PlayerMoveEvent event) {
        Location l = player.getLocation();
//        try {
//            output.write((l.getX() + "," + l.getY() + "," + l.getZ() + "\n").getBytes(StandardCharsets.UTF_8));
//            output.flush();
//        } catch(Exception e) {
//            e.printStackTrace();
//        }
        if(player.dead) {
            event.setCancelled(true);
            return;
        }
        boolean canFly = false;
        Location dst = event.getTo();

        if(server.state == ServerElytraRun.GAME_INPROGRESS) {
            double dist = server.getPlayerDistance(player, server.tunnel);

            player.inBlock = false;
            // If the Player reached the end of the tunnel
            if(ServerElytraRun.tunnelFinish[server.tunnel].contains(dst)) {
                if(player.startingTime != 0) {
                    long timeTaken = System.nanoTime() - server.roundStartTime;
                    if(player.maxDistance[server.tunnel] != Double.POSITIVE_INFINITY) {
                        // Tunnel completion/scoring logic goes here, tunnel has been completed for the first time
                        player.bukkitPlayer.playSound(player.getLocation(), "tsf:voices.tfa_land", SoundCategory.PLAYERS, 1, 1);

                        player.maxDistance[server.tunnel] = Double.POSITIVE_INFINITY;
                        player.completedTime[server.tunnel] = timeTaken;
                        player.addScore(300, "finished tunnel " + server.tunnel);

                        int playerCount = 0;

                        for (PlayerElytraRun otherPlayer : server.players) {
                            if(otherPlayer.maxDistance[server.tunnel] != Double.POSITIVE_INFINITY) {
                                playerCount ++;
                            }
                        }
                        MCGMain.broadcastInfo("Players remaining: " + playerCount);

                        // Tunnel completed message
                        player.bukkitPlayer.sendTitle("Tunnel completed in " + timeToString(timeTaken), "");
                        player.bukkitPlayer.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("Tunnel completed in " + timeToString(timeTaken) + " seconds!"));

                        ItemStack unstuck = new ItemStack(Material.PAPER, 1);
                        ItemMeta unstuckMeta = unstuck.getItemMeta();
                        unstuckMeta.setDisplayName(ChatColor.RED + "Teleport to beginning");
                        unstuck.setItemMeta(unstuckMeta);
                        player.bukkitPlayer.getInventory().setItem(7, unstuck);

                        server.sendMessageAll(ChatColor.WHITE + "" + ChatColor.BOLD + "[+300] " +
                                ChatColor.RESET + player.getDisplayName() + " has completed tunnel " + (server.tunnel + 1) + " in " + timeToString(timeTaken) + " seconds!");
                    }
                }
            }

            canFly = true;
        }

        if(server.state == ServerElytraRun.GAME_INPROGRESS || server.state == ServerElytraRun.GAME_WAITING) {
            int tunnel;
            if(server.state == ServerElytraRun.GAME_WAITING) {
                tunnel = server.getTunnel(dst);
            } else {
                tunnel = server.tunnel;
            }
            if(tunnel != -1) {
                 if (server.state == ServerElytraRun.GAME_WAITING) {
                    if(ServerElytraRun.tunnelFinish[tunnel].contains(dst)) {
                        if(player.startingTime != 0) {
                            long timeTaken = System.nanoTime() - player.startingTime;
                            player.bukkitPlayer.sendTitle("Tunnel completed in " + timeToString(timeTaken), "");
                            player.bukkitPlayer.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("Tunnel completed in " + timeToString(timeTaken) + " seconds!"));
                        }
                    }
                }
                double dist = server.getPlayerDistance(player, tunnel);
                // Player is in a zone where they can crash
                if (ServerElytraRun.dangerZones[tunnel].contains(dst)) {
                    if (dist > player.maxDistance[tunnel])
                        player.maxDistance[tunnel] = dist;

                    if (player.startingTime == 0 && player.getLocation().distance(server.jumpPlatform[tunnel]) < 100) {
                        player.startingTime = System.nanoTime();
                    }
                    if (player.startingTime != 0) {
                        player.bukkitPlayer.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(
                                String.format("Speed: %.1f  Completion: %.0f%%",
                                        event.getTo().distance(event.getFrom()) * 20,
                                        100.0 * server.getPlayerDistance(player, tunnel) / server.tunnelLength(tunnel)
                                )
                                /*+ timeToString(System.nanoTime() - player.startingTime)*/
                        ));
                    }

                    // If the player has crashed
                    if (server.world.getBlockAt(player.getLocation()).getType() != Material.AIR) {
                        player.inBlock = true;
                    }
                    if (player.bukkitPlayer.isOnGround()) {
                        server.killPlayer(player);
                    }
                } else {
                    player.startingTime = 0;
                }
                player.lastTunnel = tunnel;
            }
        }

        if(server.state == ServerElytraRun.GAME_STARTING) {
            if(ServerElytraRun.dangerZones[0].contains(dst)) {
                player.teleport(server.jumpPlatform[0]);
            }
        }

        if(server.state == ServerElytraRun.GAME_WAITING) {
            canFly = true;
        }

        ItemStack chestplate = player.bukkitPlayer.getInventory().getChestplate();
        if(chestplate != null) {
            ItemMeta meta = chestplate.getItemMeta();
            if (meta != null) {
                if(canFly) {
                    ((Damageable) meta).setDamage(0);
                } else {
                    ((Damageable) meta).setDamage(432);
                }
                chestplate.setItemMeta(meta);
            }
        }
    }

    @Override
    public void onDropItem(PlayerElytraRun player, PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @Override
    protected void onInteract(PlayerElytraRun player, PlayerInteractEvent event) {
        if(event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            return;
        }
        /*if(event.getAction() == Action.PHYSICAL) {
            Block block = event.getClickedBlock();
            if(block != null && block.getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE) {
                int tunnel = server.getTunnel(player.getLocation());
                if(tunnel != -1) {
                    Point3D from = ServerElytraRun.tunnelAirlockEnd[tunnel];
                    Point3D to = ServerElytraRun.tunnelAirlockStart[tunnel + 1];
                    Point3D diff = to.subtract(from);
                    player.teleportRelative(diff.x, diff.y, diff.z, 0, 0, true, true, true, true, true);
                }
            }
        } else */
        if(event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if(block.getType() == Material.BIRCH_WALL_SIGN) {
                Sign sign = (Sign) block.getState();
                if(sign.getLine(1).equals("Go to")) {
                    String line2 = sign.getLine(2);
                    if(line2.startsWith("Tunnel ")) {
                        player.teleport(server.jumpPlatform[Integer.parseInt(line2.substring(7)) - 1]);
                        return;
                    }
                }
            }
        }

        if(event.getMaterial() == Material.PAPER) {
            if(server.state == ServerElytraRun.GAME_INPROGRESS) {
                player.teleport(server.jumpPlatform[server.tunnel]);
            }
        }

        if(event.getMaterial() == Material.TARGET && server.state == ServerElytraRun.GAME_WAITING) {
            player.teleport(server.practiceChooser);
            event.setCancelled(true);
            return;
        }

        if (event.getMaterial() == Material.END_ROD) {
            player.hintsEnabled = !player.hintsEnabled;
            player.bukkitPlayer.sendMessage("Hints have been " + (player.hintsEnabled ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
            event.setCancelled(true);
        }
    }

    @Override
    public void onDeath(PlayerElytraRun player, PlayerDeathEvent event) {
        if(!player.dead) {
            server.spawnDeathEffect(player);
        }
    }

    @Override
    public void onDamage(PlayerElytraRun player, EntityDamageEvent event) {
        if(server.state == ServerElytraRun.GAME_FINISHED) {
            event.setCancelled(true);
            return;
        }

        if (!ServerElytraRun.dangerZones[server.tunnel].contains(player.getLocation())) {
            event.setCancelled(true);
        }

        if(player.bukkitPlayer.getHealth() - event.getFinalDamage() < 1) {
            event.setCancelled(true);
            server.killPlayer(player);
        }
    }

    @Override
    public void onRespawn(PlayerElytraRun player, PlayerRespawnEvent event) {
        player.dead = false;
        server.giveInventory(player);
        event.setRespawnLocation(server.getPlayerStartLocation(player));
        if(server.state == ServerElytraRun.GAME_INPROGRESS) {
            if(player.maxDistance[server.tunnel] == Double.POSITIVE_INFINITY) {
                ItemStack unstuck = new ItemStack(Material.PAPER, 1);
                ItemMeta unstuckMeta = unstuck.getItemMeta();
                unstuckMeta.setDisplayName(ChatColor.RED + "Teleport to beginning");
                unstuck.setItemMeta(unstuckMeta);
                player.bukkitPlayer.getInventory().setItem(7, unstuck);
            }
        }
    }

    @Override
    public void onInventoryClick(PlayerElytraRun player, InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
