package ca.zharry.MinecraftGamesServer.Utils;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class GameModeManager {
	private final HashMap<Player, GameMode> pendingGameModes = new HashMap<>();

	public void setGameMode(Player player, GameMode gameMode) {
		pendingGameModes.put(player, gameMode);
	}

	public GameMode getGameMode(Player player) {
		return pendingGameModes.getOrDefault(player, player.getGameMode());
	}

	public void tick() {
		for(Map.Entry<Player, GameMode> entry : pendingGameModes.entrySet()) {
			entry.getKey().setGameMode(entry.getValue());
		}
		pendingGameModes.clear();
	}
}
