package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.Players.PlayerParkour;
import ca.zharry.MinecraftGamesServer.Servers.ServerParkour;
import javafx.geometry.Point3D;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ListenerOnPlayerJoinParkour implements Listener {

    ServerParkour server;
    public ListenerOnPlayerJoinParkour(ServerParkour server) {
        this.server = server;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerParkour playerParkour = new PlayerParkour(player, server);
        server.players.add(playerParkour);
        player.setGameMode(GameMode.ADVENTURE);

        if (server.state == ServerParkour.GAME_WAITING || server.state == ServerParkour.GAME_STARTING) {
            Location serverSpawn = new Location(player.getWorld(), 253.5, 134, -161.5);
            player.teleport(serverSpawn);
            player.setBedSpawnLocation(serverSpawn, true);
        }
        if (server.state == ServerParkour.GAME_INPROGRESS) {
            // Teleport player to last checkpoint
            Point3D checkpointLoc = ServerParkour.stage1Checkpoints.get(0);
            switch (playerParkour.stage) {
                case 1:
                    checkpointLoc = ServerParkour.stage1Checkpoints.get(playerParkour.level);
                    break;
                case 2:
                    checkpointLoc = ServerParkour.stage2Checkpoints.get(playerParkour.level);
                    break;
                case 3:
                    checkpointLoc = ServerParkour.stage3Checkpoints.get(playerParkour.level);
                    break;
                case 4:
                    checkpointLoc = ServerParkour.stage4Checkpoints.get(playerParkour.level);
                    break;
                case 5:
                    checkpointLoc = ServerParkour.stage5Checkpoints.get(playerParkour.level);
                    break;
                case 6:
                    checkpointLoc = ServerParkour.stage6Checkpoints.get(playerParkour.level);
                    break;
                default:
            }
            Location nextStage = new Location(player.getWorld(), checkpointLoc.getX() + 0.5, checkpointLoc.getY() + 1, checkpointLoc.getZ() + 0.5);
            player.teleport(nextStage);
        }
        if (server.state == ServerParkour.GAME_FINISHED) {
            Location gameSpectate = new Location(player.getWorld(), 8.5, 131, 9.5);
            player.teleport(gameSpectate);
            player.setBedSpawnLocation(gameSpectate, true);
        }
    }
}
