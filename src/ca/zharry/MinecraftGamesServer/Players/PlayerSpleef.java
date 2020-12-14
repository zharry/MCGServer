package ca.zharry.MinecraftGamesServer.Players;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Servers.ServerParkour;
import ca.zharry.MinecraftGamesServer.Servers.ServerSpleef;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

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

        bukkitPlayer.setBedSpawnLocation(new Location(bukkitPlayer.getWorld(), 14.5, 75, 17.5), true);
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

        objective.getScore(ChatColor.BLUE + "" + ChatColor.BOLD + "Game 2/6: " + ChatColor.RESET + "Spleef").setScore(10);

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
        objective.getScore(ChatColor.GREEN + "" + ChatColor.BOLD + "Team Score: " + ChatColor.RESET + "" + myTeam.getScore("spleef")).setScore(1);
        objective.getScore(ChatColor.GREEN + "" + ChatColor.BOLD + "Your Score: " + ChatColor.RESET + "" + currentScore).setScore(0);

        this.bukkitPlayer.setScoreboard(scoreboard);
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
                    "(" + (id == -1 ? "NULL" : id) + ", '" + bukkitPlayer.getUniqueId() + "', '" + MCGMain.SEASON + "', 'spleef', '"+ currentScore + "')" +
                    "ON DUPLICATE KEY UPDATE" +
                    "`score` = " + currentScore + ";");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
