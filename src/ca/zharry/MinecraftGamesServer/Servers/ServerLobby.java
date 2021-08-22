package ca.zharry.MinecraftGamesServer.Servers;

import ca.zharry.MinecraftGamesServer.Commands.CommandSetGame;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimer;
import ca.zharry.MinecraftGamesServer.Listeners.DisableHunger;
import ca.zharry.MinecraftGamesServer.Listeners.ListenerLobby;
import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Players.PlayerLobby;
import ca.zharry.MinecraftGamesServer.Timer.Timer;
import ca.zharry.MinecraftGamesServer.Utils.MusicManager;
import ca.zharry.MinecraftGamesServer.Utils.Zone;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.util.UUID;

public class ServerLobby extends ServerInterface<PlayerLobby> {

    // Game config
    public static final int TIMER_START = 60 * 20;

    // Server states
    public static final int ERROR = -1;
    public static final int LOBBY_WAITING = 0;
    public static final int LOBBY_STARTED = 1;
    public int state = ERROR;
    public String nextMinigame;

    // Inside the building
    public static final Zone SAFE_REGION = new Zone().rangeX(1443, 1492).rangeZ(511, 549).maxY(29);

    // Server tasks
    public Timer timerNextGame;

    public ServerLobby(JavaPlugin plugin, World world, String minigame) {
        super(plugin, world, minigame);
        serverSpawn = new Location(world, 1484.5, 4, 530, 90, 0);

        timerNextGame = new Timer(plugin, "next", TIMER_START) {
            @Override
            public void onStart() {
                state = LOBBY_STARTED;
                commitAllPlayers();
                sendTitleAll(MCGMain.serverNames.get(nextMinigame), "is starting in 60 seconds!");
            }

            @Override
            public void onTick() {
                countdownTimer(this, 10,
                        "",
                        "",
                        "Teleporting to " + MCGMain.serverNames.get(nextMinigame),
                        "Loading " + MCGMain.serverNames.get(nextMinigame) + "...");
            }

            @Override
            public void onEnd() {
                MCGMain.bungeeManager.sendAllPlayers(nextMinigame, false, true);

                state = LOBBY_WAITING;
                timerNextGame.set(TIMER_START);
            }
        };
    }

    @Override
    public void onEnableCall() {
        super.onEnableCall();
        this.state = LOBBY_WAITING;

        MCGMain.resourcePackManager.forceResourcePack("https://play.mcg-private.tk/test.zip", new File(MCGMain.resourcePackRoot, "test.zip"));
        MusicManager.Music music1 = new MusicManager.Music("tsf:music.glidermusic1", 140.8);
        MusicManager.Music music2 = new MusicManager.Music("tsf:music.glidermusic2", 140.8);
        MCGMain.musicManager.playMusicBackgroundSequence(p -> p.index == 0 ? music1 : music2);
    }

    @Override
    public void onDisableCall() {
        super.onDisableCall();
        this.state = ERROR;
    }

    @Override
    public void registerCommands() {
        plugin.getCommand("setgame").setExecutor(new CommandSetGame(this, timerNextGame));
        plugin.getCommand("timer").setExecutor(new CommandTimer(this, timerNextGame));
    }

    @Override
    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(new ListenerLobby(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DisableHunger(), plugin);
//        plugin.getServer().getPluginManager().registerEvents(new DisableDamage(), plugin);
    }

    @Override
    public PlayerLobby createNewPlayerInterface(UUID uuid, String name) {
        return new PlayerLobby(this, uuid, name);
    }

    @Override
    public void configureScoreboardTeam(Team minecraftTeam, MCGTeam team) {
        super.configureScoreboardTeam(minecraftTeam, team);
        minecraftTeam.setAllowFriendlyFire(true);
    }
}
