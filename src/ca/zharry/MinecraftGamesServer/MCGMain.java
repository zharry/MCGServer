package ca.zharry.MinecraftGamesServer;

import ca.zharry.MinecraftGamesServer.Commands.CommandReloadTeamScores;
import ca.zharry.MinecraftGamesServer.Commands.CommandSetTeam;
import ca.zharry.MinecraftGamesServer.MysqlConnection.MysqlConnection;
import ca.zharry.MinecraftGamesServer.Servers.*;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class MCGMain extends JavaPlugin {
    public static final Logger logger = Logger.getLogger("Minecraft");
    public static MysqlConnection conn;
    public static MysqlConnection asyncConn;
    public static ProtocolManager protocolManager;

    // Global configuration
    public static final int SEASON = 2;
    public static final int PLAYER_TARGET = 32;

    // Current server information
    public String serverMinigame;
    public ServerInterface server;

    public static String serverToSendAll;

    @Override
    public void onEnable() {
        protocolManager = ProtocolLibrary.getProtocolManager();

        logger.info("MCG Plugin Enabled! Test");
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", new PluginMessageListener() {
            @Override
            public void onPluginMessageReceived(String channel, Player player, byte[] message) {
                ByteArrayDataInput input = ByteStreams.newDataInput(message);
                String subchannel = input.readUTF();
                if(subchannel.equals("PlayerList")) {
                    if(serverToSendAll == null) {
                        return;
                    }
                    input.readUTF();
                    String[] players = input.readUTF().split(", ");
                    for(String otherPlayerName : players) {
                        ByteArrayDataOutput output = ByteStreams.newDataOutput();
                        output.writeUTF("ConnectOther");
                        output.writeUTF(otherPlayerName);
                        output.writeUTF(serverToSendAll);
                        player.sendPluginMessage(MCGMain.this, "BungeeCord", output.toByteArray());
                    }
                    serverToSendAll = null;
                }
            }
        });

        conn = new MysqlConnection("mysql", "3306", "mcg", "root", "password");
        asyncConn = new MysqlConnection("mysql", "3306", "mcg", "root", "password");
        this.setupDatabase();

        this.getConfigurationFile();
        switch (serverMinigame) {
            case "lobby":
                server = new ServerLobby(this);
                break;
            case "parkour":
                server = new ServerParkour(this);
                break;
            case "spleef":
                server = new ServerSpleef(this);
                break;
            case "dodgeball":
                server = new ServerDodgeball(this);
                break;
            case "survivalgames":
                server = new ServerSurvivalGames(this);
                break;
        }

        server.onEnableCall();
        this.getCommand("setteam").setExecutor(new CommandSetTeam(server));
        this.getCommand("reloadteams").setExecutor(new CommandReloadTeamScores(server));
    }

    @Override
    public void onDisable() {
        logger.info("MCG Plugin Disabled!");
        if(server != null) {
            server.onDisableCall();
        }

        try {
            conn.connection.close();
            asyncConn.connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void getConfigurationFile() {
        saveDefaultConfig();
        serverMinigame = getConfig().getString("for");
        logger.info("Current server running for: " + serverMinigame);
    }

    public void setupDatabase() {
        try {
            Statement statement = conn.connection.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS `scores` ( " +
                    "`id` INT NOT NULL AUTO_INCREMENT , " +
                    "`uuid` VARCHAR(255) NOT NULL , " +
                    "`season` INT NOT NULL , " +
                    "`minigame` VARCHAR(255) NOT NULL , " +
                    "`score` INT NOT NULL , " +
                    "`metadata` VARCHAR(255) NOT NULL DEFAULT '' , " +
                    "`time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "PRIMARY KEY (`id`)) ENGINE = InnoDB;");

            statement.execute("CREATE TABLE IF NOT EXISTS `teams` ( " +
                    "`id` INT NOT NULL AUTO_INCREMENT , " +
                    "`season` INT NOT NULL , " +
                    "`teamname` VARCHAR(255) NOT NULL , " +
                    "`players` VARCHAR(255) NOT NULL , " + // MAX TEAM SIZE IS 6
                    "`color` VARCHAR(255) NOT NULL , " +
                    "PRIMARY KEY (`id`)) ENGINE = InnoDB;");

            statement.execute("CREATE TABLE IF NOT EXISTS `usernames` ( " +
                    " `uuid` varchar(255) NOT NULL, " +
                    " `season` int(11) NOT NULL, " +
                    " `username` varchar(255) NOT NULL, " +
                    " PRIMARY KEY (`uuid`,`season`) USING BTREE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            statement.execute("CREATE TABLE IF NOT EXISTS `logs` (" +
                    " `id` int(11) NOT NULL AUTO_INCREMENT," +
                    " `season` int(11) NOT NULL," +
                    " `minigame` varchar(255) NOT NULL," +
                    " `playeruuid` varchar(255) NOT NULL," +
                    " `scoredelta` int(11) NOT NULL," +
                    " `message` varchar(255) NOT NULL," +
                    " `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "PRIMARY KEY (`id`)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
