package ca.zharry.MinecraftGamesServer.Utils;

import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Random;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;

public class MusicManager {

	public TreeSet<PlayerWrapper> playerEventQueue = new TreeSet<>();
	public HashMap<Player, PlayerWrapper> playerWrapperLookup = new HashMap<>();

	Function<PlayerWrapper, Music> getNextBackgroundMusic;

	public MusicManager(JavaPlugin plugin) {
		Bukkit.getOnlinePlayers().forEach(player -> playerWrapperLookup.put(player, new PlayerWrapper(player)));

		plugin.getServer().getPluginManager().registerEvents(new Listener() {
			@EventHandler
			public void onPlayerJoin(PlayerJoinEvent event) {
				Player player = event.getPlayer();
				PlayerWrapper wrapper = new PlayerWrapper(player);
				playerWrapperLookup.put(player, wrapper);
				if(getNextBackgroundMusic != null) {
					wrapper.playMusic(getNextBackgroundMusic);
				}
			}

			@EventHandler
			public void onPlayerQuit(PlayerQuitEvent event) {
				Player player = event.getPlayer();
				PlayerWrapper wrapper = playerWrapperLookup.get(player);
				playerEventQueue.remove(wrapper);
				playerWrapperLookup.remove(player);
			}

			@EventHandler
			public void onPlayerResourcePackStatus(PlayerResourcePackStatusEvent event) {
				if(event.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
					if(getNextBackgroundMusic != null) {
						playerWrapperLookup.get(event.getPlayer()).playMusic(getNextBackgroundMusic);
					}
				}
			}
		}, plugin);
	}

	public void tick() {
		long currentTime = System.nanoTime();
		PlayerWrapper player;
		while(playerEventQueue.size() > 0 && currentTime >= (player = playerEventQueue.first()).nextEvent) {
			playerEventQueue.remove(player);
			if(player.nextEvent(currentTime)) {
				playerEventQueue.add(player);
			}
		}
	}

	public void playMusicBackgroundSequence(Function<PlayerWrapper, Music> getNextMusic) {
		getNextBackgroundMusic = getNextMusic;
		playMusicAllSequence(getNextMusic);
	}

	public void playMusicAllOnce(Music music) {
		playMusicAllSequence(p -> p.index == 0 ? music : null);
	}

	public void playMusicAllSequence(Function<PlayerWrapper, Music> getNextMusic) {
		stopMusicAll();
		playerWrapperLookup.values().forEach(player -> player.playMusic(getNextMusic));
	}

	public void stopMusicAll() {
		playerWrapperLookup.values().forEach(PlayerWrapper::stopMusic);
	}

	public class PlayerWrapper implements Comparable<PlayerWrapper> {
		public Music currentMusic;
		public int index;

		public long nextEvent;

		public long userLong;

		public Player player;

		public Function<PlayerWrapper, Music> getNextMusic;

		public PlayerWrapper(Player player) {
			this.player = player;
		}

		public boolean nextEvent(long currentTime) {
			if(getNextMusic == null) {
				return false;
			}

			currentMusic = getNextMusic.apply(this);
			if(currentMusic == null) {
				return false;
			}

			nextEvent = currentTime + (long) (currentMusic.length * 1e9);
			index += 1;

			if (currentMusic.resourceName != null) {
				player.playSound(player.getLocation(), currentMusic.resourceName, SoundCategory.MUSIC, 1, 1);
			}
			return true;
		}

		public void playMusic(Function<PlayerWrapper, Music> getNextMusic) {
			stopMusic();
			userLong = 0;
			this.getNextMusic = getNextMusic;
			nextEvent = 0;
			index = 0;
			playerEventQueue.add(this);
		}

		public void stopMusic() {
			if(currentMusic != null) {
				if(currentMusic.resourceName != null) {
					player.stopSound(currentMusic.resourceName, SoundCategory.MUSIC);
				}
			}
			playerEventQueue.remove(this);
		}

		public int compareTo(PlayerWrapper o) {
			return Long.compare(nextEvent, o.nextEvent);
		}

		public String toString() {
			return "PlayerWrapper[player=" + player.getName() + ", music=" + currentMusic + ", nextEvent=" + nextEvent + "]";
		}
	}

	public static class Music {
		public String resourceName;
		public double length;
		public Music(String resourceName, double length) {
			this.resourceName = resourceName;
			this.length = length;
		}

		public String toString() {
			return "Music[res=" + resourceName + ", len=" + length + "]";
		}
	}
}
