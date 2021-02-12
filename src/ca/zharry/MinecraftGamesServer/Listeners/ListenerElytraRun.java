package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.Players.PlayerElytraRun;
import ca.zharry.MinecraftGamesServer.Players.PlayerElytraRun;
import ca.zharry.MinecraftGamesServer.Players.PlayerLobby;
import ca.zharry.MinecraftGamesServer.Servers.ServerElytraRun;
import ca.zharry.MinecraftGamesServer.Servers.ServerLobby;
import ca.zharry.MinecraftGamesServer.Utils.Point3D;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class ListenerElytraRun extends PlayerListenerAdapter<ServerElytraRun, PlayerElytraRun> {

    public ListenerElytraRun(ServerElytraRun server) {
        super(server, PlayerElytraRun.class);
    }

    @Override
    public void onJoin(PlayerElytraRun player, PlayerJoinEvent event) {
        player.reset(GameMode.ADVENTURE);
        server.giveElytra(player);

        if(server.state == ServerElytraRun.GAME_INPROGRESS || server.state == ServerElytraRun.GAME_STARTING) {
            player.teleport(server.getPlayerStartLocation(player));
        } else {
            player.teleport(server.serverSpawn);
            player.bukkitPlayer.setBedSpawnLocation(server.serverSpawn, true);
        }
        player.dead = false;
    }

    public String timeToString(long nanoseconds) {
        return String.format("%.1f", nanoseconds / 1e9);
    }

    @Override
    public void onMove(PlayerElytraRun player, PlayerMoveEvent event) {
        if(player.dead) {
            event.setCancelled(true);
            return;
        }
        boolean canFly = false;
        Location dst = event.getTo();

        if(ServerElytraRun.endDrop.contains(dst)) {
            player.teleport(server.practiceChooser);
        }

        if(server.state == ServerElytraRun.GAME_INPROGRESS) {
            int tunnel = server.getTunnel(dst);
            player.inBlock = false;
            if(tunnel != -1) {
                if(ServerElytraRun.tunnelFinish[tunnel].contains(dst)) {
                    if(player.startingTime != 0) {
                        long timeTaken = System.nanoTime() - player.startingTime;
                        if(tunnel + 1 > player.tunnel) {
                            // Tunnel completion/scoring logic goes here, tunnel has been completed for the first time
                            player.bukkitPlayer.playSound(player.getLocation(), "tsf:voices.tfa_land", SoundCategory.PLAYERS, 1, 1);
                            player.tunnel = tunnel + 1;
                        }
                        // Tunnel completed, might not be the first time
                        player.bukkitPlayer.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("Tunnel completed in " + timeToString(timeTaken)));
                    }
                }
                if(ServerElytraRun.dangerZones[tunnel].contains(dst)) {
                    if(player.startingTime == 0 && player.getLocation().distance(server.jumpPlatform[tunnel]) < 100) {
                        player.startingTime = System.nanoTime();
                    }
                    if(player.startingTime != 0) {
                        player.bukkitPlayer.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(String.format("%.1f", event.getTo().distance(event.getFrom()) * 20) + "  " + (int) server.getPlayerDistance(player, tunnel) + "/" + (int) server.tunnelLength(tunnel) + "  " + timeToString(System.nanoTime() - player.startingTime)));
                    }
                    if(server.world.getBlockAt(player.getLocation()).getType() != Material.AIR) {
                        player.inBlock = true;
                    }
                    if(player.bukkitPlayer.isOnGround()) {
                        server.killPlayer(player);
                    }
                } else {
                    player.startingTime = 0;
                }
                canFly = true;
            }
        }

        if(server.state == ServerElytraRun.GAME_STARTING) {
            if(ServerElytraRun.dangerZones[0].contains(dst)) {
                player.teleport(server.jumpPlatform[0]);
            }
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
    protected void onInteract(PlayerElytraRun player, PlayerInteractEvent event) {
        if(event.getAction() == Action.PHYSICAL) {
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
        } else if(event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if(block.getType() == Material.BIRCH_WALL_SIGN) {
                Sign sign = (Sign) block.getState();
                if(sign.getLine(1).equals("Go to")) {
                    String line2 = sign.getLine(2);
                    if(line2.startsWith("Tunnel ")) {
                        player.teleport(server.jumpPlatform[Integer.parseInt(line2.substring(7)) - 1]);
                    }
                }
            }
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
        if(player.bukkitPlayer.getHealth() - event.getFinalDamage() < 1) {
            event.setCancelled(true);
            server.killPlayer(player);
        }
    }

    @Override
    public void onRespawn(PlayerElytraRun player, PlayerRespawnEvent event) {
        player.dead = false;
        if(server.state == ServerElytraRun.GAME_INPROGRESS || server.state == ServerElytraRun.GAME_STARTING) {
            event.setRespawnLocation(server.getPlayerStartLocation(player));
        }
    }

    @Override
    public void onInventoryClick(PlayerElytraRun player, InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
