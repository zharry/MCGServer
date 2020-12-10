package ca.zharry.MinecraftGamesServer.Servers;

import ca.zharry.MinecraftGamesServer.Commands.CommandTimerPause;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerResume;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerSet;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerStart;
import ca.zharry.MinecraftGamesServer.Listeners.DisableDamage;
import ca.zharry.MinecraftGamesServer.Listeners.DisableHunger;
import ca.zharry.MinecraftGamesServer.Listeners.ListenerOnPlayerJoinParkour;
import ca.zharry.MinecraftGamesServer.Listeners.ListenerOnPlayerQuitParkour;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Players.PlayerParkour;
import ca.zharry.MinecraftGamesServer.Timer.Timer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;

public class ServerParkour extends ServerInterface {

    // Game config
    public static final int TIMER_STARTING = 60 * 20;
    public static final int TIMER_INPROGRESS = 15 * 60 * 20;
    public static final int TIMER_FINISHED = 45 * 20;

    // Server state
    public static final int ERROR = -1;
    public static final int GAME_WAITING = 0;
    public static final int GAME_STARTING = 1;
    public static final int GAME_INPROGRESS = 2;
    public static final int GAME_FINISHED = 3;
    public int state = ERROR;

    // Server tasks
    public Timer timerStartGame;
    public Timer timerInProgress;
    public Timer timerFinished;

    public ServerParkour(JavaPlugin plugin) {
        super(plugin);

        // Add existing players (for hot-reloading)
        ArrayList<Player> currentlyOnline = new ArrayList<>(Bukkit.getOnlinePlayers());
        for (Player player: currentlyOnline) {
            players.add(new PlayerParkour(player, this));
        }

        timerStartGame = new Timer(plugin) {
            @Override
            public void onStart() {
                state = GAME_STARTING;
            }
            @Override
            public void onTick() { }
            @Override
            public void onEnd() {
                timerInProgress.start();
            }
        }.set(TIMER_STARTING);

        timerInProgress = new Timer(plugin) {
            @Override
            public void onStart() {
                state = GAME_INPROGRESS;
                parkourStart();
            }
            @Override
            public void onTick() {
                parkourTick();
            }
            @Override
            public void onEnd() {
                parkourEnd();
                timerFinished.start();
            }
        }.set(TIMER_INPROGRESS);

        timerFinished = new Timer(plugin) {
            @Override
            public void onStart() {
                state = GAME_FINISHED;
            }
            @Override
            public void onTick() { }
            @Override
            public void onEnd() {
                sendPlayersToLobby();

                state = GAME_WAITING;
                timerStartGame.set(TIMER_STARTING);
                timerInProgress.set(TIMER_INPROGRESS);
                timerFinished.set(TIMER_FINISHED);
            }
        }.set(TIMER_FINISHED);
    }

    @Override
    public void onEnableCall() {
        super.onEnableCall();
        this.state = GAME_WAITING;
    }

    @Override
    public void onDisableCall() {
        super.onDisableCall();
        this.state = ERROR;
    }

    @Override
    public void registerCommands() {
        javaPlugin.getCommand("start").setExecutor(new CommandTimerStart(timerStartGame));
        javaPlugin.getCommand("timerstartset").setExecutor(new CommandTimerSet(timerStartGame));
        javaPlugin.getCommand("timerstartpause").setExecutor(new CommandTimerPause(timerStartGame));
        javaPlugin.getCommand("timerstartresume").setExecutor(new CommandTimerResume(timerStartGame));
        javaPlugin.getCommand("timergameset").setExecutor(new CommandTimerSet(timerInProgress));
        javaPlugin.getCommand("timergamepause").setExecutor(new CommandTimerPause(timerInProgress));
        javaPlugin.getCommand("timergameresume").setExecutor(new CommandTimerResume(timerInProgress));
        javaPlugin.getCommand("timerfinishedset").setExecutor(new CommandTimerSet(timerFinished));
        javaPlugin.getCommand("timerfinishedpause").setExecutor(new CommandTimerPause(timerFinished));
        javaPlugin.getCommand("timerfinishedresume").setExecutor(new CommandTimerResume(timerFinished));
        plugin.getServer().getPluginManager().registerEvents(new DisableHunger(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DisableDamage(), plugin);
    }

    @Override
    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(new ListenerOnPlayerJoinParkour(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ListenerOnPlayerQuitParkour(this), plugin);
    }

    private void parkourStart() {
        Player dummyPlayer = players.get(0).bukkitPlayer;
        Location mapStart = new Location(dummyPlayer.getWorld(), 7.5, 78, 9.5);

        for (PlayerInterface player: players) {
            player.bukkitPlayer.teleport(mapStart);
        }
    }
    private void parkourTick() {
        for (int i = 0; i < players.size(); i++) {
            PlayerParkour player = (PlayerParkour) players.get(i);
            Player bukkitPlayer = player.bukkitPlayer;

            // Check if the player is on a Beacon
            if (bukkitPlayer.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.BEACON) {
                Location blockLocation = bukkitPlayer.getLocation().getBlock().getLocation();

                // Check if we're been here already
                for (Location loc : player.parkourFinished) {
                    if (loc.getX() == blockLocation.getX() && loc.getY() == blockLocation.getY() && loc.getZ() == blockLocation.getZ()) {
                        return;
                    }
                }

                // Award score and add to finished, show message
                bukkitPlayer.sendTitle(parkourCheckpoints.get(player.parkourFinished.size()), "Checkpoint Completed", 10, 30, 10);
                player.parkourFinished.add(blockLocation);
                player.currentScore += 150;

                // Check if we finished the map
                if (player.parkourFinished.size() == parkourCheckpoints.size()) {
                    bukkitPlayer.teleport(new Location(bukkitPlayer.getWorld(), 8.5, 131, 9.5));
                }
            }
        }

    }
    private void parkourEnd() {
        Player dummyPlayer = players.get(0).bukkitPlayer;
        Location mapEnd = new Location(dummyPlayer.getWorld(), 8.5, 131, 9.5);

        for (PlayerInterface player: players) {
            if (player.bukkitPlayer.getLocation().getY() < 130) {
                player.bukkitPlayer.teleport(mapEnd);
            }
            player.commit();
        }
    }

    public static ArrayList<String> parkourCheckpoints = new ArrayList<>(Arrays.asList(
            "Stage 1-1",
            "Stage 1-2",
            "Stage 1-3",
            "Stage 1-4"//,
            //"Stage 2-1",
            //"Stage 2-2",
            //"Stage 2-3",
            //"Stage 2-4",
            //"Stage 3-1",
            //"Stage 3-2",
            //"Stage 3-3",
            //"Stage 3-4",
            //"Stage 4-1",
            //"Stage 4-2",
            //"Stage 4-3",
            //"Stage 4-4",
            //"Stage 4-5",
            //"Stage 5-1",
            //"Stage 5-2",
            //"Stage 6-1",
            //"Stage 6-2",
            //"Stage 6-3",
            //"Stage 6-4",
            //"Stage 6-5",
            //"Stage 6-6",
            //"Stage 6-7",
            //"Stage 6-8"
    ));

}
