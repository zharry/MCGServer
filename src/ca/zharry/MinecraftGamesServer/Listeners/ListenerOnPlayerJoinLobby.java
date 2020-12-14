package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.Players.PlayerLobby;
import ca.zharry.MinecraftGamesServer.Servers.ServerLobby;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ListenerOnPlayerJoinLobby implements Listener {

    ServerLobby server;
    public ListenerOnPlayerJoinLobby(ServerLobby server) {
        this.server = server;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        server.addPlayer(new PlayerLobby(player, server));
        player.setGameMode(GameMode.ADVENTURE);

        Location serverSpawn = new Location(player.getWorld(), 1484.5, 4, 530);
        player.teleport(serverSpawn);
        player.setBedSpawnLocation(serverSpawn, true);
    }
}
