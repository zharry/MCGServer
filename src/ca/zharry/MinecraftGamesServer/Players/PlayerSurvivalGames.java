package ca.zharry.MinecraftGamesServer.Players;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Servers.ServerSurvivalGames;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PlayerSurvivalGames extends PlayerInterface {

    // Minigame variables
    public boolean dead = false;
    public int kills = 0;

    public ServerSurvivalGames server;

    public PlayerSurvivalGames(Player bukkitPlayer, ServerSurvivalGames server) {
        super(bukkitPlayer, server, "survivalgames");
        this.server = server;

        try {
            kills = Integer.parseInt(currentMetadata);
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

        objective.getScore(ChatColor.BLUE + "" + ChatColor.BOLD + "Game 1/6: " + ChatColor.RESET + "Survival Games").setScore(14);

        if (server.state == ServerSurvivalGames.GAME_WAITING) {
            objective.getScore(ChatColor.WHITE + "Waiting for game start...").setScore(13);
        } else if (server.state == ServerSurvivalGames.GAME_STARTING) {
            objective.getScore(ChatColor.RED + "" + ChatColor.BOLD + "Teleporting: " + ChatColor.RESET + server.timerStartGame.getString() + (server.timerStartGame.isPaused() ? " (Paused)" : "")).setScore(13);
        } else if (server.state == ServerSurvivalGames.GAME_BEGIN) {
            objective.getScore(ChatColor.RED + "" + ChatColor.BOLD + "Game Begins: " + ChatColor.RESET + server.timerBegin.getString() + (server.timerBegin.isPaused() ? " (Paused)" : "")).setScore(13);
        } else if (server.state == ServerSurvivalGames.GAME_INPROGRESS) {
            objective.getScore(ChatColor.RED + "" + ChatColor.BOLD + "Time left: " + ChatColor.RESET + server.timerInProgress.getString() + (server.timerInProgress.isPaused() ? " (Paused)" : "")).setScore(13);
            objective.getScore(ChatColor.BLUE + "" + ChatColor.BOLD + "Next event: " + ChatColor.RESET + "" + server.getNextEvent()).setScore(12);
            objective.getScore(ChatColor.WHITE + "" + ChatColor.BOLD + "World Border: " + ChatColor.RESET + "" + server.getWorldBorder()).setScore(11);
        } else if (server.state == ServerSurvivalGames.GAME_FINISHED) {
            objective.getScore(ChatColor.RED + "" + ChatColor.BOLD + "Back to lobby: " + ChatColor.RESET + server.timerFinished.getString() + (server.timerFinished.isPaused() ? " (Paused)" : "")).setScore(13);
        }
        objective.getScore("").setScore(10);
        setGameScores(objective, 9, "survivalgames", myTeam.id);
        objective.getScore("  ").setScore(4);
        objective.getScore(ChatColor.BLUE + "" + ChatColor.BOLD + "Still alive: " + ChatColor.RESET + "" + server.getPlayersAlive()).setScore(3);
        objective.getScore(ChatColor.WHITE + "" + ChatColor.BOLD + "Kills: " + ChatColor.RESET + "" + kills).setScore(2);
        objective.getScore(ChatColor.GREEN + "" + ChatColor.BOLD + "Your Score: " + ChatColor.RESET + "" + currentScore).setScore(1);

        this.bukkitPlayer.setScoreboard(scoreboard);
    }

    @Override
    public void commit() {
        currentMetadata = "" + kills;

        try {
            int id = -1;
            Statement statement = MCGMain.conn.connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM `scores` WHERE " +
                    "`uuid` = '" + bukkitPlayer.getUniqueId() + "' AND " +
                    "`season` = '" + MCGMain.SEASON + "' AND " +
                    "`minigame` = 'survivalgames';");
            while (resultSet.next()) {
                id = resultSet.getInt("id");
            }

            statement.execute("INSERT INTO `scores` " +
                    "(`id`, `uuid`, `season`, `minigame`, `score`, `metadata`) " +
                    "VALUES " +
                    "(" + (id == -1 ? "NULL" : id) + ", '" + bukkitPlayer.getUniqueId() + "', '" + MCGMain.SEASON + "', 'survivalgames', '" + currentScore + "', '" + currentMetadata + "')" +
                    "ON DUPLICATE KEY UPDATE" +
                    "`score` = " + currentScore + ", `metadata` = '" + currentMetadata + "', `time` = current_timestamp();");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
