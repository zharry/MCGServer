package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.Players.PlayerDodgeball;
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
            Player k = event.getEntity().getPlayer().getKiller();
            Player d = event.getEntity().getPlayer();
            if (k == null || k.getUniqueId() == d.getUniqueId())
                return;
            PlayerDodgeball killer = (PlayerDodgeball) server.playerLookup.get(k.getUniqueId());
            PlayerDodgeball dead = (PlayerDodgeball) server.playerLookup.get(d.getUniqueId());

            // If the killer is dead or dead is already dead
            if (killer.lives <= 0 || dead.lives <= 0)
                return;

            // Award the killer
            killer.kills++;
            killer.totalKills++;
            killer.currentScore += 50;

            // Take a life away from the dead
            dead.lives--;
            if (dead.lives <= 0) {
                // Send the dead player to the spectator zone
                Point3D arenaSpectatorZone = ServerDodgeball.arenaSpectator.get(dead.arena - 1);
                Location arenaSpectatorLocation = new Location(dead.bukkitPlayer.getWorld(),
                        arenaSpectatorZone.getX(), arenaSpectatorZone.getY(), arenaSpectatorZone.getZ());
                dead.bukkitPlayer.teleport(arenaSpectatorLocation);
                dead.bukkitPlayer.setBedSpawnLocation(arenaSpectatorLocation, true);
            }
        }
    }

}
