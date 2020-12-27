package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.Players.PlayerLobby;
import ca.zharry.MinecraftGamesServer.Servers.ServerLobby;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ListenerLobby implements Listener {

    ServerLobby server;

    public ListenerLobby(ServerLobby server) {
        this.server = server;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        server.addPlayer(new PlayerLobby(player, server));
        player.setGameMode(GameMode.ADVENTURE);

        Location serverSpawn = new Location(player.getWorld(), 1484.5, 4, 530);
        serverSpawn.setYaw(90);
        player.teleport(serverSpawn);
        player.setBedSpawnLocation(serverSpawn, true);
        player.setDisplayName(server.teams.get(server.teamLookup.get(player.getUniqueId())).chatColor + player.getName() + ChatColor.RESET);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        for (int i = 0; i < server.players.size(); i++) {
            PlayerLobby curPlayer = (PlayerLobby) server.players.get(i);
            if (curPlayer.bukkitPlayer.getUniqueId() == player.getUniqueId()) {
                curPlayer.commit();
                server.players.remove(i);
                break;
            }
        }
    }
}
