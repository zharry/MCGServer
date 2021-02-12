package ca.zharry.MinecraftGamesServer.Utils;

import ca.zharry.MinecraftGamesServer.BungeeMain;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Servers.ServerInterface;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;

public class BungeeManager implements PluginMessageListener {

	public final ServerInterface<? extends PlayerInterface> server;
	public final JavaPlugin plugin;
	public ResourcePackManager resourcePackManager;

	public String nextTargetServer;
	public boolean reloadRequested;

	public BungeeManager(JavaPlugin plugin, ServerInterface<? extends PlayerInterface> server, ResourcePackManager resourcePackManager) {
		this.plugin = plugin;
		this.server = server;
		this.resourcePackManager = resourcePackManager;

		plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
		plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", this);
		plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, BungeeMain.BUNGEE_CHANNEL_NAME);
		plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, BungeeMain.BUNGEE_CHANNEL_NAME, this);
	}

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		if (channel.equals(BungeeMain.BUNGEE_CHANNEL_NAME)) {
			resourcePackManager.bungeePlayerResourcePackInfo(player, new String(message, StandardCharsets.UTF_8));
		} else if (channel.equals("BungeeCord")) {
			ByteArrayDataInput input = ByteStreams.newDataInput(message);
			String subchannel = input.readUTF();
			if (subchannel.equals("PlayerList")) {
				if (nextTargetServer == null) {
					return;
				}
				input.readUTF();
				String[] players = input.readUTF().split(", ");
				for (String otherPlayerName : players) {
					ByteArrayDataOutput output;
					if (reloadRequested) {
						output = ByteStreams.newDataOutput();
						output.writeUTF("Forward");
						output.writeUTF(nextTargetServer);
						output.writeUTF("MCG_Reload");
						output.writeShort(0);
						player.sendPluginMessage(plugin, "BungeeCord", output.toByteArray());
						reloadRequested = false;
					}

					output = ByteStreams.newDataOutput();
					output.writeUTF("ConnectOther");
					output.writeUTF(otherPlayerName);
					output.writeUTF(nextTargetServer);
					player.sendPluginMessage(plugin, "BungeeCord", output.toByteArray());
				}
				nextTargetServer = null;
			} else if (subchannel.equals("MCG_Reload")) {
				server.reloadTeamsAndPlayers();
			}
		}
	}

	public void sendAllPlayers(String targetServer, boolean includeAllPlayers, boolean requestReload) {
		if(server.players.size() > 0) {
			ByteArrayDataOutput out = ByteStreams.newDataOutput();
			out.writeUTF("PlayerList");
			out.writeUTF(includeAllPlayers ? "ALL" : server.minigame);
			nextTargetServer = targetServer;
			reloadRequested = requestReload;
			PlayerInterface player = server.players.get(0);
			player.bukkitPlayer.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
		} else {
			System.err.println("There are no players connected to this server, cannot send");
		}
	}

	public void kickPlayer(Player player, String kickString) {
		ByteArrayDataOutput output = ByteStreams.newDataOutput();
		output.writeUTF("KickPlayer");
		output.writeUTF(player.getName());
		output.writeUTF(kickString);
		player.sendPluginMessage(plugin, "BungeeCord", output.toByteArray());
	}

	public static void requestPlayerResourcePackInfo(JavaPlugin plugin, Player player) {
		player.sendPluginMessage(plugin, BungeeMain.BUNGEE_CHANNEL_NAME, new byte[0]);
	}

	public static void updatePlayerResourcePackInfo(JavaPlugin plugin, Player player, String info) {
		player.sendPluginMessage(plugin, BungeeMain.BUNGEE_CHANNEL_NAME, info.getBytes(StandardCharsets.UTF_8));
	}

}
