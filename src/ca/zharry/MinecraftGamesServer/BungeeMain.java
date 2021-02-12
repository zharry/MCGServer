package ca.zharry.MinecraftGamesServer;

import net.md_5.bungee.ServerConnection;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class BungeeMain extends Plugin implements Listener {

	public static final String BUNGEE_CHANNEL_NAME = "mcg:resource_pack_info";
	public HashMap<Connection, String> playerHashInfo = new HashMap<>();

	@Override
	public void onEnable() {
		getProxy().registerChannel(BUNGEE_CHANNEL_NAME);
		getProxy().getPluginManager().registerListener(this, this);
	}

	@EventHandler
	public void onPostLogin(PostLoginEvent event) {
		playerHashInfo.put(event.getPlayer(), "");
	}

	@EventHandler
	public void onPlayerDisconnect(PlayerDisconnectEvent event) {
		playerHashInfo.remove(event.getPlayer());
	}

	@EventHandler
	public void onServerConnected(ServerConnectedEvent event) {
	}

	@EventHandler
	public void onPluginMessage(PluginMessageEvent event) {
		if(event.getTag().equals(BUNGEE_CHANNEL_NAME)) {
			byte[] data = event.getData();
			if (data.length == 0) {
				((ServerConnection) event.getSender()).sendData(BUNGEE_CHANNEL_NAME, playerHashInfo.get(event.getReceiver()).getBytes(StandardCharsets.UTF_8));
			} else {
				playerHashInfo.put(event.getReceiver(), new String(event.getData(), StandardCharsets.UTF_8));
			}
		}
	}
}
