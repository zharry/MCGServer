package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.Players.PlayerParkour;
import ca.zharry.MinecraftGamesServer.Servers.ServerParkour;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class ListenerOnPlayerQuitParkour implements Listener {

    ServerParkour server;

    public ListenerOnPlayerQuitParkour(ServerParkour server) {
        this.server = server;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        for (int i = 0; i < server.players.size(); i++) {
            PlayerParkour curPlayer = (PlayerParkour) server.players.get(i);
            if (curPlayer.bukkitPlayer.getUniqueId() == player.getUniqueId()) {
                curPlayer.commit();
                server.players.remove(i);
                break;
            }
        }
    }
}
