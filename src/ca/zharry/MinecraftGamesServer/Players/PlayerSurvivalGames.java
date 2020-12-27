package ca.zharry.MinecraftGamesServer.Players;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Servers.ServerSurvivalGames;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PlayerSurvivalGames extends PlayerInterface {

    // Minigame variables
    public boolean dead = false;
    public int kills = 0;
    public Location deathLocation;

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
        // This is a spacer
        sidebar.add("                          ");

        sidebar.add(ChatColor.BLUE + "" + ChatColor.BOLD + "Game 1/6: " + ChatColor.RESET + "Survival Games");

        if (server.state == ServerSurvivalGames.GAME_WAITING) {
            sidebar.add(ChatColor.WHITE + "Waiting for game start...");
        } else if (server.state == ServerSurvivalGames.GAME_STARTING) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Teleporting: " + ChatColor.RESET + server.timerStartGame.getString() + (server.timerStartGame.isPaused() ? " (Paused)" : ""));
        } else if (server.state == ServerSurvivalGames.GAME_BEGIN) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Game Begins: " + ChatColor.RESET + server.timerBegin.getString() + (server.timerBegin.isPaused() ? " (Paused)" : ""));
        } else if (server.state == ServerSurvivalGames.GAME_INPROGRESS) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Time left: " + ChatColor.RESET + server.timerInProgress.getString() + (server.timerInProgress.isPaused() ? " (Paused)" : ""));
            sidebar.add(ChatColor.BLUE + "" + ChatColor.BOLD + "Next event: " + ChatColor.RESET + "" + server.getNextEvent());
            sidebar.add(ChatColor.WHITE + "" + ChatColor.BOLD + "World Border: " + ChatColor.RESET + "" + server.getWorldBorder());
        } else if (server.state == ServerSurvivalGames.GAME_FINISHED) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Back to lobby: " + ChatColor.RESET + server.timerFinished.getString() + (server.timerFinished.isPaused() ? " (Paused)" : ""));
        }
        sidebar.add("");
        setGameScores("survivalgames", myTeam.id);
        sidebar.add("");
        sidebar.add(ChatColor.BLUE + "" + ChatColor.BOLD + "Still alive: " + ChatColor.RESET + "" + server.getPlayersAlive());
        sidebar.add(ChatColor.WHITE + "" + ChatColor.BOLD + "Kills: " + ChatColor.RESET + "" + kills);
        sidebar.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Your Score: " + ChatColor.RESET + "" + currentScore);
        sidebar.end();
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

    @Override
    public String getPlayerNameFormatted(Player player) {
        PlayerInterface playerInterface = server.playerLookup.get(player.getUniqueId());
        if(playerInterface instanceof PlayerSurvivalGames && ((PlayerSurvivalGames) playerInterface).dead) {
            return "§7" + player.getName();
        }
        return super.getPlayerNameFormatted(player);
    }
}
