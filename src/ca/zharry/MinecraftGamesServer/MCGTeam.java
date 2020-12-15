package ca.zharry.MinecraftGamesServer;

import ca.zharry.MinecraftGamesServer.Servers.ServerInterface;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.UUID;

public class MCGTeam {

    public int id;
    public String teamname;
    public String color;
    public ChatColor chatColor;
    public ArrayList<UUID> players;

    private ServerInterface server;

    public MCGTeam(int id, String teamname, String color, ServerInterface server) {
        this.id = id;
        this.teamname = teamname;
        this.color = color;
        this.chatColor = ChatColor.valueOf(color);
        players = new ArrayList<UUID>();

        this.server = server;
    }

    public void addPlayer(UUID uuid) {
        players.add(uuid);
    }

    public int getScore() {
        int val = 0;
        for (UUID uuid : players) {
            if (server.playerLookup.containsKey(uuid))
                val += server.playerLookup.get(uuid).getScore();
        }
        return val;
    }

    public int getScore(String minigame) {
        int val = 0;
        for (UUID uuid : players) {
            if (server.playerLookup.containsKey(uuid))
                val += server.playerLookup.get(uuid).getScore(minigame);
        }
        return val;
    }

}
