package ca.zharry.MinecraftGamesServer.Players;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Servers.ServerDodgeball;
import ca.zharry.MinecraftGamesServer.Servers.ServerParkour;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PlayerDodgeball extends PlayerInterface {

    // Minigame variables
    public int totalKills = 0;
    public int kills = 0;
    public int lives = 0;
    public int arena = -1;
    public boolean invulnerable = true;
    public MCGTeam opponentTeam;
    public Location lastDeathLocation;

    public ServerDodgeball server;

    public PlayerDodgeball(Player bukkitPlayer, ServerDodgeball server) {
        super(bukkitPlayer, server, "dodgeball");
        this.server = server;

        try {
            String[] metadataSplit = currentMetadata.split("\\|");
            totalKills = Integer.parseInt(metadataSplit[0]);
            invulnerable = Boolean.parseBoolean(metadataSplit[1]);
        } catch (Exception ignore) {
        }
    }

    @Override
    public void updateScoreboard() {
        // This is a spacer
        sidebar.add("                          ");

        sidebar.add(ChatColor.BLUE + "" + ChatColor.BOLD + "Game 2/6: " + ChatColor.RESET + "Dodgeball");

        if (server.state == ServerParkour.GAME_WAITING) {
            sidebar.add(ChatColor.WHITE + "Waiting for game start...");
        } else if (server.state == ServerParkour.GAME_STARTING) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Game begins: " + ChatColor.RESET + server.timerStartGame.getString() + (server.timerStartGame.isPaused() ? " (Paused)" : ""));
        } else if (server.state == ServerParkour.GAME_INPROGRESS) {

            if (bukkitPlayer.getGameMode() == GameMode.ADVENTURE)
                sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Time left: " + ChatColor.RESET + server.timerInProgress.getString() + (server.timerInProgress.isPaused() ? " (Paused)" : ""));
            else
                sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Waiting for others: " + ChatColor.RESET + server.timerInProgress.getString() + (server.timerInProgress.isPaused() ? " (Paused)" : ""));

            sidebar.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Round: " + ChatColor.RESET + server.currentGame + "/" + server.totalGames);
        } else if (server.state == ServerParkour.GAME_FINISHED) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Back to lobby: " + ChatColor.RESET + server.timerFinished.getString() + (server.timerFinished.isPaused() ? " (Paused)" : ""));
        }
        sidebar.add("");
        setGameScores("dodgeball", myTeam.id);
        sidebar.add("");
        if (opponentTeam != null) {
            if (server.state == ServerParkour.GAME_STARTING) {
                sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Next Opponent: ");
                sidebar.add(opponentTeam.chatColor + "" + ChatColor.BOLD + opponentTeam.teamname + " ");
                sidebar.add("   ");
            } else if (server.state == ServerParkour.GAME_INPROGRESS) {
                sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Opponent: " + ChatColor.RESET + "" + opponentTeam.chatColor + "" + opponentTeam.teamname + " ");
                sidebar.add(ChatColor.WHITE + "" + ChatColor.BOLD + "Kills: " + ChatColor.RESET + "" + kills);
                sidebar.add(ChatColor.WHITE + "" + ChatColor.BOLD + "Lives: " + ChatColor.RESET + "" + lives);
            } else if (server.state == ServerParkour.GAME_FINISHED) {
                sidebar.add(ChatColor.WHITE + "" + ChatColor.BOLD + "Total Kills: " + ChatColor.RESET + "" + totalKills);
            }
        } else {
            sidebar.add(ChatColor.WHITE + "Please wait...");
            sidebar.add("");
        }
        sidebar.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Your Score: " + ChatColor.RESET + "" + currentScore);
        sidebar.end();
    }

    @Override
    public void commit() {
        currentMetadata = totalKills + "|" + invulnerable;

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

    @Override
    public String getPlayerNameForTabMenu(Player player) {
        if(server.state == ServerDodgeball.GAME_INPROGRESS) {
            PlayerInterface playerInterface = server.playerLookup.get(player.getUniqueId());
            if (playerInterface instanceof PlayerDodgeball && ((PlayerDodgeball) playerInterface).lives <= 0) {
                return ChatColor.GRAY + player.getName();
            }
        }
        return super.getPlayerNameForTabMenu(player);
    }

}
