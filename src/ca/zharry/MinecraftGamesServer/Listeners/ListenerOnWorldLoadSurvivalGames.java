package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.Servers.ServerSurvivalGames;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

public class ListenerOnWorldLoadSurvivalGames implements Listener {

    ServerSurvivalGames server;

    public ListenerOnWorldLoadSurvivalGames(ServerSurvivalGames server) {
        this.server = server;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        for(int x = -16; x < 16; ++x) {
            for(int z = -16; z < 16; ++z) {
                world.setChunkForceLoaded(x, z, true);
            }
        }
    }
}