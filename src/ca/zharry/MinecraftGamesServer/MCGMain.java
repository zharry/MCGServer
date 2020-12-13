package ca.zharry.MinecraftGamesServer;

import ca.zharry.MinecraftGamesServer.MysqlConnection.MysqlConnection;
import ca.zharry.MinecraftGamesServer.Servers.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Statement;
import java.util.logging.Logger;

public class MCGMain extends JavaPlugin {
    public static final Logger logger = Logger.getLogger("Minecraft");
    public static MysqlConnection conn;

    // Global configuration
    public static final int SEASON = 0;
    public static final int PLAYER_TARGET = 24;

    // Current server information
    public String serverMinigame;
    public ServerInterface server;

    @Override
    public void onEnable() {
        logger.info("MCG Plugin Enabled!");
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        conn = new MysqlConnection("localhost", "3306", "mcg", "root", "password");
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
        }

        server.onEnableCall();
    }

    @Override
    public void onDisable() {
        logger.info("MCG Plugin Disabled!");
        server.onDisableCall();
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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
