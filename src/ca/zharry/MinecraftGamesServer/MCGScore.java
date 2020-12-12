package ca.zharry.MinecraftGamesServer;

import java.util.UUID;

public class MCGScore {

    public int id;
    public UUID uuidFor;
    public int season;
    public String minigame;
    public int score;

    public MCGScore(int id, String uuid, int season, String minigame, int score) {
        this.id = id;
        this.uuidFor = UUID.fromString(uuid);
        this.season = season;
        this.minigame = minigame;
        this.score = score;
    }

}
