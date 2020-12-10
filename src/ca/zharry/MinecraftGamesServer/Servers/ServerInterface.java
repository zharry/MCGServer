package ca.zharry.MinecraftGamesServer.Servers;

import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class ServerInterface {

    public HashMap<String, String> minigames = new HashMap<String, String>();

    public JavaPlugin javaPlugin;
    public Plugin plugin;

    public BukkitTask taskScoreboard;
    public ArrayList<PlayerInterface> players;

    public ServerInterface(JavaPlugin plugin) {
        this.javaPlugin = plugin;
        this.plugin = plugin;
        this.players = new ArrayList<>();

        taskScoreboard = new BukkitRunnable() {
            @Override
            public void run() {
                for (PlayerInterface player: players) {
                    player.updateScoreboard();
                }
            }
        }.runTaskTimer(plugin, 0, 5);

        minigames.put("parkour", "Parkour");
        minigames.put("spleef", "Spleef");
        minigames.put("dodgeball", "Dodgeball");
    }

    public void onEnableCall() {
        this.registerCommands();
        this.registerListeners();
    }

    public void onDisableCall() {
        // Commit existing players (for hot-reloading)
        for (PlayerInterface player: players) {
            player.commit();
        }
    }

    public abstract void registerCommands();

    public abstract void registerListeners();

    public void sendPlayersToLobby() {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF("lobby");

        for (int i = 0; i < players.size(); i++) {
            players.get(i).bukkitPlayer.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        }
    }

}
