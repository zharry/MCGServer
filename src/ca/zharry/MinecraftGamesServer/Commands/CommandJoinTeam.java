package ca.zharry.MinecraftGamesServer.Commands;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Servers.ServerInterface;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class CommandJoinTeam implements TabExecutor {

	public ServerInterface server;

	public CommandJoinTeam(ServerInterface server) {
		this.server = server;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!MCGMain.allowUserJoinTeam) {
			sender.sendMessage(ChatColor.RED + "You cannot join teams at this time");
			return true;
		}

		if(args.length < 1) {
			return false;
		}

		if(!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "Sender must be a player");
			return true;
		}

		UUID playerUUID = ((Player) sender).getUniqueId();

		int teamId = -1;

		for(MCGTeam team : server.getRealTeams()) {
			if(team.teamname.toLowerCase().replaceAll("[^0-9a-z ]", "").replaceAll(" +", "_").equals(args[0].toLowerCase())) {
				teamId = team.id;
				break;
			}
		}
		if(teamId == -1) {
			sender.sendMessage(ChatColor.RED + "Team " + args[0] + " does not exist");
			return true;
		}

		if(teamId == server.getPlayerFromUUID(playerUUID).myTeam.id) {
			sender.sendMessage(ChatColor.RED + "You are already in " + server.getTeamFromTeamID(teamId));
			return true;
		}

		try {
			MCGMain.sqlManager.executeQuery("UPDATE `players` SET `teamid` = ? WHERE `season` = ? AND `uuid` = ?", teamId == 0 ? null : teamId, MCGMain.SEASON, playerUUID.toString());
			sender.sendMessage("Successfully joined " + server.getTeamFromTeamID(teamId));
		} catch (SQLException e) {
			sender.sendMessage("Could not join " + server.getTeamFromTeamID(teamId));
			e.printStackTrace();
			Bukkit.broadcast(ChatColor.RED + "[MCG] SQL operation failed: " + e.toString(), Server.BROADCAST_CHANNEL_ADMINISTRATIVE);
		}

		server.reloadTeamsAndPlayers();

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		String currArg = (args.length > 0 ? args[args.length - 1] : "").toLowerCase();
		if(args.length <= 1) {
			return server.getRealTeams().stream()
					.map(t -> t.teamname.toLowerCase().replaceAll("[^0-9a-z ]", "").replaceAll(" +", "_"))
					.filter(t -> t.startsWith(currArg))
					.sorted()
					.collect(Collectors.toList());
		}
		return new ArrayList<>();
	}
}
