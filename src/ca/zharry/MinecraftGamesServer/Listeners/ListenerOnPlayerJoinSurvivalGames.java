package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.Players.PlayerSurvivalGames;
import ca.zharry.MinecraftGamesServer.Servers.ServerSpleef;
import ca.zharry.MinecraftGamesServer.Servers.ServerSurvivalGames;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ListenerOnPlayerJoinSurvivalGames implements Listener {

    ServerSurvivalGames server;

    public ListenerOnPlayerJoinSurvivalGames(ServerSurvivalGames server) {
        this.server = server;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerSurvivalGames playerSurvivalGames = new PlayerSurvivalGames(player, server);
        server.addPlayer(playerSurvivalGames);
        player.setGameMode(GameMode.ADVENTURE);

        if (server.state == ServerSpleef.GAME_INPROGRESS) {
            if (player.getLocation().getY() >= ServerSpleef.COMPETITION_MAX_HEIGHT) {
                server.dead.add(playerSurvivalGames);
                playerSurvivalGames.dead = true;
            } else {
                player.setGameMode(GameMode.SURVIVAL);
            }
        } else {
            Location serverSpawn = new Location(player.getWorld(), 0.5, 176, 0.5);
            player.teleport(serverSpawn);
            player.setBedSpawnLocation(serverSpawn, true);
        }
    }
}
