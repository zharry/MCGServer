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
        PlayerDodgeball playerDodgeball = new PlayerDodgeball(player, server);
        server.addPlayer(playerDodgeball);

        player.setGameMode(GameMode.ADVENTURE);
        Location serverSpawn = new Location(player.getWorld(), -15.5, 4, 1.5);
        player.teleport(serverSpawn);
        player.setBedSpawnLocation(serverSpawn, true);
    }
}
