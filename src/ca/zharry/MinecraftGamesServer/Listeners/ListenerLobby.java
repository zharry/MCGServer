package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Players.PlayerLobby;
import ca.zharry.MinecraftGamesServer.Servers.ServerLobby;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class ListenerLobby extends PlayerListenerAdapter<ServerLobby, PlayerLobby> {
    public ListenerLobby(ServerLobby server) {
        super(server, PlayerLobby.class);
    }

    @Override
    public void onJoin(PlayerLobby player, PlayerJoinEvent event) {
        player.setGameMode(GameMode.SURVIVAL);

        player.teleport(server.serverSpawn);
        player.bukkitPlayer.setBedSpawnLocation(server.serverSpawn, true);
    }

    @Override
    public void onDamage(PlayerLobby player, EntityDamageEvent event) {
        double x = player.getLocation().getX();
        double z = player.getLocation().getZ();
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID) {
            if(ServerLobby.SAFE_MIN_X <= x && x <= ServerLobby.SAFE_MAX_X && ServerLobby.SAFE_MIN_Z <= z && z <= ServerLobby.SAFE_MAX_Z) {
                event.setCancelled(true);
            } else {
                event.setDamage(0);
            }
        }
    }

    @Override
    public void onBlockBreak(PlayerLobby player, BlockBreakEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onBlockPlace(PlayerLobby player, BlockPlaceEvent event) {
        event.setCancelled(true);
    }
}
