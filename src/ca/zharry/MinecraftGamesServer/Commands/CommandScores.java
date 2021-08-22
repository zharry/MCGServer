package ca.zharry.MinecraftGamesServer.Commands;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Servers.ServerInterface;
import org.bukkit.command.*;

import java.sql.SQLException;
import java.util.List;

public class CommandScores extends CommandBase {
	public CommandScores(ServerInterface<? extends PlayerInterface> server) {
		super(server);
	}

	@Subcommand(argsRequired = 2, params = {"players", "minigame"}, desc = "Delete player score", reloadTeams = true)
	public void delete(CommandSender sender, @ArgPlayers List<PlayerInterface> players, @ArgMinigame(wildcard = true) String game) throws SQLException {
		server.unloadPlayers();
		for(PlayerInterface player : players) {
			MCGMain.sqlManager.executeQuery("DELETE FROM `scores` WHERE `season` = ? AND `uuid` = ? AND (? = '*' OR `minigame` = ?)", MCGMain.SEASON, player.uuid.toString(), game, game);
			MCGMain.sqlManager.executeQueryAsync("INSERT INTO `logs`(`season`, `minigame`, `playeruuid`, `scoredelta`, `message`) VALUES (?, ?, ?, NULL, ?)", MCGMain.SEASON, game, player.uuid.toString(), "scores delete");
		}
	}

	@Subcommand(argsRequired = 3, params = {"players", "minigame", "amount"}, desc = "Add score for player", reloadTeams = true)
	public void add(CommandSender sender, @ArgPlayers List<PlayerInterface> players, @ArgMinigame(wildcard = true) String game, @ArgInt int amount) throws SQLException {
		server.unloadPlayers();
		String[] minigames;
		if(game.equals("*")) {
			minigames = MCGMain.getMinigames().toArray(new String[0]);
		} else {
			minigames = new String[] {game};
		}
		for(String minigame : minigames) {
			for (PlayerInterface player : players) {
				MCGMain.sqlManager.executeQuery(
						"INSERT INTO `scores` (`uuid`, `season`, `minigame`, `score`) " +
								"VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
								"`score` = `score` + VALUES(`score`), " +
								"`time` = current_timestamp()", player.uuid.toString(), MCGMain.SEASON, minigame, amount);
				MCGMain.sqlManager.executeQueryAsync("INSERT INTO `logs`(`season`, `minigame`, `playeruuid`, `scoredelta`, `message`) VALUES (?, ?, ?, ?, ?)", MCGMain.SEASON, game, player.uuid.toString(), amount, "scores add " + amount);
			}
		}
	}

	@Subcommand(argsRequired = 3, params = {"players", "minigame", "score"}, desc = "Set score for player", reloadTeams = true)
	public void set(CommandSender sender, @ArgPlayers List<PlayerInterface> players, @ArgMinigame(wildcard = true) String game, @ArgInt int amount) throws SQLException {
		server.unloadPlayers();
		String[] minigames;
		if(game.equals("*")) {
			minigames = MCGMain.getMinigames().toArray(new String[0]);
		} else {
			minigames = new String[] {game};
		}
		for(String minigame : minigames) {
			for (PlayerInterface player : players) {
				MCGMain.sqlManager.executeQuery(
						"INSERT INTO `scores` (`uuid`, `season`, `minigame`, `score`) " +
								"VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
								"`score` = VALUES(`score`), " +
								"`time` = current_timestamp()", player.uuid.toString(), MCGMain.SEASON, minigame, amount);
				MCGMain.sqlManager.executeQueryAsync("INSERT INTO `logs`(`season`, `minigame`, `playeruuid`, `scoredelta`, `message`) VALUES (?, ?, ?, ?, ?)", MCGMain.SEASON, game, player.uuid.toString(), amount, "scores set " + amount);
			}
		}
	}
}
