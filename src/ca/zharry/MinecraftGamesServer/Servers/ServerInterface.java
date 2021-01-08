package ca.zharry.MinecraftGamesServer.Servers;

import ca.zharry.MinecraftGamesServer.Listeners.ChangeGameRule;
import ca.zharry.MinecraftGamesServer.Listeners.ListenerJoinQuit;
import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Timer.Timer;
import ca.zharry.MinecraftGamesServer.Utils.SQLQueryUtil;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class ServerInterface {

    public HashMap<String, String> minigames = new HashMap<String, String>();

    public JavaPlugin plugin;
    public BukkitTask taskScoreboard;

    public World world;
    public Location serverSpawn;

    public ArrayList<PlayerInterface> players = new ArrayList<>();
    public HashMap<UUID, PlayerInterface> playerLookup = new HashMap<>();
    public ArrayList<PlayerInterface> offlinePlayers = new ArrayList<>();
    public HashMap<UUID, PlayerInterface> offlinePlayerLookup = new HashMap<>();

    public ArrayList<Integer> teamIDs = new ArrayList<>();
    private HashMap<Integer, MCGTeam> teams = new HashMap<>(); // Team ID to MCGTeam Mapping
    private HashMap<UUID, Integer> teamLookup = new HashMap<>(); // Player UUID to Team ID Mapping
    public MCGTeam defaultTeam;

    public ServerInterface(JavaPlugin plugin) {
        this.plugin = plugin;

        world = plugin.getServer().getWorld("world");

        minigames.put("parkour", "Parkour");
        minigames.put("spleef", "Spleef");
        minigames.put("dodgeball", "Dodgeball");
        minigames.put("survivalgames", "Survival Games");

        defaultTeam = new MCGTeam(0, "No Team", "WHITE", this);

        taskScoreboard = new BukkitRunnable() {
            @Override
            public void run() {
                for (PlayerInterface player : players) {
                    player.doStatsRefresh();
                }
            }
        }.runTaskTimer(plugin, 0, 5);

        reloadTeamsAndPlayers();

        applyGameRules(world);
        plugin.getServer().getPluginManager().registerEvents(new ChangeGameRule(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ListenerJoinQuit(this), plugin);
    }

    /* ABSTRACT FUNCTIONS */

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

    public void applyGameRules(World world) {
    }

    public abstract PlayerInterface createNewPlayerInterface(UUID uuid, String name);

    /* PLAYER LOGIC */

    public PlayerInterface getPlayerFromUUID(UUID uuid) {
        if(playerLookup.containsKey(uuid)) {
            return playerLookup.get(uuid);
        }
        else if(offlinePlayerLookup.containsKey(uuid)) {
            return offlinePlayerLookup.get(uuid);
        }
        return null;
    }

    public void playerJoin(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerInterface playerInterface = offlinePlayerLookup.get(uuid);
        System.out.println(playerInterface);
        if(playerInterface != null) {
            players.add(playerInterface);
            playerLookup.put(uuid, playerInterface);
            offlinePlayers.remove(playerInterface);
            offlinePlayerLookup.remove(uuid);
            playerInterface.fetchData();
        } else {
            playerInterface = createNewPlayerInterface(player.getUniqueId(), player.getName());
            players.add(playerInterface);
            playerLookup.put(uuid, playerInterface);
        }
        playerInterface.playerJoin(player);
    }

    public void playerQuit(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerInterface playerInterface = playerLookup.get(uuid);
        playerInterface.playerQuit(player);
        offlinePlayers.add(playerInterface);
        offlinePlayerLookup.put(uuid, playerInterface);
        players.remove(playerInterface);
        playerLookup.remove(uuid);
    }

    /* PROXY LOGIC */

    public void sendPlayersToLobby() {
        sendPlayersToGame("lobby");
    }

    public void sendPlayersToGame(String minigame) {
        if(players.size() > 0) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("PlayerList");
            out.writeUTF("ALL");
            MCGMain.serverToSendAll = minigame;
            PlayerInterface player = players.get(0);
            player.bukkitPlayer.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        } else {
            System.err.println("There are no players connected to this server, cannot send");
        }
    }

    /* TEAM LOGIC */

    public void fetchTeams() {
        this.teams.clear();
        this.teamLookup.clear();
        this.teamIDs.clear();

        teams.put(defaultTeam.id, defaultTeam);

        try {
            Statement statement = MCGMain.conn.connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM `teams` WHERE `season` = " + MCGMain.SEASON + ";");
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String teamname = resultSet.getString("teamname").trim();
                String playerList = resultSet.getString("players").trim();
                String[] players = playerList.split(",");
                if(playerList.trim().length() == 0) {
                    players = new String[0];
                }
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

    public void reloadTeamsAndPlayers() {
        offlinePlayers.clear();
        offlinePlayerLookup.clear();

        players.forEach(PlayerInterface::commit);
        players.clear();
        playerLookup.clear();

        defaultTeam.players.clear();

        this.fetchTeams();

        HashMap<UUID, String> users = SQLQueryUtil.queryAllPlayers(MCGMain.SEASON);
        for(Map.Entry<UUID, String> user : users.entrySet()) {
            PlayerInterface playerInterface = createNewPlayerInterface(user.getKey(), user.getValue());
            offlinePlayers.add(playerInterface);
            offlinePlayerLookup.put(user.getKey(), playerInterface);
            if(playerInterface.myTeam == defaultTeam) {
                defaultTeam.addPlayer(playerInterface.uuid);
            }
        }

        for(Player onlinePlayer : Bukkit.getServer().getOnlinePlayers()) {
            playerJoin(onlinePlayer);
        }
    }

    public ArrayList<MCGTeam> getTeamsOrderedByScore() {
        ArrayList<MCGTeam> res = new ArrayList<>(teams.values());
        res.removeIf(team -> team == defaultTeam);
        res.sort((a, b) -> b.getScore() - a.getScore());
        return res;
    }

    public ArrayList<MCGTeam> getTeamsOrderedByScore(String minigame) {
        ArrayList<MCGTeam> res = new ArrayList<>(teams.values());
        res.removeIf(team -> team == defaultTeam);
        res.sort((a, b) -> b.getScore(minigame) - a.getScore(minigame));
        return res;
    }

    public ArrayList<MCGTeam> getRealTeams() {
        ArrayList<MCGTeam> list = new ArrayList<>(this.teams.values());
        list.removeIf(team -> team == defaultTeam);
        System.out.println(list);
        return list;
    }

    public MCGTeam getTeamFromPlayerUUID(UUID uuid) {
        return teams.get(getTeamIDFromPlayerUUID(uuid));
    }

    public int getTeamIDFromPlayerUUID(UUID uuid) {
        Integer teamId = teamLookup.get(uuid);
        if(teamId == null) {
            return 0;
        }
        return teamId;
    }

    public MCGTeam getTeamFromTeamID(int id) {
        return teams.get(id);
    }

    /* MESSAGE AND TITLE LOGIC */

    public void countdownTimer(Timer timer, int startSeconds, String startingText, String startingSubtext, String progressText, String finishedText) {
        if (timer.get() < (startSeconds + 1) * 20) {
            int secondsLeft = (int) (timer.get() / 20 + 0.5);
            if (secondsLeft == startSeconds)
                for (PlayerInterface player : players) {
                    player.bukkitPlayer.sendTitle(startingText, startingSubtext, 0, 20, 0);
                }
            else if (secondsLeft == 0)
                for (PlayerInterface player : players) {
                    player.bukkitPlayer.sendTitle(finishedText, "", 0, 20, 20);
                }
            else
                for (PlayerInterface player : players) {
                    player.bukkitPlayer.sendTitle(ChatColor.RESET + "" + secondsLeft, progressText, 0, 20, 0);
                }
        }
    }

    public void sendTitleAll(String title, String subtitle) {
        for (PlayerInterface player : players) {
            player.bukkitPlayer.sendTitle(title, subtitle, 10, 60, 20);
        }
    }

    public void sendTitleAll(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for (PlayerInterface player : players) {
            player.bukkitPlayer.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    public void sendMessageAll(String message) {
        for (PlayerInterface player : players) {
            player.bukkitPlayer.sendMessage(message);
        }
    }

    public void sendMultipleMessageAll(String[] message, int[] delays) {
        for (int i = 0; i < 5; i++)
            sendMessageAll("");

        int delay = 0;
        for (int i = 0; i < message.length; i++) {
            String str = message[i];
            delay += delays[i];
            new BukkitRunnable() {
                public void run() {
                    sendMessageAll(str);
                }
            }.runTaskLater(this.plugin, delay);
        }
    }

    public void sendActionBarAll(String message) {
        for (PlayerInterface player : players) {
            player.bukkitPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
        }
    }

}
