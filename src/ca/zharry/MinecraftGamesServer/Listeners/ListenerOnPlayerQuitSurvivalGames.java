package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.Players.PlayerSurvivalGames;
import ca.zharry.MinecraftGamesServer.Servers.ServerSurvivalGames;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class ListenerOnPlayerQuitSurvivalGames implements Listener {

    ServerSurvivalGames server;

    public ListenerOnPlayerQuitSurvivalGames(ServerSurvivalGames server) {
        this.server = server;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        for (int i = 0; i < server.players.size(); i++) {
            PlayerSurvivalGames curPlayer = (PlayerSurvivalGames) server.players.get(i);
            if (curPlayer.bukkitPlayer.getUniqueId() == player.getUniqueId()) {
                curPlayer.commit();
                server.players.remove(i);
                break;
            }
        }
    }
}
