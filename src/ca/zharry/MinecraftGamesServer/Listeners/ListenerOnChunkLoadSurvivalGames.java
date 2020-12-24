package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.Servers.ServerSurvivalGames;
import ca.zharry.MinecraftGamesServer.Utils.Coord3D;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class ListenerOnChunkLoadSurvivalGames implements Listener {

    ServerSurvivalGames server;

    public ListenerOnChunkLoadSurvivalGames(ServerSurvivalGames server) {
        this.server = server;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        BlockState[] states = event.getChunk().getTileEntities();
        for(BlockState state : states) {
            if(state.getType() == Material.CHEST) {
                server.addChest(new Coord3D(state.getX(), state.getY(), state.getZ()));
            }
        }
    }

}
