package ca.zharry.MinecraftGamesServer.Players;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Servers.ServerParkour;
import ca.zharry.MinecraftGamesServer.Servers.ServerDodgeball;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.sql.SQLException;
import java.sql.Statement;

public class PlayerDodgeball extends PlayerInterface {

    // Minigame variables
    public int currentScore = 0;
    public boolean dead = false;

    public ServerDodgeball server;
    public PlayerDodgeball(Player bukkitPlayer, ServerDodgeball server) {
        super(bukkitPlayer);
        this.server = server;
    }

    @Override
    public void updateScoreboard() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("scoreboard", "dummy", "MCG Season " + MCGMain.SEASON);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // This is a spacer
        objective.getScore("                          ").setScore(15);

        objective.getScore(ChatColor.BLUE + "" + ChatColor.BOLD + "Game 2/6: " + ChatColor.RESET + "Dodgeball").setScore(10);

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
        objective.getScore(ChatColor.GREEN + "" + ChatColor.BOLD + "Team Score: ").setScore(1);
        objective.getScore(ChatColor.GREEN + "" + ChatColor.BOLD + "Your Score: " + ChatColor.RESET + "" + currentScore).setScore(0);

        this.bukkitPlayer.setScoreboard(scoreboard);
    }

    @Override
    public void commit() {
        try {
            Statement statement = MCGMain.conn.connection.createStatement();
            statement.execute("INSERT INTO `scores` " +
                    "(`uuid`, `season`, `minigame`, `score`) " +
                    "VALUES " +
                    "('" + bukkitPlayer.getUniqueId() + "', '" + MCGMain.SEASON + "', 'dodgeball', '"+ currentScore + "');\n");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
