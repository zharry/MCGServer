package ca.zharry.MinecraftGamesServer.Utils;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Servers.ServerInterface;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ResourcePackManager {

	public String resourcePackURL;
	public String hash;
	public JavaPlugin plugin;

	public ResourcePackManager(JavaPlugin plugin) {
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(new Listener() {
			@EventHandler
			public void onPlayerJoin(PlayerJoinEvent event) {
				if(resourcePackURL != null) {
					event.getPlayer().setResourcePack(resourcePackURL, hash);
				}
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
					}
				}
			}
		}, plugin);
	}

	public void forceResourcePack(String resourcePackURL, String hash) {
		this.resourcePackURL = resourcePackURL;
		this.hash = hash;
		System.out.println("Forcing resource pack " + resourcePackURL);
		plugin.getServer().getOnlinePlayers().forEach(player -> player.setResourcePack(resourcePackURL, hash));
	}
}
