package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Servers.ServerDodgeball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class ListenerOnPlayerDeathDodgeball implements Listener {

    ServerDodgeball server;
    public ListenerOnPlayerDeathDodgeball(ServerDodgeball server) {
        this.server = server;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (server.state == ServerDodgeball.GAME_INPROGRESS) {
            MCGMain.logger.info(event.getEntity().getPlayer().getKiller().getName() + " killed " + event.getEntity().getPlayer());
        }
    }
    
}
