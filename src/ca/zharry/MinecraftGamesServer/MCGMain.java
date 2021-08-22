package ca.zharry.MinecraftGamesServer;

import ca.zharry.MinecraftGamesServer.Commands.*;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.SQL.SQLManager;
import ca.zharry.MinecraftGamesServer.Servers.*;
import ca.zharry.MinecraftGamesServer.Utils.*;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MCGMain extends JavaPlugin {
    public static final Logger logger = Logger.getLogger("Minecraft");
    public static File resourcePackRoot = new File("/home/mcg/ResourcePack");

    // Managers
    public static ProtocolManager protocolManager;
    public static SQLManager sqlManager;
    public static GameModeManager gameModeManager;
    public static BungeeManager bungeeManager;
    public static ResourcePackManager resourcePackManager;
    public static MusicManager musicManager;

    // Allow user self enroll in teams
    public static boolean allowUserJoinTeam;

    // Global configuration
    public static final int SEASON = 2;
    public static final int PLAYER_TARGET = 32;

    // Current server information
    public String serverMinigame;
    public ServerInterface<? extends PlayerInterface> server;

    // Minigame information
    public static String lobbyId = "lobby";
    public static HashMap<String, String> serverNames = new HashMap<>();
    public static HashMap<String, TriFunction<JavaPlugin, World, String, ServerInterface<? extends PlayerInterface>>> serverConstructors = new HashMap<>();
    public static void addServer(String id, TriFunction<JavaPlugin, World, String, ServerInterface<? extends PlayerInterface>> constructor, String name) {
        serverNames.put(id, name);
        serverConstructors.put(id, constructor);
    }

    public static List<String> getMinigames() {
        return serverNames.keySet().stream().filter(n -> !n.equals(lobbyId)).collect(Collectors.toList());
    }

    static {
        addServer("lobby", ServerLobby::new, "Lobby");
        addServer("parkour", ServerParkour::new, "Parkour");
        addServer("spleef", ServerSpleef::new, "Spleef");
        addServer("dodgeball", ServerDodgeball::new, "Dodgeball");
        addServer("survivalgames", ServerSurvivalGames::new, "Survival Games");
        addServer("elytrarun", ServerElytraRun::new, "Elytra Run");
    }

    public static void broadcastInfo(String msg) {
        Bukkit.broadcast(ChatColor.GRAY + "[MCG] " + msg, Server.BROADCAST_CHANNEL_ADMINISTRATIVE);
    }

    public static void broadcastError(String msg) {
        Bukkit.broadcast(ChatColor.RED + "[MCG] " + msg, Server.BROADCAST_CHANNEL_ADMINISTRATIVE);
    }

    @Override
    public void onEnable() {
        protocolManager = ProtocolLibrary.getProtocolManager();
        gameModeManager = new GameModeManager();
        resourcePackManager = new ResourcePackManager(this);
        musicManager = new MusicManager(this);

        try {
            Properties sqlProperties = new Properties();
            sqlProperties.setProperty("user", "root");
            sqlProperties.setProperty("password", "password");

            sqlManager = new SQLManager("jdbc:mysql://mysql:3306/mcg", sqlProperties, 5);
            this.setupDatabase();

            // Instantiate the correct ServerInterface
            this.getConfigurationFile();
            if(!serverConstructors.containsKey(serverMinigame)) {
                throw new RuntimeException("Server " + serverMinigame + " does not exist");
            }
            server = serverConstructors.get(serverMinigame).apply(this, getServer().getWorld("world"), serverMinigame);
            bungeeManager = new BungeeManager(this, server, resourcePackManager);

            resourcePackManager.initialize();

            this.getCommand("cutscene").setExecutor(new CommandCutscene(server));
            this.getCommand("join").setExecutor(new CommandJoinTeam(server));
            this.getCommand("teams").setExecutor(new CommandTeams(server));
            this.getCommand("scores").setExecutor(new CommandScores(server));

            // Tick some of the managers
            new BukkitRunnable() {
                public void run() {
                    sqlManager.tick();
                    gameModeManager.tick();
                    musicManager.tick();
                }
            }.runTaskTimer(this, 0, 0);

            server.onEnableCall();

            logger.info("MCG Plugin Enabled!");
        } catch(Exception e) {
            broadcastError("onEnable failed: " + e.toString());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (server != null) {
                server.onDisableCall();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        try {
            musicManager.stopMusicAll();
        } catch(Exception e) {
            e.printStackTrace();
        }

        try {
            sqlManager.stop();
        } catch(Exception e) {
            e.printStackTrace();
        }

        logger.info("MCG Plugin Disabled!");
    }

    public void getConfigurationFile() {
        saveDefaultConfig();
        serverMinigame = getConfig().getString("for");
        logger.info("Current server running for: " + serverMinigame);
    }

    public void setupDatabase() throws SQLException, IOException {
        sqlManager.executeQuery(new String(getClass().getResourceAsStream("/setup.sql").readAllBytes()));
    }
}
