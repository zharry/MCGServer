package ca.zharry.MinecraftGamesServer;

import ca.zharry.MinecraftGamesServer.MysqlConnection.MysqlConnection;
import ca.zharry.MinecraftGamesServer.Servers.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.ResultSet;
import java.sql.SQLException;
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
        logger.info("MCG Plugin Enabled! Test");
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        conn = new MysqlConnection("mysql", "3306", "mcg", "root", "password");
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

        this.getCommand("setteam").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                if (!sender.isOp())
                    return false;

                Player player = null;
                String username = args[0];
                int teamId = Integer.parseInt(args[1]);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().equals(username)) {
                        player = p;
                        break;
                    }
                }
                if (player == null) {
                    sender.sendMessage("Player " + username + " does not exist");
                    return false;
                }

                try {
                    // Check if the team they are being added to is valid
                    int queryTeamId = -1;
                    String queryTeamName = "";
                    String queryTeamPlayers = "";
                    Statement statement = MCGMain.conn.connection.createStatement();
                    ResultSet resultSet = statement.executeQuery("SELECT * FROM `teams`;");
                    while (resultSet.next()) {
                        queryTeamId = resultSet.getInt("id");
                        if (queryTeamId == teamId) {
                            queryTeamName = resultSet.getString("teamname");
                            queryTeamPlayers = resultSet.getString("players");
                            break;
                        } else {
                            queryTeamId = -1;
                        }
                    }
                    if (queryTeamId == -1) {
                        sender.sendMessage("Team with id " + teamId + " does not exist");
                        return false;
                    }

                    // Construct new player list
                    String newPlayerList = queryTeamPlayers + "," + player.getUniqueId();
                    if (queryTeamPlayers.equals(""))
                        newPlayerList = "" + player.getUniqueId();

                    // Add them to the team
                    statement.execute("UPDATE `teams` SET `players` = '" + newPlayerList + "' " +
                            "WHERE `id` = " + teamId + ";");
                    sender.sendMessage("Added " + player.getName() + " to team " + queryTeamName);
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                return true;
            }
        });

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
