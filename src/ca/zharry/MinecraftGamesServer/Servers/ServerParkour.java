package ca.zharry.MinecraftGamesServer.Servers;

import ca.zharry.MinecraftGamesServer.Commands.CommandTimerPause;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerResume;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerSet;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerStart;
import ca.zharry.MinecraftGamesServer.Listeners.DisableDamage;
import ca.zharry.MinecraftGamesServer.Listeners.DisableHunger;
import ca.zharry.MinecraftGamesServer.Listeners.ListenerOnPlayerJoinParkour;
import ca.zharry.MinecraftGamesServer.Listeners.ListenerOnPlayerQuitParkour;
import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Players.PlayerParkour;
import ca.zharry.MinecraftGamesServer.Timer.Timer;
import javafx.geometry.Point3D;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public class ServerParkour extends ServerInterface {

    // Game config
    public static final int TIMER_STARTING = 60 * 20;
    public static final int TIMER_INPROGRESS = 15 * 60 * 20;
    public static final int TIMER_FINISHED = 45 * 20;
    public static final ArrayList<Point3D> stage1Checkpoints = new ArrayList<Point3D>();
    public static final ArrayList<Point3D> stage2Checkpoints = new ArrayList<Point3D>();
    public static final ArrayList<Point3D> stage3Checkpoints = new ArrayList<Point3D>();
    public static final ArrayList<Point3D> stage4Checkpoints = new ArrayList<Point3D>();
    public static final ArrayList<Point3D> stage5Checkpoints = new ArrayList<Point3D>();
    public static final ArrayList<Point3D> stage6Checkpoints = new ArrayList<Point3D>();

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
        initCheckpoints();

        // Add existing players (for hot-reloading)
        ArrayList<Player> currentlyOnline = new ArrayList<>(Bukkit.getOnlinePlayers());
        for (Player player: currentlyOnline) {
            addPlayer(new PlayerParkour(player, this));
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
    }

    @Override
    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(new ListenerOnPlayerJoinParkour(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ListenerOnPlayerQuitParkour(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DisableHunger(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DisableDamage(), plugin);
    }

    private void parkourStart() {
        Player dummyPlayer = players.get(0).bukkitPlayer;
        Location mapStart = new Location(dummyPlayer.getWorld(), 7.5, 78, 9.5);

        for (PlayerInterface player: players) {
            PlayerParkour parkourPlayer = (PlayerParkour) player;
            player.bukkitPlayer.teleport(mapStart);
            parkourPlayer.stage = 1;
            parkourPlayer.level = 0;
        }
    }
    private void parkourTick() {
        for (int i = 0; i < players.size(); i++) {
            PlayerParkour player = (PlayerParkour) players.get(i);
            Player bukkitPlayer = player.bukkitPlayer;

            // Check if the player is on a Beacon
            if (bukkitPlayer.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.BEACON) {
                Location blockLocation = bukkitPlayer.getLocation().getBlock().getLocation();
                Point3D blockPoint = new Point3D(blockLocation.getX(), blockLocation.getY() - 1, blockLocation.getZ());

                // Find out which stage the beacon block is on
                int stage1Index = stage1Checkpoints.indexOf(blockPoint);
                int stage2Index = stage2Checkpoints.indexOf(blockPoint);
                int stage3Index = stage3Checkpoints.indexOf(blockPoint);
                int stage4Index = stage4Checkpoints.indexOf(blockPoint);
                int stage5Index = stage5Checkpoints.indexOf(blockPoint);
                int stage6Index = stage6Checkpoints.indexOf(blockPoint);
                int stage = -1;
                if (stage1Index != -1) stage = 1;
                if (stage2Index != -1) stage = 2;
                if (stage3Index != -1) stage = 3;
                if (stage4Index != -1) stage = 4;
                if (stage5Index != -1) stage = 5;
                if (stage6Index != -1) stage = 6;
                // This represents what level the block is on, for the stage determined above
                int[] levels = { -1, stage1Index, stage2Index, stage3Index, stage4Index, stage5Index, stage6Index };
                // This represents how many levels the player would have completed upon reaching the (i + 1)th stage
                int[] stageCompletedLevels = { 0,
                        stage1Checkpoints.size() - 1,
                        stage1Checkpoints.size() + stage2Checkpoints.size() - 2,
                        stage1Checkpoints.size() + stage2Checkpoints.size() + stage3Checkpoints.size() - 3,
                        stage1Checkpoints.size() + stage2Checkpoints.size() + stage3Checkpoints.size() + stage4Checkpoints.size() - 4,
                        stage1Checkpoints.size() + stage2Checkpoints.size() + stage3Checkpoints.size() + stage4Checkpoints.size() + stage5Checkpoints.size() - 5,
                        stage1Checkpoints.size() + stage2Checkpoints.size() + stage3Checkpoints.size() + stage4Checkpoints.size() + stage5Checkpoints.size() + stage6Checkpoints.size() - 6
                };

                MCGMain.logger.info(stage1Index + " a" + blockLocation.getX() + " a" + blockLocation.getY() + " a" + blockLocation.getZ());
                MCGMain.logger.info(stage2Index + " a " + player.stage + " a" + player.level );
                MCGMain.logger.info(stage3Index + " a " + stage + " a" + levels[stage]);
                MCGMain.logger.info(stage4Index + " a");
                MCGMain.logger.info(stage5Index + " a");
                MCGMain.logger.info(stage6Index + " a");

                // Award points if this is new
                if (stage == player.stage && levels[stage] > player.level ||
                        stage > player.stage && levels[stage] < player.level ) {
                    player.stage = stage;
                    player.level = levels[stage];
                    player.currentScore = stageCompletedLevels[stage - 1] * 150 + levels[stage] * 150;
                    bukkitPlayer.sendTitle("Stage " + stage + "-" + levels[stage], "Checkpoint Completed", 10, 30, 10);
                }

                // Check if we finished the stages
                boolean finishedStage = false;
                Point3D nextStart = stage1Checkpoints.get(0);
                if (stage1Index == stage1Checkpoints.size() - 1) {
                    finishedStage = true;
                    nextStart = stage2Checkpoints.get(0);
                }
                else if (stage2Index == stage2Checkpoints.size() - 1) {
                    finishedStage = true;
                    nextStart = stage3Checkpoints.get(0);
                }
                else if (stage3Index == stage3Checkpoints.size() - 1) {
                    finishedStage = true;
                    nextStart = stage4Checkpoints.get(0);
                }
                else if (stage4Index == stage4Checkpoints.size() - 1) {
                    finishedStage = true;
                    nextStart = stage5Checkpoints.get(0);
                }
                else if (stage5Index == stage5Checkpoints.size() - 1) {
                    finishedStage = true;
                    nextStart = stage6Checkpoints.get(0);
                }
                else if (stage6Index == stage6Checkpoints.size() - 1) {
                    bukkitPlayer.teleport(new Location(bukkitPlayer.getWorld(), 8.5, 131, 9.5));
                    continue;
                }
                if (finishedStage) {
                    Location nextStage = new Location(bukkitPlayer.getWorld(), nextStart.getX() + 0.5, nextStart.getY() + 1, nextStart.getZ() + 0.5);
                    bukkitPlayer.teleport(nextStage);
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

    public void initCheckpoints() {
        stage1Checkpoints.add(new Point3D(7,77,9));
        stage1Checkpoints.add(new Point3D(-18,77,11));
        stage1Checkpoints.add(new Point3D(-39,77,11));
        stage1Checkpoints.add(new Point3D(-73,77,10));
        stage1Checkpoints.add(new Point3D(-82,86,5));
        stage2Checkpoints.add(new Point3D(7,77,23));
        stage2Checkpoints.add(new Point3D(-26,77,22));
        stage2Checkpoints.add(new Point3D(-29,85,27));
        stage2Checkpoints.add(new Point3D(-65,77,23));
        stage2Checkpoints.add(new Point3D(-85,90,24));
        stage3Checkpoints.add(new Point3D(7,77,-5));
        stage3Checkpoints.add(new Point3D(-14,81,-9));
        stage3Checkpoints.add(new Point3D(-38,79,-5));
        stage3Checkpoints.add(new Point3D(-75,78,-5));
        stage3Checkpoints.add(new Point3D(-89,92,-11));
        stage4Checkpoints.add(new Point3D(7,77,37));
        stage4Checkpoints.add(new Point3D(-21,80,32));
        stage4Checkpoints.add(new Point3D(-41,83,34));
        stage4Checkpoints.add(new Point3D(-66,77,35));
        stage4Checkpoints.add(new Point3D(-89,77,41));
        stage4Checkpoints.add(new Point3D(-92,89,43));
        stage5Checkpoints.add(new Point3D(7,77,-19));
        stage5Checkpoints.add(new Point3D(-83,65,-17));
        stage5Checkpoints.add(new Point3D(-91,80,-18));
        stage5Checkpoints.add(new Point3D(-88,89,-22));
        stage6Checkpoints.add(new Point3D(7,77,51));
        stage6Checkpoints.add(new Point3D(-27,80,52));
        stage6Checkpoints.add(new Point3D(-35,84,48));
        stage6Checkpoints.add(new Point3D(-45,92,55));
        stage6Checkpoints.add(new Point3D(-67,98,49));
        stage6Checkpoints.add(new Point3D(-74,106,54));
        stage6Checkpoints.add(new Point3D(-74,111,51));
        stage6Checkpoints.add(new Point3D(-85,118,54));
    }

}
