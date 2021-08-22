package ca.zharry.MinecraftGamesServer.Servers;

import ca.zharry.MinecraftGamesServer.Listeners.ChangeGameRule;
import ca.zharry.MinecraftGamesServer.Listeners.ListenerJoinQuit;
import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Timer.Cutscene;
import ca.zharry.MinecraftGamesServer.Timer.Timer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Team;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class ServerInterface<P extends PlayerInterface> {

    public JavaPlugin plugin;
    public String minigame;
    public BukkitTask taskScoreboard;

    public World world;
    public Location serverSpawn;

    public ArrayList<P> players = new ArrayList<>();
    public HashMap<UUID, P> playerLookup = new HashMap<>();
    public ArrayList<P> offlinePlayers = new ArrayList<>();
    public HashMap<UUID, P> offlinePlayerLookup = new HashMap<>();

    public ArrayList<Integer> teamIDs = new ArrayList<>();
    private HashMap<Integer, MCGTeam> teams = new HashMap<>(); // Team ID to MCGTeam Mapping
    private HashMap<UUID, Integer> teamLookup = new HashMap<>(); // Player UUID to Team ID Mapping
    public MCGTeam defaultTeam;
    public Cutscene currentCutscene;

    public ServerInterface(JavaPlugin plugin, World world, String minigame) {
        this.plugin = plugin;
        this.world = world;
        this.minigame = minigame;

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
        for (P player : players) {
            player.commit();
        }

        if(currentCutscene != null) {
            currentCutscene.cancel();
        }
    }

    public abstract void registerCommands();

    public abstract void registerListeners();

    public void applyGameRules(World world) {
    }

    public abstract P createNewPlayerInterface(UUID uuid, String name);

    /* PLAYER LOGIC */

    public P getPlayerFromUUID(UUID uuid) {
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
        P playerInterface = offlinePlayerLookup.get(uuid);
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
        P playerInterface = playerLookup.get(uuid);
        playerInterface.playerQuit(player);
        offlinePlayers.add(playerInterface);
        offlinePlayerLookup.put(uuid, playerInterface);
        players.remove(playerInterface);
        playerLookup.remove(uuid);
    }

    /* TEAM LOGIC */

    public void fetchTeams() {
        this.teams.clear();
        this.teamLookup.clear();
        this.teamIDs.clear();

        teams.put(defaultTeam.id, defaultTeam);

        try {
            ResultSet resultSet = MCGMain.sqlManager.executeQuery("SELECT * FROM `teams` WHERE `season` = ?", MCGMain.SEASON);
            while(resultSet.next()) {
                int id = resultSet.getInt("id");
                this.teamIDs.add(id);
                this.teams.put(id, new MCGTeam(id, resultSet.getString("teamname").trim(), resultSet.getString("color").trim(), this));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fetchPlayers() {
        try {
            ResultSet resultSet = MCGMain.sqlManager.executeQuery("SELECT * FROM `players` WHERE `season` = ?", MCGMain.SEASON);
            while (resultSet.next()) {
                int teamId = resultSet.getInt("teamid");
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                String username = resultSet.getString("username");
                teamLookup.put(uuid, teamId);
                teams.get(teamId).addPlayer(uuid);

                P playerInterface = createNewPlayerInterface(uuid, username);
                offlinePlayers.add(playerInterface);
                offlinePlayerLookup.put(uuid, playerInterface);
                if (playerInterface.myTeam == defaultTeam) {
                    defaultTeam.addPlayer(playerInterface.uuid);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void commitAllPlayers() {
        players.forEach(PlayerInterface::commit);
    }

    public void unloadPlayers() {
        offlinePlayers.clear();
        offlinePlayerLookup.clear();

        players.forEach(PlayerInterface::commit);
        players.clear();
        playerLookup.clear();

        defaultTeam.players.clear();
    }

    public void reloadTeamsAndPlayers() {
        try {
            unloadPlayers();

            this.fetchTeams();
            this.fetchPlayers();

            for (Player onlinePlayer : Bukkit.getServer().getOnlinePlayers()) {
                playerJoin(onlinePlayer);
            }

            for (int id : teamIDs) {
                MCGTeam team = teams.get(id);
                MCGMain.logger.info("Team '" + team + "': " + team.players.stream().map(UUID::toString).collect(Collectors.joining(", ")));
            }

            MCGMain.broadcastInfo("Teams and scores reload successful");
        } catch(Exception e) {
            MCGMain.broadcastError("Teams reload failed: " + e);
            e.printStackTrace();
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

    public ArrayList<MCGTeam> getAllTeams() {
        return new ArrayList<>(this.teams.values());
    }

    public ArrayList<MCGTeam> getRealTeams() {
        ArrayList<MCGTeam> list = new ArrayList<>(this.teams.values());
        list.removeIf(team -> team == defaultTeam);
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

    public void configureScoreboardTeam(Team minecraftTeam, MCGTeam team) {
        minecraftTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        minecraftTeam.setCanSeeFriendlyInvisibles(false);
        if(team != defaultTeam) {
            minecraftTeam.setAllowFriendlyFire(false);
        }
    }
}
