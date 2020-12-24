package ca.zharry.MinecraftGamesServer.Listeners;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Players.PlayerSurvivalGames;
import ca.zharry.MinecraftGamesServer.Servers.ServerSurvivalGames;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class ListenerOnPlayerDeathSurvivalGames implements Listener {

    ServerSurvivalGames server;

    public ListenerOnPlayerDeathSurvivalGames(ServerSurvivalGames server) {
        this.server = server;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (server.state == ServerSurvivalGames.GAME_INPROGRESS) {
            // Find the dead player
            Player deadPlayer = event.getEntity();
            PlayerSurvivalGames player = (PlayerSurvivalGames) server.playerLookup.get(deadPlayer.getUniqueId());

            // Mark them as dead and upload their score
            server.dead.add(player);
            player.dead = true;
            player.commit();
            MCGMain.logger.info(player.bukkitPlayer.getName() + " DEAD");

            // Award all non-dead players some score
            for (PlayerInterface p : server.players) {
                PlayerSurvivalGames playerSurvivalGames = (PlayerSurvivalGames) p;
                if (!playerSurvivalGames.dead) {
                    playerSurvivalGames.currentScore += 50;
                }
            }

            // Award killer some score
            Player k = event.getEntity().getPlayer().getKiller();
            if (k != null) {
                PlayerSurvivalGames killer = (PlayerSurvivalGames) server.playerLookup.get(k.getUniqueId());
                killer.kills += 1;
                killer.currentScore += 50;
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        player.setGameMode(GameMode.ADVENTURE);
        Location serverSpawn = new Location(player.getWorld(), 0.5, 176, 0.5);
        player.teleport(serverSpawn);
        player.getInventory().clear();
    }

}
