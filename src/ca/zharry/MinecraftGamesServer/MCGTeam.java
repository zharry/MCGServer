package ca.zharry.MinecraftGamesServer;

import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Servers.ServerInterface;

import java.util.ArrayList;
import java.util.UUID;

public class MCGTeam {

    public int id;
    public String teamname;
    public String color;
    public ArrayList<UUID> players;

    private ServerInterface server;

    public MCGTeam(int id, String teamname, String color, ServerInterface server) {
        this.id = id;
        this.teamname = teamname;
        this.color = color;
        players = new ArrayList<UUID>();

        this.server = server;
    }

    public void addPlayer(UUID uuid) {
        players.add(uuid);
    }

    public int getScore() {
        int val = 0;
        for (PlayerInterface player: server.players) {
            if (players.contains(player.bukkitPlayer.getUniqueId())) {
                val += player.getScore();
            }
        }
        return val;
    }

    public int getScore(String minigame) {
        int val = 0;
        for (PlayerInterface player: server.players) {
            if (players.contains(player.bukkitPlayer.getUniqueId())) {
                val += player.getScore(minigame);
            }
        }
        return val;
    }

}
