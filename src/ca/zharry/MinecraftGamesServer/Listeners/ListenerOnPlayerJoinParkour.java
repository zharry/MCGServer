package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.Players.PlayerParkour;
import ca.zharry.MinecraftGamesServer.Servers.ServerParkour;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ListenerOnPlayerJoinParkour implements Listener {

    ServerParkour server;
    public ListenerOnPlayerJoinParkour(ServerParkour server) {
        this.server = server;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        server.players.add(new PlayerParkour(player, server));
        player.setGameMode(GameMode.ADVENTURE);

        if (server.state == ServerParkour.GAME_WAITING || server.state == ServerParkour.GAME_STARTING) {
            Location serverSpawn = new Location(player.getWorld(), 253.5, 134, -161.5);
            player.teleport(serverSpawn);
        }
        if (server.state == ServerParkour.GAME_FINISHED) {
            Location gameSpectate = new Location(player.getWorld(), 8.5, 131, 9.5);
            player.teleport(gameSpectate);
        }
    }
}
