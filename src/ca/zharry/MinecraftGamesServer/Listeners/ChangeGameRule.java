package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.Servers.ServerInterface;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

public class ChangeGameRule implements Listener {
    public ServerInterface server;

    public ChangeGameRule(ServerInterface server) {
        this.server = server;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        server.applyGameRules(event.getWorld());
    }
}
