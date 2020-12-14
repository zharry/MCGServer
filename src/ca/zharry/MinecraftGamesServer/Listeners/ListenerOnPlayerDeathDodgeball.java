package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.Players.PlayerDodgeball;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Servers.ServerDodgeball;
import javafx.geometry.Point3D;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class ListenerOnPlayerDeathDodgeball implements Listener {

    ServerDodgeball server;
    public ListenerOnPlayerDeathDodgeball(ServerDodgeball server) {
        this.server = server;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (server.state == ServerDodgeball.GAME_INPROGRESS) {
            Player killer = event.getEntity().getPlayer().getKiller();
            Player dead = event.getEntity().getPlayer();
            if (killer.getUniqueId() == dead.getUniqueId())
                return;

            // If the killer is dead
            for (PlayerInterface p : server.players) {
                PlayerDodgeball player = (PlayerDodgeball) p;
                if (killer.getUniqueId() == p.bukkitPlayer.getUniqueId() && player.lives <= 0)
                    return;
            }

            for (PlayerInterface p : server.players) {
                PlayerDodgeball player = (PlayerDodgeball) p;

                // Find the killer
                if (killer.getUniqueId() == p.bukkitPlayer.getUniqueId()) {
                    player.kills++;
                    player.totalKills++;
                    player.currentScore += 50;
                }

                // Find the dead guy
                if (dead.getUniqueId() == p.bukkitPlayer.getUniqueId()) {
                    player.lives--;
                    if (player.lives <= 0) {
                        // Send the dead player to the spectator zone
                        Point3D arenaSpectatorZone = ServerDodgeball.arenaSpectator.get(player.arena - 1);
                        Location arenaSpectatorLocation = new Location(player.bukkitPlayer.getWorld(),
                                arenaSpectatorZone.getX(), arenaSpectatorZone.getY(), arenaSpectatorZone.getZ());
                        player.bukkitPlayer.teleport(arenaSpectatorLocation);
                        player.bukkitPlayer.setBedSpawnLocation(arenaSpectatorLocation, true);
                    }
                }
            }
        }
    }

}
