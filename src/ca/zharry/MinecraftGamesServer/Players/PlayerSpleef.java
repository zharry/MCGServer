package ca.zharry.MinecraftGamesServer.Players;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Servers.ServerParkour;
import ca.zharry.MinecraftGamesServer.Servers.ServerSpleef;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PlayerSpleef extends PlayerInterface {

    // Minigame variables
    public boolean dead = false;

    public ServerSpleef server;

    public PlayerSpleef(Player bukkitPlayer, ServerSpleef server) {
        super(bukkitPlayer, server, "spleef");
        this.server = server;
    }

    @Override
    public void updateScoreboard() {
        // This is a spacer
        sidebar.add("                          ");

        sidebar.add(ChatColor.BLUE + "" + ChatColor.BOLD + "Game 2/6: " + ChatColor.RESET + "Spleef");
        if (server.state == ServerParkour.GAME_STARTING || server.state == ServerParkour.GAME_INPROGRESS) {
            sidebar.add(ChatColor.BLUE + "" + ChatColor.BOLD + "Round: " + ChatColor.RESET + "" + server.currentGame + "/" + ServerSpleef.TOTAL_GAMES);
        }
        if (server.state == ServerParkour.GAME_WAITING) {
            sidebar.add(ChatColor.WHITE + "Waiting for game start...");
        } else if (server.state == ServerParkour.GAME_STARTING) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Game begins: " + ChatColor.RESET + server.timerStartGame.getString() + (server.timerStartGame.isPaused() ? " (Paused)" : ""));
        } else if (server.state == ServerParkour.GAME_INPROGRESS) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Time left: " + ChatColor.RESET + server.timerInProgress.getString() + (server.timerInProgress.isPaused() ? " (Paused)" : ""));
        } else if (server.state == ServerParkour.GAME_FINISHED) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Back to lobby: " + ChatColor.RESET + server.timerFinished.getString() + (server.timerFinished.isPaused() ? " (Paused)" : ""));
        }
        sidebar.add("");
        setGameScores("spleef", myTeam.id);
        sidebar.add("  ");
        sidebar.add(ChatColor.BLUE + "" + ChatColor.BOLD + "Still alive: " + ChatColor.RESET + "" + server.getPlayersAlive());
        sidebar.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Team Score: " + ChatColor.RESET + "" + myTeam.getScore("spleef"));
        sidebar.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Your Score: " + ChatColor.RESET + "" + currentScore);
        sidebar.end();
    }

    @Override
    public void commit() {
        try {
            int id = -1;
            Statement statement = MCGMain.conn.connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM `scores` WHERE " +
                    "`uuid` = '" + bukkitPlayer.getUniqueId() + "' AND " +
                    "`season` = '" + MCGMain.SEASON + "' AND " +
                    "`minigame` = 'spleef';");
            while (resultSet.next()) {
                id = resultSet.getInt("id");
            }

            statement.execute("INSERT INTO `scores` " +
                    "(`id`, `uuid`, `season`, `minigame`, `score`) " +
                    "VALUES " +
                    "(" + (id == -1 ? "NULL" : id) + ", '" + bukkitPlayer.getUniqueId() + "', '" + MCGMain.SEASON + "', 'spleef', '" + currentScore + "')" +
                    "ON DUPLICATE KEY UPDATE" +
                    "`score` = " + currentScore + ";");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getPlayerNameForTabMenu(Player player) {
        PlayerInterface playerInterface = server.playerLookup.get(player.getUniqueId());
        if(playerInterface instanceof PlayerSpleef && ((PlayerSpleef) playerInterface).dead) {
            return "§7" + player.getName();
        }
        return super.getPlayerNameForTabMenu(player);
    }

}
