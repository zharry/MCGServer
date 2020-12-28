package ca.zharry.MinecraftGamesServer.Servers;

import ca.zharry.MinecraftGamesServer.Listeners.ChangeGameRule;
import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Timer.Timer;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public abstract class ServerInterface {

    public HashMap<String, String> minigames = new HashMap<String, String>();

    public JavaPlugin plugin;
    public BukkitTask taskScoreboard;
    public ArrayList<PlayerInterface> players;
    public HashMap<UUID, PlayerInterface> playerLookup;

    public World world;
    public Location serverSpawn;

    public ArrayList<Integer> teamIDs;
    public HashMap<Integer, MCGTeam> teams;
    public HashMap<UUID, Integer> teamLookup;

    public ServerInterface(JavaPlugin plugin) {
        this.plugin = plugin;
        this.players = new ArrayList<>();
        this.playerLookup = new HashMap<UUID, PlayerInterface>();

        world = plugin.getServer().getWorld("world");

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

        applyGameRules(world);
        plugin.getServer().getPluginManager().registerEvents(new ChangeGameRule(this), plugin);
    }

    public void applyGameRules(World world) {
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

    public void sendPlayersToGame(String minigame) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(minigame);

        for (PlayerInterface player : players) {
            player.bukkitPlayer.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        }
    }

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

    public void sendTitleAll(String title, String subtitle, int fadeIn, int stay, int fadOut) {
        for (PlayerInterface player : players) {
            player.bukkitPlayer.sendTitle(title, subtitle, fadeIn, stay, fadOut);
        }
    }

    public void sendMessageAll(String message) {
        for (PlayerInterface player : players) {
            player.bukkitPlayer.sendMessage(message);
        }
    }

    public void sendActionBarAll(String message) {
        for (PlayerInterface player : players) {
            player.bukkitPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
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

}
