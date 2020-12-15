package ca.zharry.MinecraftGamesServer.Servers;

import ca.zharry.MinecraftGamesServer.Commands.CommandTimerPause;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerResume;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerSet;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerStart;
import ca.zharry.MinecraftGamesServer.Listeners.DisableHunger;
import ca.zharry.MinecraftGamesServer.Listeners.ListenerOnPlayerDeathDodgeball;
import ca.zharry.MinecraftGamesServer.Listeners.ListenerOnPlayerJoinDodgeball;
import ca.zharry.MinecraftGamesServer.Listeners.ListenerOnPlayerQuitDodgeball;
import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Players.PlayerDodgeball;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Timer.Timer;
import javafx.geometry.Point3D;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class ServerDodgeball extends ServerInterface {

    // Ingame variables
    public ArrayList<ArrayList<Integer>> roundRobin; // 2D Array of when (i,j) should play each other
    public HashMap<Integer, Integer> rrIndexToTeamId;
    public int currentGame = 0;
    public int totalGames = 0;

    // Game config
    public static final int TIMER_STARTING = 60 * 20;
    public static final int TIMER_INPROGRESS = 1 * 60 * 20;
    public static final int TIMER_FINISHED = 45 * 20;
    public static final ArrayList<Point3D> arenaSpawns = new ArrayList<Point3D>(); // RED Team spawns (BLUE Team is y + 45)
    public static final ArrayList<Point3D> arenaSpectator = new ArrayList<Point3D>();

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

    public ServerDodgeball(JavaPlugin plugin) {
        super(plugin);
        initArenaSpawns();

        roundRobin = new ArrayList<ArrayList<Integer>>();
        rrIndexToTeamId = new HashMap<Integer, Integer>();

        // Add existing players (for hot-reloading)
        ArrayList<Player> currentlyOnline = new ArrayList<>(Bukkit.getOnlinePlayers());
        for (Player player: currentlyOnline) {
            addPlayer(new PlayerDodgeball(player, this));
        }

        dodgeballRoundRobinSetup();

        timerStartGame = new Timer(plugin) {
            @Override
            public void onStart() {
                dodgeballPreStart();
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
                dodgeballStart();
                state = GAME_INPROGRESS;
            }
            @Override
            public void onTick() {
                dodgeballTick();
            }
            @Override
            public void onEnd() {
                dodgeballEnd();
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
        plugin.getServer().getPluginManager().registerEvents(new ListenerOnPlayerJoinDodgeball(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ListenerOnPlayerQuitDodgeball(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ListenerOnPlayerDeathDodgeball(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DisableHunger(), plugin);
    }

    private void dodgeballRoundRobinSetup() {
        // Check if there is an even number of teams
        int numTeams = teamIDs.size();
        totalGames = numTeams - 1;
        if (numTeams % 2 != 0 || numTeams > 8) {
            MCGMain.logger.warning("There must be an even number of teams (<=8 total) in order to start Dodgeball!");
            timerStartGame.set(TIMER_STARTING);
            timerStartGame.set(TIMER_STARTING);
            state = GAME_WAITING;
            return;
        }

        // Generate round robin: https://math.stackexchange.com/questions/535586/when-is-round-robin-scheduling-possible-and-with-in-minimal-time
        for (int i = 0; i < numTeams; i++) {
            int num = i;
            ArrayList<Integer> rows = new ArrayList<Integer>();
            int skipped = -1;
            for (int j = 0; j < numTeams - 1; j++) {
                if (i == j) {
                    rows.add(0);
                    skipped = num;
                }
                else if (i < j) {
                    rows.add(num);
                }
                else {
                    rows.add(0);
                }
                num++;
                if (num >= numTeams) num = 1;
            }
            if (skipped == 0)
                rows.add(numTeams - 1);
            else
                rows.add(skipped);
            roundRobin.add(rows);
        }

        MCGMain.logger.info("Round robin game setup:");
        for (int i = 0; i < roundRobin.size(); i++) {
            String a = "";
            for (int j = 0; j < roundRobin.get(i).size(); j++) {
                a += " " + roundRobin.get(i).get(j);
            }
            MCGMain.logger.info(a);
        }

        Collections.shuffle(teamIDs);
        for (int i = 0; i < roundRobin.size(); i++) {
            rrIndexToTeamId.put(i, teamIDs.get(i));
        }
    }

    private void dodgeballPreStart() {
        currentGame++;
        for (int i = 0; i < roundRobin.size(); i++) {
            for (int j = 0; j < roundRobin.get(i).size(); j++) {
                if (roundRobin.get(i).get(j) == currentGame) {
                    MCGTeam team1 = teams.get(rrIndexToTeamId.get(i));
                    MCGTeam team2 = teams.get(rrIndexToTeamId.get(j));

                    // This section of code is from dodgeballStart() except without the teleporting
                    // It's just for settings the opponentTeam field in each player
                    for (PlayerInterface player: players) {
                        PlayerDodgeball playerDodgeball = (PlayerDodgeball) player;
                        if (player.myTeam.id == team1.id)
                            playerDodgeball.opponentTeam = team2;
                        if (player.myTeam.id == team2.id)
                            playerDodgeball.opponentTeam = team1;
                    }
                }
            }
        }
    }

    private void dodgeballStart() {
        int arenaNo = 0;
        for (int i = 0; i < roundRobin.size(); i++) {
            for (int j = 0; j < roundRobin.get(i).size(); j++) {
                if (roundRobin.get(i).get(j) == currentGame) {
                    MCGTeam team1 = teams.get(rrIndexToTeamId.get(i));
                    MCGTeam team2 = teams.get(rrIndexToTeamId.get(j));

                    MCGMain.logger.info("Round " + currentGame + " starting: " +
                            "(" + i + ", " + team1.id + ", " + team1.teamname + ") vs " +
                            "(" + j + ", " + team2.id + ", " + team2.teamname + ")");

                    Point3D redSpawnLocation = arenaSpawns.get(arenaNo);
                    Point3D blueSpawnLocation = arenaSpawns.get(arenaNo).add(0, 0, 45);
                    arenaNo++;

                    for (PlayerInterface player: players) {
                        PlayerDodgeball playerDodgeball = (PlayerDodgeball) player;

                        // Send team 1 to RED spawn
                        if (player.myTeam.id == team1.id) {
                            playerDodgeball.opponentTeam = team2;
                            playerDodgeball.arena = arenaNo;
                            Location redSpawn = new Location(player.bukkitPlayer.getWorld(),
                                    redSpawnLocation.getX(), redSpawnLocation.getY(), redSpawnLocation.getZ());
                            player.bukkitPlayer.teleport(redSpawn);
                            player.bukkitPlayer.setBedSpawnLocation(redSpawn, true);
                        }
                        // Send team 2 to RED spawn
                        if (player.myTeam.id == team2.id) {
                            playerDodgeball.opponentTeam = team1;
                            playerDodgeball.arena = arenaNo;
                            Location blueSpawn = new Location(player.bukkitPlayer.getWorld(),
                                    blueSpawnLocation.getX(), blueSpawnLocation.getY(), blueSpawnLocation.getZ());
                            player.bukkitPlayer.teleport(blueSpawn);
                            player.bukkitPlayer.setBedSpawnLocation(blueSpawn, true);
                        }
                    }
                }
            }
        }

        // Reset kills and lives counter
        for (PlayerInterface player: players) {
            PlayerDodgeball playerDodgeball = (PlayerDodgeball) player;
            playerDodgeball.kills = 0;
            playerDodgeball.lives = 3;
        }
    }

    private void dodgeballTick() {
    }

    private void dodgeballEnd() {
        for (PlayerInterface player: players) {
            Location serverSpawn = new Location(player.bukkitPlayer.getWorld(), -15.5, 4, 1.5);
            player.bukkitPlayer.teleport(serverSpawn);
            player.bukkitPlayer.setBedSpawnLocation(serverSpawn, true);
        }

        if (currentGame >= totalGames) {
            timerFinished.start();
            return;
        }

        timerStartGame.set(TIMER_STARTING);
        timerInProgress.set(TIMER_INPROGRESS);
        timerStartGame.start();
    }

    private void initArenaSpawns() {
        arenaSpawns.add(new Point3D(57.5, 4, 44.5));
        arenaSpawns.add(new Point3D(57.5 - 50, 4, 44.5));
        arenaSpawns.add(new Point3D(57.5 - 100, 4, 44.5));
        arenaSpawns.add(new Point3D(57.5 - 150, 4, 44.5));

        arenaSpectator.add(new Point3D(73.5, 14, 56.5));
        arenaSpectator.add(new Point3D(73.5 - 50, 14, 56.5));
        arenaSpectator.add(new Point3D(73.5 - 100, 14, 56.5));
        arenaSpectator.add(new Point3D(73.5 - 150, 14, 56.5));
    }

}
