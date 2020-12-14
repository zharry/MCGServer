package ca.zharry.MinecraftGamesServer.Players;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.MCGScore;
import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Servers.ServerInterface;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

public abstract class PlayerInterface {

    // Scores
    public String curMinigame = "lobby";
    public int currentScore = 0;
    public String currentMetadata = "";
    public ArrayList<MCGScore> previousScores;

    public Player bukkitPlayer;
    public ServerInterface server;
    public Scoreboard scoreboard;
    public MCGTeam myTeam;
    public PlayerInterface(Player bukkitPlayer, ServerInterface server, String curMinigame) {
        this.bukkitPlayer = bukkitPlayer;
        this.server = server;
        this.curMinigame = curMinigame;

        this.previousScores = new ArrayList<MCGScore>();
        getData();

        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.bukkitPlayer.setScoreboard(scoreboard);
        myTeam = server.teams.get(server.teamLookup.get(bukkitPlayer.getUniqueId()));

        // For all other players
        for (PlayerInterface player: server.players) {
            // Get their team
            MCGTeam playerTeam = server.teams.get(server.teamLookup.get(player.bukkitPlayer.getUniqueId()));

            // Add their team to our own scoreboard
            addPlayerTeamToScoreboard(scoreboard, playerTeam, player);

            // Add our entry to their scoreboard
            addPlayerTeamToScoreboard(player.scoreboard, myTeam, this);
        }
        // Add our entry to our scoreboard
        addPlayerTeamToScoreboard(scoreboard, myTeam, this);

    }

    private void addPlayerTeamToScoreboard (Scoreboard scoreboard, MCGTeam team, PlayerInterface player) {
        Team minecraftTeam = scoreboard.getTeam(team.teamname);
        if (minecraftTeam == null) {
            minecraftTeam = scoreboard.registerNewTeam(team.teamname);
        }
        minecraftTeam.addEntry(player.bukkitPlayer.getName());
        minecraftTeam.setColor(team.chatColor);
        minecraftTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
    }

    public abstract void updateScoreboard();
    public abstract void commit();

    public void getData() {
        this.previousScores.clear();

        try {
            Statement statement = MCGMain.conn.connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM `scores` WHERE `uuid` = '" + bukkitPlayer.getUniqueId() + "';");
            while (resultSet .next()) {
                int id = resultSet.getInt("id");
                String uuid = resultSet.getString("uuid").trim();
                int season = resultSet.getInt("season");
                String minigame = resultSet.getString("minigame").trim();
                int score = resultSet.getInt("score");
                String metadata = resultSet.getString("metadata");

                if (season == MCGMain.SEASON && minigame.equals(curMinigame)) {
                    currentScore = score;
                    currentMetadata = metadata;
                } else {
                    MCGScore newScore = new MCGScore(id, uuid, season, minigame, score, metadata);
                    this.previousScores.add(newScore);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getScore() {
        int val = currentScore;
        for (MCGScore score: previousScores) {
            if (score.season == MCGMain.SEASON)
                val += score.score;
        }
        return val;
    }

    public int getScore(String minigame) {
        int val = 0;
        if (minigame.equals(curMinigame)) {
            val += currentScore;
        }
        for (MCGScore score: previousScores) {
            if (score.minigame.equals(minigame)) {
                if (score.season == MCGMain.SEASON)
                    val += score.score;
            }
        }
        return val;
    }

}
