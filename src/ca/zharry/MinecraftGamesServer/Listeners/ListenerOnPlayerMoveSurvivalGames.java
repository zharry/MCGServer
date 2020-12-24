package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.Servers.ServerSurvivalGames;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class ListenerOnPlayerMoveSurvivalGames implements Listener {

    ServerSurvivalGames server;

    public ListenerOnPlayerMoveSurvivalGames(ServerSurvivalGames server) {
        this.server = server;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (server.state == ServerSurvivalGames.GAME_BEGIN) {
            Player player = event.getPlayer();
            if (event.getFrom().getX() != event.getTo().getX() ||
                    event.getFrom().getY() != event.getTo().getY() ||
                    event.getFrom().getZ() != event.getTo().getZ()) {
                Location location = new Location(player.getWorld(),
                        event.getFrom().getX(), event.getFrom().getY(), event.getFrom().getZ());
                location.setPitch(player.getLocation().getPitch());
                location.setYaw(player.getLocation().getYaw());
                event.getPlayer().teleport(location);
            }
        }
    }

}
