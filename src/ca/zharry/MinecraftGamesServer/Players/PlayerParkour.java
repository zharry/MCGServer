package ca.zharry.MinecraftGamesServer.Players;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Servers.ServerParkour;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PlayerParkour extends PlayerInterface {

    // Minigame variables
    public int stage = 0; // What stage they are currently on
    public int level = 0; // What level of the stage they have completed

    public ServerParkour server;
    public PlayerParkour(Player bukkitPlayer, ServerParkour server) {
        super(bukkitPlayer, server, "parkour");
        this.server = server;

        try {
            String[] metadata = currentMetadata.split("-");
            stage = Integer.parseInt(metadata[0]);
            level = Integer.parseInt(metadata[1]);
        } catch (Exception ignore) {}
    }

    @Override
    public void updateScoreboard() {
        try {
            scoreboard.getObjective("scoreboard").unregister();
        } catch (Exception ignored) {
        }
        Objective objective = scoreboard.registerNewObjective("scoreboard", "dummy", "MCG Season " + MCGMain.SEASON);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // This is a spacer
        objective.getScore("                          ").setScore(15);

        objective.getScore(ChatColor.BLUE + "" + ChatColor.BOLD + "Game 4/6: " + ChatColor.RESET + "Parkour").setScore(10);

        if (server.state == ServerParkour.GAME_WAITING) {
            objective.getScore(ChatColor.WHITE + "Waiting for game start...").setScore(9);
        } else if (server.state == ServerParkour.GAME_STARTING) {
            objective.getScore(ChatColor.RED + "" + ChatColor.BOLD + "Game begins: " + ChatColor.RESET + server.timerStartGame.getString() + (server.timerStartGame.isPaused() ? " (Paused)" : "")).setScore(9);
        } else if (server.state == ServerParkour.GAME_INPROGRESS) {
            objective.getScore(ChatColor.RED + "" + ChatColor.BOLD + "Time left: " + ChatColor.RESET + server.timerInProgress.getString() + (server.timerInProgress.isPaused() ? " (Paused)" : "")).setScore(9);
        } else if (server.state == ServerParkour.GAME_FINISHED) {
            objective.getScore(ChatColor.RED + "" + ChatColor.BOLD + "Back to lobby: " + ChatColor.RESET + server.timerFinished.getString() + (server.timerFinished.isPaused() ? " (Paused)" : "")).setScore(9);
        }
        objective.getScore("").setScore(8);
        objective.getScore(ChatColor.BLUE + "" + ChatColor.BOLD + "Game scores: ").setScore(7);
        objective.getScore(ChatColor.WHITE + " 1. Team 1").setScore(6);
        objective.getScore(ChatColor.WHITE + " 4. Team 4").setScore(5);
        objective.getScore(ChatColor.WHITE + " 5. " + ChatColor.BOLD + "Team 5").setScore(4);
        objective.getScore(ChatColor.WHITE + " 6. Team 6").setScore(3);
        objective.getScore("  ").setScore(2);
        objective.getScore(ChatColor.GREEN + "" + ChatColor.BOLD + "Team Score: " + ChatColor.RESET + "" + myTeam.getScore("parkour")).setScore(1);
        objective.getScore(ChatColor.GREEN + "" + ChatColor.BOLD + "Your Score: " + ChatColor.RESET + "" + currentScore).setScore(0);

        this.bukkitPlayer.setScoreboard(scoreboard);
    }

    @Override
    public void commit() {
        currentMetadata = stage + "-" + level;

        try {
            int id = -1;
            Statement statement = MCGMain.conn.connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM `scores` WHERE " +
                    "`uuid` = '" + bukkitPlayer.getUniqueId() + "' AND " +
                    "`season` = '" + MCGMain.SEASON + "' AND " +
                    "`minigame` = 'parkour';");
            while (resultSet.next()) {
                id = resultSet.getInt("id");
            }

            statement.execute("INSERT INTO `scores` " +
                    "(`id`, `uuid`, `season`, `minigame`, `score`, `metadata`) " +
                    "VALUES " +
                    "(" + (id == -1 ? "NULL" : id) + ", '" + bukkitPlayer.getUniqueId() + "', '" + MCGMain.SEASON + "', 'parkour', '"+ currentScore + "', '" + currentMetadata + "')" +
                    "ON DUPLICATE KEY UPDATE" +
                    "`score` = " + currentScore + ", `metadata` = '" + currentMetadata + "', `time` = current_timestamp();");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
