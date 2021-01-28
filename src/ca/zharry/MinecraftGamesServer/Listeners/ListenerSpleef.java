package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Players.PlayerSpleef;
import ca.zharry.MinecraftGamesServer.Servers.ServerParkour;
import ca.zharry.MinecraftGamesServer.Servers.ServerSpleef;
import ca.zharry.MinecraftGamesServer.Utils.LocationUtils;
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

public class ListenerSpleef extends PlayerListenerAdapter<ServerSpleef, PlayerSpleef> {
    public ListenerSpleef(ServerSpleef server) {
        super(server, PlayerSpleef.class);
    }

    @Override
    public void onJoin(PlayerSpleef player, PlayerJoinEvent event) {
        player.bukkitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 1000000, 0, false, false));

        if (server.state == ServerSpleef.GAME_WAITING ||
                server.state == ServerSpleef.GAME_STARTING ||
                server.state == ServerSpleef.GAME_FINISHED) {
            player.teleport(server.serverSpawn);
            player.setGameMode(GameMode.ADVENTURE);
        }
        if (server.state == ServerSpleef.GAME_INPROGRESS) {
            if (player.getLocation().getY() >= ServerSpleef.COMPETITION_MAX_HEIGHT) {
                player.dead = true;
                player.setGameMode(GameMode.SPECTATOR);
            }
        }
    }

    @Override
    public void onDeath(PlayerSpleef player, PlayerDeathEvent event) {
        if (server.state == ServerSpleef.GAME_INPROGRESS) {
            // Make sure that this player isn't giving duplicate scores
            if (player.getGameMode() != GameMode.SURVIVAL || player.dead)
                return;

            // Mark them as dead and upload their score
            player.dead = true;

            // Award all non-dead players some score
            player.bukkitPlayer.sendTitle("You died!", "", 0, 30, 20);
            for (PlayerSpleef playerSpleef : server.players) {
                if (!playerSpleef.dead) {
                    playerSpleef.addScore(75, player.getDisplayName() + " has died!");
                    playerSpleef.bukkitPlayer.sendMessage(ChatColor.RESET + "" + ChatColor.BOLD + " [+75]" +
                            ChatColor.RESET + " survival points, " + player.getDisplayName() + " has died!");
                } else {
                    playerSpleef.bukkitPlayer.sendMessage(ChatColor.RESET + "" + player.getDisplayName() + " has died!");
                }
            }

            // Commit score
            player.commit();
        }
    }

    @Override
    public void onRespawn(PlayerSpleef player, PlayerRespawnEvent event) {
        event.setRespawnLocation(server.serverSpawn);
        if (server.state == ServerSpleef.GAME_INPROGRESS) {
            player.reset(GameMode.SPECTATOR);
        } else {
            player.reset(GameMode.ADVENTURE);
        }
        player.bukkitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 1000000, 0, false, false));
    }

    @Override
    public void onMove(PlayerSpleef player, PlayerMoveEvent event) {
        if (player.getLocation().getY() < ServerSpleef.COMPETITION_MIN_HEIGHT) {
            player.setHealth(0);
        }

        if (server.state == ServerSpleef.GAME_BEGIN || (server.suddenDeathState == ServerSpleef.SUDDENDEATH_COUNTDOWN && !player.dead)) {
            if(!LocationUtils.positionEquals(event.getFrom(), event.getTo())) {
                player.teleportPositionOnly(event.getFrom());
            }
        }
    }

    @Override
    public void onBlockBreak(PlayerSpleef player, BlockBreakEvent event) {
        // Disallow players to destroy spawn platform if they can reach it
        if (event.getBlock().getLocation().getY() >= ServerSpleef.COMPETITION_MAX_HEIGHT) {
            event.setCancelled(true);
            return;
        }

        // Disallow players to break the arena before the game starts
        if (server.state == ServerSpleef.GAME_BEGIN || (server.suddenDeathState == ServerSpleef.SUDDENDEATH_COUNTDOWN && !player.dead)) {
            event.setCancelled(true);
            return;
        }

        int hunger = player.bukkitPlayer.getFoodLevel();
        hunger = hunger >= 19 ? 20 : hunger + 1;
        player.bukkitPlayer.setFoodLevel(hunger);
    }

    @Override
    public void onDropItem(PlayerSpleef player, PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onInventoryClick(PlayerSpleef player, InventoryClickEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onInteract(PlayerSpleef player, PlayerInteractEvent event) {
        Block block = event.getClickedBlock();

        // Practice Mode Signs
        if (server.state == ServerParkour.GAME_WAITING && block != null && block.getType() == Material.BIRCH_WALL_SIGN) {
            String signLine1 = ChatColor.stripColor(((Sign) block.getState()).getLine(1));
            String signLine2 = ChatColor.stripColor(((Sign) block.getState()).getLine(2));

            // Send to Arena
            if (signLine1.equals("Send to") && signLine2.equals("Arena")) {
                server.sendToArena(player, ServerSpleef.TELEPORT_MIN_Y, ServerSpleef.TELEPORT_MAX_Y);
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

    @Override
    public void onDamage(PlayerSpleef player, EntityDamageEvent e) {
        if (e.getCause() == EntityDamageEvent.DamageCause.VOID ||
                e.getCause() == EntityDamageEvent.DamageCause.CUSTOM) {
            return;
        }
        e.setCancelled(true);
    }

    @Override
    public void onFoodLevelChange(PlayerSpleef player, FoodLevelChangeEvent e) {
        if (server.state != ServerSpleef.GAME_INPROGRESS) {
            if (e.getFoodLevel() < 20) {
                e.setFoodLevel(20);
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> block.getLocation().getY() > ServerSpleef.COMPETITION_MAX_HEIGHT);
        server.world.spawnParticle(Particle.EXPLOSION_LARGE, event.getLocation(), 0);
    }
}

