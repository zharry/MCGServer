package ca.zharry.MinecraftGamesServer.Players;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Servers.ServerSpleef;
import org.bukkit.ChatColor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public class PlayerSpleef extends PlayerInterface {

    // Minigame variables
    public boolean dead = false;

    public ServerSpleef server;

    public PlayerSpleef(ServerSpleef server, UUID uuid, String username) {
        super(server, uuid, username, "spleef");
        this.server = server;
    }

    @Override
    public void updateSidebar() {
        // This is a spacer
        sidebar.add("                          ");

        sidebar.add(ChatColor.BLUE + "" + ChatColor.BOLD + "Game 2/6: " + ChatColor.RESET + "Spleef");
        if (server.state == ServerSpleef.GAME_STARTING || server.state == ServerSpleef.GAME_INPROGRESS) {
            sidebar.add(ChatColor.BLUE + "" + ChatColor.BOLD + "Round: " + ChatColor.RESET + "" + server.currentGame + "/" + ServerSpleef.TOTAL_GAMES);
        }
        if (server.state == ServerSpleef.GAME_WAITING) {
            sidebar.add(ChatColor.WHITE + "Waiting for game start...");
        } else if (server.state == ServerSpleef.GAME_STARTING) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Round starts: " + ChatColor.RESET + server.timerStartGame.getString() + (server.timerStartGame.isPaused() ? " (Paused)" : ""));
        } else if (server.state == ServerSpleef.GAME_BEGIN && server.firstRun) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Game begins: " + ChatColor.RESET + server.timerStartGame.getString() + (server.timerStartGame.isPaused() ? " (Paused)" : ""));
        } else if (server.state == ServerSpleef.GAME_BEGIN) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Game begins: " + ChatColor.RESET + server.timerBegin.getString() + (server.timerBegin.isPaused() ? " (Paused)" : ""));
        } else if (server.state == ServerSpleef.GAME_INPROGRESS) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Time left: " + ChatColor.RESET + server.timerInProgress.getString() + (server.timerInProgress.isPaused() ? " (Paused)" : ""));
        } else if (server.state == ServerSpleef.GAME_FINISHED) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Back to lobby: " + ChatColor.RESET + server.timerFinished.getString() + (server.timerFinished.isPaused() ? " (Paused)" : ""));
        }
        sidebar.add("");
        setTeamScoresForSidebar("spleef", myTeam.id);
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
                    "`uuid` = '" + uuid + "' AND " +
                    "`season` = '" + MCGMain.SEASON + "' AND " +
                    "`minigame` = 'spleef';");
            while (resultSet.next()) {
                id = resultSet.getInt("id");
            }

            statement.execute("INSERT INTO `scores` " +
                    "(`id`, `uuid`, `season`, `minigame`, `score`) " +
                    "VALUES " +
                    "(" + (id == -1 ? "NULL" : id) + ", '" + uuid + "', '" + MCGMain.SEASON + "', 'spleef', '" + currentScore + "')" +
                    "ON DUPLICATE KEY UPDATE" +
                    "`score` = " + currentScore + ";");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getPlayerNameForTabMenu() {
        if (this.dead) {
            return super.getPlayerNameForTabMenu(true);
        }
        return super.getPlayerNameForTabMenu();
    }

}
