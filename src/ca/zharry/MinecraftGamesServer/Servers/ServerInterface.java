package ca.zharry.MinecraftGamesServer.Servers;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public abstract class ServerInterface {

    public HashMap<String, String> minigames = new HashMap<String, String>();

    public JavaPlugin javaPlugin;
    public Plugin plugin;

    public BukkitTask taskScoreboard;
    public ArrayList<PlayerInterface> players;
    public HashMap<UUID, PlayerInterface> playerLookup;

    public ArrayList<Integer> teamIDs;
    public HashMap<Integer, MCGTeam> teams;
    public HashMap<UUID, Integer> teamLookup;

    public ServerInterface(JavaPlugin plugin) {
        this.javaPlugin = plugin;
        this.plugin = plugin;
        this.players = new ArrayList<>();
        this.playerLookup = new HashMap<UUID, PlayerInterface>();

        minigames.put("parkour", "Parkour");
        minigames.put("spleef", "Spleef");
        minigames.put("dodgeball", "Dodgeball");
        minigames.put("survivalgames", "Survival Games");

        this.teamIDs = new ArrayList<Integer>();
        this.teams = new HashMap<Integer, MCGTeam>();
        this.teamLookup = new HashMap<UUID, Integer>();
        this.getTeams();

        taskScoreboard = new BukkitRunnable() {
            @Override
            public void run() {
                for (PlayerInterface player : players) {
                    player.doStatsRefresh();
                }
            }
        }.runTaskTimer(plugin, 0, 5);
    }

    public void addPlayer(PlayerInterface player) {
        players.add(player);
        playerLookup.put(player.bukkitPlayer.getUniqueId(), player);
    }

    public ArrayList<MCGTeam> getOrderedTeams() {
        ArrayList<MCGTeam> res = new ArrayList<>(teams.values());
        res.sort((a, b) -> b.getScore() - a.getScore());
        return res;
    }

    public void getTeams() {
        this.teams.clear();

        try {
            Statement statement = MCGMain.conn.connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM `teams` WHERE `season` = " + MCGMain.SEASON + ";");
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String teamname = resultSet.getString("teamname").trim();
                String playerList = resultSet.getString("players").trim();
                String[] players = playerList.split(",");
                String color = resultSet.getString("color").trim();

                MCGMain.logger.info("Found team: " + teamname);
                MCGMain.logger.info("Color: " + color);
                MCGMain.logger.info("Playerlist: " + playerList);

                MCGTeam team = new MCGTeam(id, teamname, color, this);
                for (String uuid : players) {
                    team.addPlayer(UUID.fromString(uuid));
                    this.teamLookup.put(UUID.fromString(uuid), id);
                }

                this.teams.put(id, team);
                this.teamIDs.add(id);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onEnableCall() {
        this.registerCommands();
        this.registerListeners();
    }

    public void onDisableCall() {
        // Commit existing players (for hot-reloading)
        for (PlayerInterface player : players) {
            player.commit();
        }
    }

    public abstract void registerCommands();

    public abstract void registerListeners();

    public void sendPlayersToLobby() {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF("lobby");

        for (PlayerInterface player : players) {
            player.bukkitPlayer.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        }
    }

}
