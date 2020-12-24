package ca.zharry.MinecraftGamesServer.Players;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Servers.ServerDodgeball;
import ca.zharry.MinecraftGamesServer.Servers.ServerParkour;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PlayerDodgeball extends PlayerInterface {

    // Minigame variables
    public int totalKills = 0;
    public int kills = 0;
    public int lives = 0;
    public int arena = -1;
    public MCGTeam opponentTeam;

    public ServerDodgeball server;

    public PlayerDodgeball(Player bukkitPlayer, ServerDodgeball server) {
        super(bukkitPlayer, server, "dodgeball");
        this.server = server;

        try {
            totalKills = Integer.parseInt(currentMetadata);
        } catch (Exception ignore) {
        }
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

        objective.getScore(ChatColor.BLUE + "" + ChatColor.BOLD + "Game 2/6: " + ChatColor.RESET + "Dodgeball").setScore(14);

        if (server.state == ServerParkour.GAME_WAITING) {
            objective.getScore(ChatColor.WHITE + "Waiting for game start...").setScore(13);
        } else if (server.state == ServerParkour.GAME_STARTING) {
            objective.getScore(ChatColor.RED + "" + ChatColor.BOLD + "Game begins: " + ChatColor.RESET + server.timerStartGame.getString() + (server.timerStartGame.isPaused() ? " (Paused)" : "")).setScore(13);
        } else if (server.state == ServerParkour.GAME_INPROGRESS) {
            objective.getScore(ChatColor.RED + "" + ChatColor.BOLD + "Time left: " + ChatColor.RESET + server.timerInProgress.getString() + (server.timerInProgress.isPaused() ? " (Paused)" : "")).setScore(13);
            objective.getScore(ChatColor.GREEN + "" + ChatColor.BOLD + "Round: " + ChatColor.RESET + server.currentGame + "/" + server.totalGames).setScore(12);
        } else if (server.state == ServerParkour.GAME_FINISHED) {
            objective.getScore(ChatColor.RED + "" + ChatColor.BOLD + "Back to lobby: " + ChatColor.RESET + server.timerFinished.getString() + (server.timerFinished.isPaused() ? " (Paused)" : "")).setScore(13);
        }
        objective.getScore("").setScore(11);
        setGameScores(objective, 10, "dodgeball", myTeam.id);
        objective.getScore("  ").setScore(5);
        if (opponentTeam != null) {
            if (server.state == ServerParkour.GAME_STARTING) {
                objective.getScore(ChatColor.RED + "" + ChatColor.BOLD + "Next Opponent: ").setScore(4);
                objective.getScore(opponentTeam.chatColor + "" + ChatColor.BOLD + opponentTeam.teamname + " ").setScore(3);
                objective.getScore("   ").setScore(2);
            } else if (server.state == ServerParkour.GAME_INPROGRESS) {
                objective.getScore(ChatColor.RED + "" + ChatColor.BOLD + "Opponent: " + ChatColor.RESET + "" + opponentTeam.chatColor + "" + opponentTeam.teamname + " ").setScore(4);
                objective.getScore(ChatColor.WHITE + "" + ChatColor.BOLD + "Kills: " + ChatColor.RESET + "" + kills).setScore(3);
                objective.getScore(ChatColor.WHITE + "" + ChatColor.BOLD + "Lives: " + ChatColor.RESET + "" + lives).setScore(2);
            } else if (server.state == ServerParkour.GAME_FINISHED) {
                objective.getScore(ChatColor.WHITE + "" + ChatColor.BOLD + "Total Kills: " + ChatColor.RESET + "" + totalKills).setScore(3);
            }
        } else {
            objective.getScore(ChatColor.WHITE + "Please wait...").setScore(4);
            objective.getScore("   ").setScore(5);
        }
        objective.getScore(ChatColor.GREEN + "" + ChatColor.BOLD + "Your Score: " + ChatColor.RESET + "" + currentScore).setScore(1);

        this.bukkitPlayer.setScoreboard(scoreboard);
    }

    @Override
    public void commit() {
        currentMetadata = "" + totalKills;

        try {
            int id = -1;
            Statement statement = MCGMain.conn.connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM `scores` WHERE " +
                    "`uuid` = '" + bukkitPlayer.getUniqueId() + "' AND " +
                    "`season` = '" + MCGMain.SEASON + "' AND " +
                    "`minigame` = 'dodgeball';");
            while (resultSet.next()) {
                id = resultSet.getInt("id");
            }

            statement.execute("INSERT INTO `scores` " +
                    "(`id`, `uuid`, `season`, `minigame`, `score`, `metadata`) " +
                    "VALUES " +
                    "(" + (id == -1 ? "NULL" : id) + ", '" + bukkitPlayer.getUniqueId() + "', '" + MCGMain.SEASON + "', 'dodgeball', '" + currentScore + "', '" + currentMetadata + "')" +
                    "ON DUPLICATE KEY UPDATE" +
                    "`score` = " + currentScore + ", `metadata` = '" + currentMetadata + "', `time` = current_timestamp();");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
