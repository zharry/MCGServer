package ca.zharry.MinecraftGamesServer.Commands;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Servers.ServerInterface;
import com.comphenix.protocol.wrappers.EnumWrappers;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandTeams implements TabExecutor {
	public ServerInterface<? extends PlayerInterface> server;

	public CommandTeams(ServerInterface<? extends PlayerInterface> server) {
		this.server = server;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(args.length < 1) {
			return false;
		}
		String[] partialArgs = Arrays.copyOfRange(args, 1, args.length);
		try {
			boolean result = false;
			boolean reload = false;
			switch (args[0].toLowerCase()) {
				case "add":
					result = add(sender, partialArgs);
					reload = true;
					break;
				case "allowjoin":
					result = allowJoin(sender, partialArgs);
					reload = false;
					break;
				case "join":
					result = join(sender, partialArgs);
					reload = true;
					break;
				case "remove":
					result = remove(sender, partialArgs);
					reload = true;
					break;
				case "rename":
					result = rename(sender, partialArgs);
					reload = true;
					break;
				case "color":
					result = color(sender, partialArgs);
					reload = true;
					break;
				case "reload":
					result = true;
					reload = true;
					break;
				case "query":
					result = queryTeams(sender);
					reload = false;
					break;
			}
			if(result && reload) {
				server.reloadTeamsAndPlayers();
			}
			return result;
		} catch(CommandException e) {
			return true;
		} catch(SQLException e) {
			sender.sendMessage(ChatColor.RED + "SQL operation failed: " + e.toString());
			e.printStackTrace();
			return true;
		}
	}

	public boolean add(CommandSender sender, String[] args) throws SQLException {
		if(args.length < 2) {
			return false;
		}
		String clr = getColor(args[0], sender).name();
		String name = getDisplayName(args, 1);
		MCGMain.sqlManager.executeQuery(
				"INSERT INTO `teams` (`season`, `teamname`, `color`) VALUES (?, ?, ?)",
				MCGMain.SEASON, name, clr);
		return true;
	}

	public boolean allowJoin(CommandSender sender, String[] args) throws SQLException {
		if(args.length < 1) {
			return false;
		}
		boolean allow = getBoolean(args[0], sender);
		if(allow != MCGMain.allowUserJoinTeam) {
			MCGMain.allowUserJoinTeam = allow;
			if(allow) {
				Bukkit.broadcast(ChatColor.GRAY + "[MCG] Enabled user joining teams", Server.BROADCAST_CHANNEL_ADMINISTRATIVE);
			} else {
				Bukkit.broadcast(ChatColor.GRAY + "[MCG] Disabled user joining teams", Server.BROADCAST_CHANNEL_ADMINISTRATIVE);
			}
		}
		return true;
	}

	public boolean join(CommandSender sender, String[] args) throws SQLException {
		if(args.length < 2) {
			return false;
		}

		List<PlayerInterface> players = getPlayer(args[0], sender);
		int teamId = getTeamId(args[1], sender, true);

		for(PlayerInterface player : players) {
			MCGMain.sqlManager.executeQuery("UPDATE `players` SET `teamid` = ? WHERE `season` = ? AND `uuid` = ?", teamId == 0 ? null : teamId, MCGMain.SEASON, player.uuid.toString());
		}
		if (teamId != 0) {
			MCGTeam team = server.getTeamFromTeamID(teamId);
			sender.sendMessage("Added " + players.size() + " players to team " + team);
		} else {
			sender.sendMessage("Removed " + players.size() + " players from any teams");
		}
		return true;
	}

	public boolean remove(CommandSender sender, String[] args) throws SQLException {
		if(args.length < 1) {
			return false;
		}
		int teamId = getTeamId(args[0], sender, false);
		MCGMain.sqlManager.executeQuery("DELETE FROM `teams` WHERE `season` = ? AND `id` = ?", MCGMain.SEASON, teamId);
		return true;
	}

	public boolean rename(CommandSender sender, String[] args) throws SQLException {
		if(args.length < 2) {
			return false;
		}
		int teamId = getTeamId(args[0], sender, false);
		String name = getDisplayName(args, 1);
		MCGMain.sqlManager.executeQuery("UPDATE `teams` SET `teamname` = ? WHERE `season` = ? AND `id` = ?", name, MCGMain.SEASON, teamId);
		return true;
	}

	public boolean color(CommandSender sender, String[] args) throws SQLException {
		if(args.length < 2) {
			return false;
		}
		int teamId = getTeamId(args[0], sender, false);
		String color = getColor(args[1], sender).name();
		MCGMain.sqlManager.executeQuery("UPDATE `teams` SET `color` = ? WHERE `season` = ? AND `id` = ?", color, MCGMain.SEASON, teamId);
		return true;
	}

	public boolean queryTeams(CommandSender sender) throws SQLException {
		StringBuilder builder = new StringBuilder();
		List<MCGTeam> teams = server.getAllTeams();
		builder.append("===== ").append(teams.size()).append(" teams").append(" =====\n");
		for(MCGTeam team : teams) {
			builder.append(team).append(" (id=").append(team.id).append("): ");
			for(UUID playerUUID : team.players) {
				PlayerInterface player = server.getPlayerFromUUID(playerUUID);
				builder.append(player.getPlayerNameForTabMenu()).append(" ");
			}
			builder.append("\n");
		}
		sender.sendMessage(builder.toString());
		return true;
	}

	private void error(CommandSender sender, String message) {
		if(sender != null) {
			sender.sendMessage(ChatColor.RED + message);
		}
		throw new CommandException();
	}

	private String getDisplayName(String[] args, int ind) {
		return String.join(" ", Arrays.copyOfRange(args, ind, args.length));
	}

	// Color ====================
	private List<String> completeColor(String currArg) {
		return Arrays.stream(Arrays.copyOf(ChatColor.values(), 16))
				.map(e -> e.name().toLowerCase())
				.filter(c -> c.startsWith(currArg))
				.collect(Collectors.toList());
	}

	private ChatColor getColor(String s, CommandSender sender) {
		ChatColor[] colors = ChatColor.values();
		for(int i = 0; i < 16; ++i) {
			if(colors[i].name().equalsIgnoreCase(s)) {
				return colors[i];
			}
		}
		error(sender, "Not a valid color: " + s);
		return null;
	}

	// Team ====================
	private List<String> completeTeam(String currArg, boolean allTeams) {
		return (allTeams ? server.getAllTeams() : server.getRealTeams()).stream()
				.map(t -> t.teamname.toLowerCase().replaceAll("[^0-9a-z ]", "").replaceAll(" +", "_"))
				.filter(t -> t.startsWith(currArg))
				.collect(Collectors.toList());
	}

	private int getTeamId(String s, CommandSender sender, boolean allTeams) {
		int teamId = -1;
		try {
			teamId = Integer.parseInt(s);
		} catch(NumberFormatException e) {
			ArrayList<MCGTeam> teams = server.getAllTeams();
			for(MCGTeam team : teams) {
				if(team.teamname.toLowerCase().replaceAll("[^0-9a-z ]", "").replaceAll(" +", "_").equals(s.toLowerCase())) {
					teamId = team.id;
					break;
				}
			}
		}
		if(teamId == -1 || (!allTeams && teamId == 0)) {
			error(sender, "Not a valid team: " + s);
		}
		return teamId;
	}

	// Boolean ====================
	private List<String> completeBoolean(String currArg) {
		return Stream.of("true", "false")
				.filter(t -> t.startsWith(currArg))
				.collect(Collectors.toList());
	}

	private boolean getBoolean(String s, CommandSender sender) {
		if(s.equalsIgnoreCase("true")) {
			return true;
		} else if(s.equalsIgnoreCase("false")) {
			return false;
		}
		error(sender, "Not a valid boolean: " + s);
		return false;
	}

	// Players ====================
	private List<String> completePlayer(String currArg) {
		return Stream.concat(Stream.concat(server.players.stream(), server.offlinePlayers.stream())
				.map(p -> p.name), Stream.of("@a", "@p", "@s", "@r"))
				.filter(p -> p.toLowerCase().startsWith(currArg))
				.collect(Collectors.toList());
	}

	private List<PlayerInterface> getPlayer(String s, CommandSender sender) {
		for(PlayerInterface p : server.offlinePlayers) {
			if(p.name.equalsIgnoreCase(s)) {
				return List.of(p);
			}
		}
		List<PlayerInterface> players = Bukkit.selectEntities(sender, s).stream()
				.filter(e -> e instanceof Player)
				.map(e -> server.playerLookup.get(e.getUniqueId()))
				.collect(Collectors.toList());
		if(players.size() == 0) {
			error(sender, "Player not found: " + s);
		}
		return players;
	}

	// Constants ====================
	private List<String> completeConstant(String currArg, String... constants) {
		return Stream.of(constants)
				.filter(v -> v.toLowerCase().startsWith(currArg))
				.collect(Collectors.toList());
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		String currArg = (args.length > 0 ? args[args.length - 1] : "").toLowerCase();
		if(args.length <= 1) {
			return Stream.of("add", "allowjoin", "join", "reload", "remove", "rename", "color", "query")
					.filter(p -> p.startsWith(currArg))
					.collect(Collectors.toList());
		} else if(args.length == 2) {
			switch(args[0]) {
				case "add":
					return completeColor(currArg);
				case "allowjoin":
					return completeBoolean(currArg);
				case "join":
					return completePlayer(currArg);
				case "remove":
				case "rename":
				case "color":
					return completeTeam(currArg, false);
			}
		} else if(args.length == 3) {
			switch(args[0]) {
				case "join":
					return completeTeam(currArg, true);
				case "color":
					return completeColor(currArg);
				case "rename":
					try {
						return List.of(server.getTeamFromTeamID(getTeamId(args[1], null, false)).teamname);
					} catch(Exception e) {
					}
					break;
			}
		}
		return new ArrayList<>();
	}
}
