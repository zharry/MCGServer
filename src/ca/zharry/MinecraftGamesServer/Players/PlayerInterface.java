package ca.zharry.MinecraftGamesServer.Players;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.MCGScore;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

public abstract class PlayerInterface {

    // Scores
    public String curMinigame = "lobby";
    public int currentScore = 0;
    public ArrayList<MCGScore> previousScores;

    public Player bukkitPlayer;
    public PlayerInterface(Player bukkitPlayer, String curMinigame) {
        this.bukkitPlayer = bukkitPlayer;
        this.curMinigame = curMinigame;

        this.previousScores = new ArrayList<MCGScore>();
        getData();
    }

    public abstract void updateScoreboard();
    public abstract void commit();

    public void getData() {
        this.previousScores.clear();

        try {
            Statement statement = MCGMain.conn.connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM `scores` WHERE `uuid` = '" + bukkitPlayer.getUniqueId() + "';");
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String uuid = resultSet.getString("uuid").trim();
                int season = resultSet.getInt("season");
                String minigame = resultSet.getString("minigame").trim();
                int score = resultSet.getInt("score");

                if (season == MCGMain.SEASON && minigame.equals(curMinigame)) {
                    currentScore = score;
                } else {
                    MCGScore newScore = new MCGScore(id, uuid, season, minigame, score);
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
