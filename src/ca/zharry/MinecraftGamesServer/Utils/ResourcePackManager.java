package ca.zharry.MinecraftGamesServer.Utils;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Servers.ServerInterface;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HashMap;

public class ResourcePackManager {

	public String resourcePackURL;
	public String hash;
	public JavaPlugin plugin;

	public HashMap<Player, String> resourcePackHash = new HashMap<>();

	private static byte[] buffer = new byte[4096];

	public ResourcePackManager(JavaPlugin plugin) {
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(new Listener() {
			@EventHandler
			public void onPlayerJoin(PlayerJoinEvent event) {
				BungeeManager.requestPlayerResourcePackInfo(plugin, event.getPlayer());
			}
			@EventHandler
			public void onPlayerQuit(PlayerQuitEvent event) {
				resourcePackHash.remove(event.getPlayer());
			}
			@EventHandler
			public void onPlayerResourcePackStatusEvent(PlayerResourcePackStatusEvent event) {
				if(resourcePackURL != null) {
					Player player = event.getPlayer();
					PlayerResourcePackStatusEvent.Status status = event.getStatus();
					if(status == PlayerResourcePackStatusEvent.Status.DECLINED) {
						MCGMain.bungeeManager.kickPlayer(player, "You must accept the resource pack\n\nPlease edit the server and set it to \"Server Resource Pack: Enabled\" and rejoin");
					} else if(status == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD) {
						MCGMain.bungeeManager.kickPlayer(player, "Resource pack download failed\n\nPlease rejoin the server\n\n\nIf this issue persists, please contact the organizers");
					} else if(status == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
						BungeeManager.updatePlayerResourcePackInfo(plugin, event.getPlayer(), hash);
					}
				}
			}
		}, plugin);
	}

	public void initialize() {
		Bukkit.getOnlinePlayers().forEach(player -> BungeeManager.requestPlayerResourcePackInfo(plugin, player));
	}

	private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

	public static String bytesToHexString(byte[] arr) {
		char[] str = new char[arr.length * 2];
		for(int i = 0; i < arr.length; ++i) {
			str[i * 2] = HEX_CHARS[(arr[i] >>> 4) & 0xf];
			str[i * 2 + 1] = HEX_CHARS[arr[i] & 0xf];
		}
		return new String(str);
	}

	public static String getHashFromFile(File f) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			try(FileInputStream fileInput = new FileInputStream(f)) {
				try(DigestInputStream digestInputStream = new DigestInputStream(fileInput, digest)) {
					while(digestInputStream.read(buffer) != -1);
					return bytesToHexString(digest.digest());
				}
			}
		} catch(Exception e) {
			throw new RuntimeException("Failed to generate digest for file " + f, e);
		}
	}

	public void forceResourcePack(String resourcePackURL, File file) {
		forceResourcePack(resourcePackURL, getHashFromFile(file));
	}

	public void forceResourcePack(String resourcePackURL, String hash) {
		this.resourcePackURL = resourcePackURL;
		this.hash = hash;

		resourcePackHash.forEach((k, v) -> {
			if(!(v.equals(hash))) {
				k.setResourcePack(resourcePackURL + "?h=" + hash, hash);
			}
		});
	}

	public void bungeePlayerResourcePackInfo(Player player, String s) {
		resourcePackHash.put(player, s);
		if(hash != null && !s.equals(hash)) {
			player.setResourcePack(resourcePackURL + "?h=" + hash, hash);
		}
	}
}
