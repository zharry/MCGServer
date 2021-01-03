package ca.zharry.MinecraftGamesServer.Players;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Servers.ServerParkour;
import org.bukkit.ChatColor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public class PlayerParkour extends PlayerInterface {

    // Minigame variables
    public int stage = 0; // What stage they are currently on
    public int level = 0; // What level of the stage they have completed

    public boolean waypointsEnabled = true;

    public ServerParkour server;

    public PlayerParkour(ServerParkour server, UUID uuid, String username) {
        super(server, uuid, username, "parkour");
        this.server = server;

        try {
            String[] metadata = currentMetadata.split("-");
            stage = Integer.parseInt(metadata[0]);
            level = Integer.parseInt(metadata[1]);
        } catch (Exception ignore) {
        }
    }

    @Override
    public void updateSidebar() {
        // This is a spacer
        sidebar.add("                          ");

        sidebar.add(ChatColor.BLUE + "" + ChatColor.BOLD + "Game 4/6: " + ChatColor.RESET + "Parkour");

        if (server.state == ServerParkour.GAME_WAITING) {
            sidebar.add(ChatColor.WHITE + "Waiting for game start...");
        } else if (server.state == ServerParkour.GAME_STARTING) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Game begins: " + ChatColor.RESET + server.timerStartGame.getString() + (server.timerStartGame.isPaused() ? " (Paused)" : ""));
        } else if (server.state == ServerParkour.GAME_INPROGRESS) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Time left: " + ChatColor.RESET + server.timerInProgress.getString() + (server.timerInProgress.isPaused() ? " (Paused)" : ""));
        } else if (server.state == ServerParkour.GAME_FINISHED) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Back to lobby: " + ChatColor.RESET + server.timerFinished.getString() + (server.timerFinished.isPaused() ? " (Paused)" : ""));
        }

        if (server.state == ServerParkour.GAME_INPROGRESS || server.state == ServerParkour.GAME_FINISHED) {
            sidebar.add("");
            sidebar.add(ChatColor.AQUA + "" + ChatColor.BOLD + "Completed Stage: " + ChatColor.RESET + stage + "-" + level);
        }
        sidebar.add("");

        setTeamScoresForSidebar("parkour", myTeam.id);
        sidebar.add("");
        sidebar.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Team Score: " + ChatColor.RESET + "" + myTeam.getScore("parkour"));
        sidebar.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Your Score: " + ChatColor.RESET + "" + currentScore);
        sidebar.end();
    }

    @Override
    public void commit() {
        currentMetadata = stage + "-" + level;

        try {
            int id = -1;
            Statement statement = MCGMain.conn.connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM `scores` WHERE " +
                    "`uuid` = '" + uuid + "' AND " +
                    "`season` = '" + MCGMain.SEASON + "' AND " +
                    "`minigame` = 'parkour';");
            while (resultSet.next()) {
                id = resultSet.getInt("id");
            }

            statement.execute("INSERT INTO `scores` " +
                    "(`id`, `uuid`, `season`, `minigame`, `score`, `metadata`) " +
                    "VALUES " +
                    "(" + (id == -1 ? "NULL" : id) + ", '" + uuid + "', '" + MCGMain.SEASON + "', 'parkour', '" + currentScore + "', '" + currentMetadata + "')" +
                    "ON DUPLICATE KEY UPDATE" +
                    "`score` = " + currentScore + ", `metadata` = '" + currentMetadata + "', `time` = current_timestamp();");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
