package ca.zharry.MinecraftGamesServer;

import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Servers.ServerInterface;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.UUID;

public class MCGTeam {

    public int id;
    public String teamname;
    public String color;
    public ChatColor chatColor;
    public ArrayList<UUID> players = new ArrayList<>();

    private ServerInterface server;

    public MCGTeam(int id, String teamname, String color, ServerInterface server) {
        this.id = id;
        this.teamname = teamname;
        this.color = color;
        this.chatColor = ChatColor.valueOf(color);

        this.server = server;
    }

    public void addPlayer(UUID uuid) {
        if (!players.contains(uuid))
            players.add(uuid);
    }
//
//    public void removePlayer(UUID uuid) {
//        players.remove(uuid);
//    }

    public int getScore() {
        int val = 0;
        for (UUID uuid : players) {
            PlayerInterface player = server.getPlayerFromUUID(uuid);
            if(player != null) {
                val += player.getScore();
            }
        }
        return val;
    }

    public int getScore(String minigame) {
        int val = 0;
        for (UUID uuid : players) {
            PlayerInterface player = server.getPlayerFromUUID(uuid);
            if(player != null) {
                val += player.getScore(minigame);
            }
        }
        return val;
    }

    public String toString() {
        return chatColor + teamname + ChatColor.RESET;
    }

}
