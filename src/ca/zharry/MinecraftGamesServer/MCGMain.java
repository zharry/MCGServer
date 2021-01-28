package ca.zharry.MinecraftGamesServer;

import ca.zharry.MinecraftGamesServer.Commands.*;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.SQL.SQLManager;
import ca.zharry.MinecraftGamesServer.Servers.*;
import ca.zharry.MinecraftGamesServer.Utils.BungeeManager;
import ca.zharry.MinecraftGamesServer.Utils.GameModeManager;
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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MCGMain extends JavaPlugin {
    public static final Logger logger = Logger.getLogger("Minecraft");
    public static SQLManager sqlManager;
    public static GameModeManager gameModeManager;
    public static ProtocolManager protocolManager;
    public static BungeeManager bungeeManager;
    public static boolean allowUserJoinTeam;

    // Global configuration
    public static final int SEASON = 2;
    public static final int PLAYER_TARGET = 32;

    // Current server information
    public String serverMinigame;
    public ServerInterface server;

    // Minigame information
    public static String lobbyId = "lobby";
    public static HashMap<String, String> serverNames = new HashMap<>();
    public static HashMap<String, Class<? extends ServerInterface<? extends PlayerInterface>>> serverClasses = new HashMap<>();
    public static void addServer(String id, Class<? extends ServerInterface<? extends PlayerInterface>> serverClass, String name) {
        serverNames.put(id, name);
        serverClasses.put(id, serverClass);
    }

    public static List<String> getMinigames() {
        return serverNames.keySet().stream().filter(n -> !n.equals(lobbyId)).collect(Collectors.toList());
    }

    static {
        addServer("lobby", ServerLobby.class, "Lobby");
        addServer("parkour", ServerParkour.class, "Parkour");
        addServer("spleef", ServerSpleef.class, "Spleef");
        addServer("dodgeball", ServerDodgeball.class, "Dodgeball");
        addServer("survivalgames", ServerSurvivalGames.class, "Survival Games");
    }

    @Override
    public void onEnable() {
        protocolManager = ProtocolLibrary.getProtocolManager();
        gameModeManager = new GameModeManager();

        try {
            Properties sqlProperties = new Properties();
            sqlProperties.setProperty("user", "root");
            sqlProperties.setProperty("password", "password");

            sqlManager = new SQLManager("jdbc:mysql://mysql:3306/mcg", sqlProperties, 5);
            this.setupDatabase();

            // Start up ticking to allow executeQueryAsyncTick
            new BukkitRunnable() {
                public void run() {
                    sqlManager.tick();
                    gameModeManager.tick();
                }
            }.runTaskTimer(this, 0, 0);

            // Instantiate the correct ServerInterface
            this.getConfigurationFile();
            Class<? extends ServerInterface<? extends PlayerInterface>> serverClass = serverClasses.get(serverMinigame);
            if(serverClass == null) {
                throw new RuntimeException("Server " + serverMinigame + " does not exist");
            }
            server = serverClass.getConstructor(JavaPlugin.class, World.class, String.class).newInstance(this, getServer().getWorld("world"), serverMinigame);
            bungeeManager = new BungeeManager(this, server);

            server.onEnableCall();
            this.getCommand("cutscene").setExecutor(new CommandCutscene(server));
            this.getCommand("join").setExecutor(new CommandJoinTeam(server));
            this.getCommand("teams").setExecutor(new CommandTeams(server));

            logger.info("MCG Plugin Enabled!");
        } catch(Exception e) {
            Bukkit.broadcast("MinecraftGamesServer onEnable failed: " + ChatColor.RED + e.toString(), Server.BROADCAST_CHANNEL_ADMINISTRATIVE);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDisable() {
        logger.info("MCG Plugin Disabled!");
        if(server != null) {
            server.onDisableCall();
        }

        sqlManager.stop();
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
