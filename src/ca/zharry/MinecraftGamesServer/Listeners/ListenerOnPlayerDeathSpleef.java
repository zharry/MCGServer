package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Players.PlayerSpleef;
import ca.zharry.MinecraftGamesServer.Servers.ServerSpleef;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class ListenerOnPlayerDeathSpleef implements Listener {

    ServerSpleef server;
    public ListenerOnPlayerDeathSpleef(ServerSpleef server) {
        this.server = server;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (server.state == ServerSpleef.GAME_INPROGRESS) {
            // Find the dead player
            Player deadPlayer = event.getEntity();
            for (PlayerInterface p : server.players) {
                PlayerSpleef player = (PlayerSpleef) p;
                if (deadPlayer.getUniqueId() == p.bukkitPlayer.getUniqueId()) {

                    // Mark them as dead and upload their score
                    server.dead.add(player);
                    player.dead = true;
                    player.commit();
                    continue;
                }

                // Award all non-dead players some score
                if (!player.dead) {
                    player.currentScore += 100;
                }
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        player.setGameMode(GameMode.ADVENTURE);
        Location serverSpawn = new Location(player.getWorld(), 14.5, 75, 17.5);
        player.teleport(serverSpawn);
        player.getInventory().clear();
    }
}
