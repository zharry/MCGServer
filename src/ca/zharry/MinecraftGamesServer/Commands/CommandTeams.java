package ca.zharry.MinecraftGamesServer.Commands;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Servers.ServerInterface;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class CommandTeams extends CommandBase {
	public CommandTeams(ServerInterface<? extends PlayerInterface> server) {
		super(server);
	}

	@Subcommand(argsRequired = 2, params = {"color", "name"}, desc = "Add a new team", reloadTeams = true)
	public void add(CommandSender sender, @ArgChatColor ChatColor color, @ArgVaarg String[] nameArgs) throws SQLException {
		MCGMain.sqlManager.executeQuery(
				"INSERT INTO `teams` (`season`, `teamname`, `color`) VALUES (?, ?, ?)",
				MCGMain.SEASON, String.join(" ", nameArgs), color.name());
	}

	@Subcommand(argsRequired = 1, params = {"allow"}, desc = "Allow self enrolment")
	public void allowJoin(CommandSender sender, @ArgBoolean boolean allow) {
		if(allow != MCGMain.allowUserJoinTeam) {
			MCGMain.allowUserJoinTeam = allow;
			if(allow) {
				MCGMain.broadcastInfo("Enabled user joining teams");
			} else {
				MCGMain.broadcastInfo("Disabled user joining teams");
			}
		}
	}

	@Subcommand(argsRequired = 2, params = {"players", "team"}, desc = "Add players to a team", reloadTeams = true)
	public void join(CommandSender sender, @ArgPlayers List<PlayerInterface> players, @ArgTeamID(allowDefaultTeam = true, allowNumericID = true) int teamId) throws SQLException {
		for(PlayerInterface player : players) {
			MCGMain.sqlManager.executeQuery("UPDATE `players` SET `teamid` = ? WHERE `season` = ? AND `uuid` = ?", teamId == 0 ? null : teamId, MCGMain.SEASON, player.uuid.toString());
		}
		if (teamId != 0) {
			MCGTeam team = server.getTeamFromTeamID(teamId);
			sender.sendMessage("Added " + players.size() + " players to team " + team);
		} else {
			sender.sendMessage("Removed " + players.size() + " players from any teams");
		}
	}

	@Subcommand(argsRequired = 1, params = {"team"}, desc = "Remove a team", reloadTeams = true)
	public void remove(CommandSender sender, @ArgTeamID(allowNumericID = true) int teamId) throws SQLException {
		MCGMain.sqlManager.executeQuery("DELETE FROM `teams` WHERE `season` = ? AND `id` = ?", MCGMain.SEASON, teamId);
	}

	@Subcommand(argsRequired = 2, params = {"team", "name"}, desc = "Rename a team", reloadTeams = true)
	public void rename(CommandSender sender, @ArgTeamID(allowNumericID = true) int teamId, @ArgVaarg String[] nameArgs) throws SQLException {
		MCGMain.sqlManager.executeQuery("UPDATE `teams` SET `teamname` = ? WHERE `season` = ? AND `id` = ?", String.join(" ", nameArgs), MCGMain.SEASON, teamId);
	}

	@Subcommand(argsRequired = 2, params = {"team", "color"}, desc = "Change a team's color", reloadTeams = true)
	public void color(CommandSender sender, @ArgTeamID(allowNumericID = true) int teamId, @ArgChatColor ChatColor color) throws SQLException {
		MCGMain.sqlManager.executeQuery("UPDATE `teams` SET `color` = ? WHERE `season` = ? AND `id` = ?", color.name(), MCGMain.SEASON, teamId);
	}

	@Subcommand(desc = "Reload teams", reloadTeams = true)
	public void reload(CommandSender sender) {
	}

	@Subcommand(desc = "Print loaded teams")
	public void query(CommandSender sender) {
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
	}
}
