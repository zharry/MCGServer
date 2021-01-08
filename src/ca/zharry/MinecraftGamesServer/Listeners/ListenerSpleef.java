package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Players.PlayerSpleef;
import ca.zharry.MinecraftGamesServer.Servers.ServerParkour;
import ca.zharry.MinecraftGamesServer.Servers.ServerSpleef;
import ca.zharry.MinecraftGamesServer.Utils.PlayerUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class ListenerSpleef implements Listener {
    ServerSpleef server;

    public ListenerSpleef(ServerSpleef server) {
        this.server = server;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        System.out.println("PLAYER JOIN SPLEEF");
        Player player = event.getPlayer();
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 1000000, 0, false, false));
        PlayerSpleef playerSpleef = (PlayerSpleef) server.playerLookup.get(player.getUniqueId());

        if (server.state == ServerSpleef.GAME_WAITING ||
                server.state == ServerSpleef.GAME_STARTING ||
                server.state == ServerSpleef.GAME_FINISHED) {
            player.teleport(server.serverSpawn);
            player.setGameMode(GameMode.ADVENTURE);
            System.out.println("PLAYER FINISHED JOIN SPLEEF");
        }
        if (server.state == ServerSpleef.GAME_INPROGRESS) {
            if (player.getLocation().getY() >= ServerSpleef.COMPETITION_MAX_HEIGHT) {
                playerSpleef.dead = true;
                playerSpleef.bukkitPlayer.setGameMode(GameMode.SPECTATOR);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (server.state == ServerSpleef.GAME_INPROGRESS) {
            // Find the dead player
            Player deadPlayer = event.getEntity();
            PlayerSpleef player = (PlayerSpleef) server.playerLookup.get(deadPlayer.getUniqueId());

            // Make sure that this player isn't giving duplicate scores
            if (deadPlayer.getGameMode() != GameMode.SURVIVAL || player.dead)
                return;

            // Mark them as dead and upload their score
            player.dead = true;

            // Award all non-dead players some score
            player.bukkitPlayer.sendTitle("You died!", "", 0, 30, 20);
            for (PlayerInterface p : server.players) {
                PlayerSpleef playerSpleef = (PlayerSpleef) p;
                if (!playerSpleef.dead) {
                    playerSpleef.addScore(75, deadPlayer.getName() + " has died!");
                    playerSpleef.bukkitPlayer.sendMessage(ChatColor.RESET + "" + ChatColor.BOLD + " [+75]" +
                            ChatColor.RESET + " survival points, " + deadPlayer.getDisplayName() + " has died!");
                } else {
                    playerSpleef.bukkitPlayer.sendMessage(ChatColor.RESET + "" + deadPlayer.getDisplayName() + " has died!");
                }
            }

            // Commit score
            player.commit();
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        new BukkitRunnable() {
            public void run() {
                Player player = event.getPlayer();
                player.teleport(server.serverSpawn);
                if (server.state == ServerSpleef.GAME_INPROGRESS) {
                    PlayerUtils.resetPlayer(player, GameMode.SPECTATOR);
                } else {
                    PlayerUtils.resetPlayer(player, GameMode.ADVENTURE);
                }
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 1000000, 0, false, false));
            }
        }.runTaskLater(server.plugin, 1);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getLocation().getY() < ServerSpleef.COMPETITION_MIN_HEIGHT) {
            player.setHealth(0);
        }

        PlayerSpleef playerSpleef = (PlayerSpleef) server.playerLookup.get(player.getUniqueId());

        if (server.state == ServerSpleef.GAME_BEGIN || (server.suddenDeathState == ServerSpleef.SUDDENDEATH_COUNTDOWN && !playerSpleef.dead)) {
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

    @EventHandler
    public void onPlayerDestroy(BlockBreakEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }
        // Disallow players to destroy spawn platform if they can reach it
        if (event.getBlock().getLocation().getY() >= ServerSpleef.COMPETITION_MAX_HEIGHT) {
            event.setCancelled(true);
            return;
        }

        PlayerSpleef playerSpleef = (PlayerSpleef) server.playerLookup.get(event.getPlayer().getUniqueId());

        // Disallow players to break the arena before the game starts
        if (server.state == ServerSpleef.GAME_BEGIN || (server.suddenDeathState == ServerSpleef.SUDDENDEATH_COUNTDOWN && !playerSpleef.dead)) {
            event.setCancelled(true);
            return;
        }

        Player p = event.getPlayer();
        int hunger = p.getFoodLevel();
        hunger = hunger >= 19 ? 20 : hunger + 1;
        p.setFoodLevel(hunger);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> block.getLocation().getY() > ServerSpleef.COMPETITION_MAX_HEIGHT);
        server.world.spawnParticle(Particle.EXPLOSION_LARGE, event.getLocation(), 0);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked().getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        PlayerSpleef playerSpleef = (PlayerSpleef) server.getPlayerFromUUID(p.getUniqueId());
        Block block = event.getClickedBlock();

        // Practice Mode Signs
        if (server.state == ServerParkour.GAME_WAITING && block != null && block.getType() == Material.BIRCH_WALL_SIGN) {
            String signLine1 = ChatColor.stripColor(((Sign) block.getState()).getLine(1));
            String signLine2 = ChatColor.stripColor(((Sign) block.getState()).getLine(2));

            // Send to Arena
            if (signLine1.equals("Send to") && signLine2.equals("Arena")) {
                server.sendToArena(playerSpleef, ServerSpleef.TELEPORT_MIN_Y, ServerSpleef.TELEPORT_MAX_Y);
                event.setCancelled(true);
            }
            // Reset World
            if (signLine1.equals("Reset") && signLine2.equals("World")) {
                server.spleefRestore(server.world);
                event.setCancelled(true);
            }
            ///some change
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent e) {
        System.out.println(e.getCause());
        if (e.getCause() == EntityDamageEvent.DamageCause.VOID ||
                e.getCause() == EntityDamageEvent.DamageCause.CUSTOM) {
            return;
        }
        System.out.println("Test3");
        e.setCancelled(true);
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent e) {
        if (server.state != ServerSpleef.GAME_INPROGRESS) {
            if (e.getFoodLevel() < 20) {
                e.setFoodLevel(20);
            }
        }
    }
}

