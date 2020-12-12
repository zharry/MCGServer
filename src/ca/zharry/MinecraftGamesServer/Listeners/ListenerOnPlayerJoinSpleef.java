package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.Players.PlayerSpleef;
import ca.zharry.MinecraftGamesServer.Servers.ServerSpleef;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ListenerOnPlayerJoinSpleef implements Listener {

    ServerSpleef server;
    public ListenerOnPlayerJoinSpleef(ServerSpleef server) {
        this.server = server;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        server.players.add(new PlayerSpleef(player, server));

        if (server.state == ServerSpleef.GAME_WAITING ||
                server.state == ServerSpleef.GAME_STARTING ||
                server.state == ServerSpleef.GAME_FINISHED) {
            Location gameSpectate = new Location(player.getWorld(), 14.5, 75, 17.5);
            player.teleport(gameSpectate);
            player.setGameMode(GameMode.ADVENTURE);
        }
    }
}
