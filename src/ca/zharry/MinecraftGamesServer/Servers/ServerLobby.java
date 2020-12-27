package ca.zharry.MinecraftGamesServer.Servers;

import ca.zharry.MinecraftGamesServer.Commands.CommandLobbySetNextGame;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerPause;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerResume;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerSet;
import ca.zharry.MinecraftGamesServer.Listeners.DisableDamage;
import ca.zharry.MinecraftGamesServer.Listeners.DisableHunger;
import ca.zharry.MinecraftGamesServer.Listeners.ListenerLobby;
import ca.zharry.MinecraftGamesServer.Players.PlayerLobby;
import ca.zharry.MinecraftGamesServer.Timer.Timer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public class ServerLobby extends ServerInterface {

    // Lobby config
    public static final int TIMER_START = 60 * 20;

    // Server states
    public static final int ERROR = -1;
    public static final int LOBBY_WAITING = 0;
    public static final int LOBBY_STARTED = 1;
    public int state = ERROR;
    public String nextMinigame;

    // Server tasks
    public Timer timerNextGame;

    public ServerLobby(JavaPlugin plugin) {
        super(plugin);

        // Add existing players (for hot-reloading)
        ArrayList<Player> currentlyOnline = new ArrayList<>(Bukkit.getOnlinePlayers());
        for (Player player : currentlyOnline) {
            addPlayer(new PlayerLobby(player, this));
        }

        timerNextGame = new Timer(plugin) {
            @Override
            public void onStart() {
                state = LOBBY_STARTED;
                sendTitleAll(minigames.get(nextMinigame), "is starting in 60 seconds!");
            }

            @Override
            public void onTick() {
                countdownTimer(this, 10,
                        "",
                        "",
                        "Teleporting to " + minigames.get(nextMinigame),
                        "Loading " + minigames.get(nextMinigame) + "...");
            }

            @Override
            public void onEnd() {
                sendPlayersToGame(nextMinigame);

                state = LOBBY_WAITING;
                timerNextGame.set(TIMER_START);
            }
        }.set(TIMER_START);
    }

    @Override
    public void onEnableCall() {
        super.onEnableCall();
        this.state = LOBBY_WAITING;
    }

    @Override
    public void onDisableCall() {
        super.onDisableCall();
        this.state = ERROR;
    }

    @Override
    public void registerCommands() {
        javaPlugin.getCommand("setgame").setExecutor(new CommandLobbySetNextGame(this, timerNextGame));
        javaPlugin.getCommand("timernextset").setExecutor(new CommandTimerSet(timerNextGame));
        javaPlugin.getCommand("timernextpause").setExecutor(new CommandTimerPause(timerNextGame));
        javaPlugin.getCommand("timernextresume").setExecutor(new CommandTimerResume(timerNextGame));
    }

    @Override
    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(new ListenerLobby(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DisableHunger(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DisableDamage(), plugin);
    }

}
