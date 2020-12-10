package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.Players.PlayerDodgeball;
import ca.zharry.MinecraftGamesServer.Servers.ServerDodgeball;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ListenerOnPlayerJoinDodgeball implements Listener {

    ServerDodgeball server;
    public ListenerOnPlayerJoinDodgeball(ServerDodgeball server) {
        this.server = server;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        server.players.add(new PlayerDodgeball(player, server));

        if (server.state == ServerDodgeball.GAME_WAITING || server.state == ServerDodgeball.GAME_STARTING) {
            Location serverSpawn = new Location(player.getWorld(), 14.5, 75, 17.5);
            player.teleport(serverSpawn);
            player.setGameMode(GameMode.ADVENTURE);
        }
        if (server.state == ServerDodgeball.GAME_FINISHED) {
            Location gameSpectate = new Location(player.getWorld(), 14.5, 75, 17.5);
            player.teleport(gameSpectate);
            player.setGameMode(GameMode.ADVENTURE);
        }
    }
}
