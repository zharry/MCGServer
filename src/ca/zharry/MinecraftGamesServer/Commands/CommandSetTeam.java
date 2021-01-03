package ca.zharry.MinecraftGamesServer.Commands;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Servers.ServerInterface;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class CommandSetTeam implements CommandExecutor {

    public ServerInterface server;

    public CommandSetTeam(ServerInterface server) {
        this.server = server;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp())
            return false;

        String username = args[0];
        int teamId = Integer.parseInt(args[1]);

        ArrayList<PlayerInterface> allPlayers = new ArrayList<>();
        allPlayers.addAll(server.players);
        allPlayers.addAll(server.offlinePlayers);

        PlayerInterface player = null;

        for (PlayerInterface p : allPlayers) {
            if (p.name.equals(username)) {
                player = p;
                break;
            }
        }
        if (player == null) {
            sender.sendMessage("Player " + username + " does not exist");
            return false;
        }

        try {
            Statement statement = MCGMain.conn.connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM `teams` WHERE `players` LIKE '%" + player.uuid + "%';");
            if(resultSet.next()) {
                int queryId = resultSet.getInt("id");
                String players = resultSet.getString("players");
                String[] splitted = players.split(",");
                if(players.trim().length() == 0) {
                    splitted = new String[0];
                }

                ArrayList<String> playerRemoved = new ArrayList<>();

                for(String s : splitted) {
                    if(!s.equals(player.uuid.toString())) {
                        playerRemoved.add(s);
                    }
                }

                String joined = String.join(",", playerRemoved);
                statement = MCGMain.conn.connection.createStatement();
                statement.execute("UPDATE `teams` SET `players` = '" + joined + "' WHERE `id` = " + queryId + ";");
            }

            if(teamId != 0) {

                // Check if the team they are being added to is valid
                int queryTeamId = -1;
                String queryTeamName = "";
                String queryTeamPlayers = "";
                statement = MCGMain.conn.connection.createStatement();
                resultSet = statement.executeQuery("SELECT * FROM `teams`;");
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
                String newPlayerList = queryTeamPlayers + "," + player.uuid;
                if (queryTeamPlayers.equals(""))
                    newPlayerList = "" + player.uuid;

                // Add them to the team
                statement.execute("UPDATE `teams` SET `players` = '" + newPlayerList + "' " +
                        "WHERE `id` = " + teamId + ";");
                sender.sendMessage("Added " + player.name + " to team " + queryTeamName);
            } else {
                sender.sendMessage("Removed " + player.name + " from any teams");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(ChatColor.RED + "SQL operation failed: " + e.toString());
        }

        try {
            server.reloadTeamsAndPlayers();

            sender.sendMessage("Team and players reloaded");
        } catch(Exception e) {
            e.printStackTrace();
            sender.sendMessage(ChatColor.RED + "Reload failed: " + e.toString());
        }

        return true;
    }
}
