package ca.zharry.MinecraftGamesServer.Servers;

import ca.zharry.MinecraftGamesServer.Commands.CommandTimerPause;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerResume;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerSet;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerStart;
import ca.zharry.MinecraftGamesServer.Listeners.ChangeGameRule;
import ca.zharry.MinecraftGamesServer.Listeners.DisableHunger;
import ca.zharry.MinecraftGamesServer.Listeners.ListenerDodgeball;
import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Players.PlayerDodgeball;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Timer.Timer;
import ca.zharry.MinecraftGamesServer.Utils.PlayerUtils;
import ca.zharry.MinecraftGamesServer.Utils.Point3D;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class ServerDodgeball extends ServerInterface {

    // Ingame variables
    public ArrayList<ArrayList<Integer>> roundRobin; // 2D Array of when (i,j) should play each other
    public HashMap<Integer, Integer> rrIndexToTeamId;
    public int currentGame = 0;
    public int totalGames = 0;
    public boolean displayedWelcomeMessage = false;
    public int spawnArrowsTick = 0;

    // Game config
    public static final int TIMER_STARTING = 60 * 20;
    public static final int TIMER_INPROGRESS = 1 * 60 * 20;
    public static final int TIMER_FINISHED = 45 * 20;
    public static final int SPAWN_ARROWS_TICK = 20 * 20;
    public static final ArrayList<Point3D> arenaSpawns = new ArrayList<Point3D>(); // RED Team spawns (BLUE Team is y + 45)
    public static final ArrayList<Point3D> arenaArrowSpawns = new ArrayList<Point3D>();

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
        for (Player player : currentlyOnline) {
            Location serverSpawn = new Location(player.getWorld(), -15.5, 4, 1.5);
            player.teleport(serverSpawn);
            PlayerUtils.resetPlayer(player, GameMode.ADVENTURE);
            addPlayer(new PlayerDodgeball(player, this));
        }

        dodgeballRoundRobinSetup();

        timerStartGame = new Timer(plugin) {
            @Override
            public void onStart() {
                state = GAME_STARTING;
                dodgeballPreStart();

                if (!displayedWelcomeMessage) {
                    displayedWelcomeMessage = true;
                    sendTitleAll("Welcome to Dodgeball!", "");
                    sendMultipleMessageAll(new String[]{
                            ChatColor.RED + "" + ChatColor.BOLD + "Welcome to Dodgeball!\n" + ChatColor.RESET +
                                    "This map is " + ChatColor.BOLD + "SG Dodgeball" + ChatColor.RESET + ", by SkyGames Team\n" +
                                    " \n" +
                                    " \n",
                            ChatColor.GREEN + "" + ChatColor.BOLD + "How to play:\n" + ChatColor.RESET +
                                    "1. You have three lives\n" +
                                    "2. Kill the other team, each kill is worth +50 points!\n" +
                                    "3. Eliminating all 3 lives of every opposing team's players awards everyone on your team +250 points each!",
                    }, new int[]{
                            10,
                            45,
                    });
                }
            }

            @Override
            public void onTick() {
                countdownTimer(this, 11,
                        "Ready?",
                        "",
                        "",
                        ChatColor.GREEN + "Fight!");
            }

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
                countdownTimer(this, 11,
                        "Round ends in...",
                        "",
                        "",
                        "Round Over!");
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
            public void onTick() {
            }

            @Override
            public void onEnd() {
                sendTitleAll("Joining Lobby...", "", 5, 20, 30);
                sendPlayersToLobby();

                state = GAME_WAITING;
                displayedWelcomeMessage = false;
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
        plugin.getServer().getPluginManager().registerEvents(new ListenerDodgeball(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DisableHunger(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ChangeGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ChangeGameRule(GameRule.DO_FIRE_TICK, false), plugin);
    }

    public void giveBow(PlayerDodgeball player) {
        ItemStack bow = new ItemStack(Material.BOW, 1);
        ItemMeta bowMeta = bow.getItemMeta();
        bowMeta.addEnchant(Enchantment.ARROW_DAMAGE, 32767, true);
        bowMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        bowMeta.setUnbreakable(true);
        bow.setItemMeta(bowMeta);
        player.bukkitPlayer.getInventory().addItem(bow);
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
                } else if (i < j) {
                    rows.add(num);
                } else {
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
                    for (PlayerInterface player : players) {
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
        // Remove all lingering item drops
        List<Entity> entList = players.get(0).bukkitPlayer.getWorld().getEntities();
        for (Entity current : entList) {
            if (current instanceof Item || current instanceof Arrow) {
                current.remove();
            }
        }

        // Set arrow spawner to immediately give both teams one arrow on start
        spawnArrowsTick = 0;

        // Assign each team an arena
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

                    for (PlayerInterface player : players) {
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
        for (PlayerInterface player : players) {
            PlayerDodgeball playerDodgeball = (PlayerDodgeball) player;
            playerDodgeball.kills = 0;
            playerDodgeball.lives = 3;
            playerDodgeball.invulnerable = true;

            PlayerUtils.resetPlayer(playerDodgeball.bukkitPlayer, GameMode.ADVENTURE);
            playerDodgeball.bukkitPlayer.setInvisible(true);
            giveBow(playerDodgeball);
        }
    }

    private void dodgeballTick() {
        // Logic for spawning arrows
        World world = players.get(0).bukkitPlayer.getWorld();
        spawnArrowsTick--;
        if (spawnArrowsTick <= 0) {
            spawnArrowsTick = SPAWN_ARROWS_TICK;
            for (Point3D point : arenaArrowSpawns) {
                Location arrowSpawn = new Location(world, point.getX(), point.getY(), point.getZ());
                ItemStack arrow = new ItemStack(Material.ARROW, 1);
                world.dropItem(arrowSpawn, arrow);
            }
            sendActionBarAll("An arrow has spawned!");
        } else if (spawnArrowsTick <= SPAWN_ARROWS_TICK - 5 * 20) {
            int seconds = (int) (spawnArrowsTick / 20 + 0.5);
            sendActionBarAll("Next arrow spawns in: " + seconds + " seconds");
        }

        if(timerInProgress.get() > 12 * 20) {
            boolean terminate = true;
            for (PlayerInterface player : players) {
                PlayerDodgeball playerDodgeball = (PlayerDodgeball) player;
                if (playerDodgeball.bukkitPlayer.getGameMode() != GameMode.SPECTATOR && playerDodgeball.arena != -1) {
                    terminate = false;
                    break;
                }
            }

            if (terminate) {
                timerInProgress.set(12 * 20);
            }
        }
    }

    private void dodgeballEnd() {
        ArrayList<PlayerDodgeball> playerDodgeballs = new ArrayList<>();
        for (PlayerInterface player : players) {
            playerDodgeballs.add((PlayerDodgeball) player);
            Location serverSpawn = new Location(player.bukkitPlayer.getWorld(), -15.5, 4, 1.5);
            PlayerUtils.resetPlayer(player.bukkitPlayer, GameMode.ADVENTURE);
            player.bukkitPlayer.teleport(serverSpawn);
            player.bukkitPlayer.setBedSpawnLocation(serverSpawn, true);
        }

        if (currentGame < totalGames) {
            timerStartGame.set(TIMER_STARTING);
            timerInProgress.set(TIMER_INPROGRESS);
            timerStartGame.start();
            spawnArrowsTick = 0;
            return;
        }

        // Game over logic begins
        timerFinished.start();

        String topPlayers = "";
        int count = 0;
        playerDodgeballs.sort(Comparator.comparingInt(o -> -o.currentScore));
        for (PlayerDodgeball player: playerDodgeballs) {
            topPlayers += ChatColor.RESET + "[" + player.currentScore + "] " + player.myTeam.chatColor + "" + player.bukkitPlayer.getDisplayName() + ChatColor.RESET + "\n";
            if (++count > 5) {
                break;
            }
        }

        String topKillers = "";
        count = 0;
        playerDodgeballs.sort(Comparator.comparingInt(o -> -o.totalKills));
        for (PlayerDodgeball player: playerDodgeballs) {
            topKillers += ChatColor.RESET + "" + player.totalKills + " kills - " + player.myTeam.chatColor + "" + player.bukkitPlayer.getDisplayName() + ChatColor.RESET + "\n";
            if (++count > 5) {
                break;
            }
        }

        String topTeams = "";
        ArrayList<MCGTeam> teamDodgeballs = new ArrayList<>(teams.values());
        teamDodgeballs.sort(Comparator.comparingInt(o -> -o.getScore("dodgeball")));
        for (MCGTeam team: teamDodgeballs) {
            topTeams += ChatColor.RESET + "[" + team.getScore("dodgeball") + "] " + team.chatColor + "" + team.teamname + "\n";
        }

        sendMultipleMessageAll(new String[]{
                ChatColor.BOLD + "Top Players:\n" + topPlayers +
                        " \n",
                ChatColor.BOLD + "Top Killers:\n" + topKillers +
                        " \n",
                ChatColor.BOLD + "Final Team Score for Survival Games:\n" + topTeams,
        }, new int[]{
                10,
                60,
                60,
        });

    }

    private void initArenaSpawns() {
        arenaSpawns.add(new Point3D(10014.5, 4, 1.5));
        arenaSpawns.add(new Point3D(10014.5 + 10000, 4, 1.5));
        arenaSpawns.add(new Point3D(10014.5 + 20000, 4, 1.5));
        arenaSpawns.add(new Point3D(10014.5 + 30000, 4, 1.5));

        arenaArrowSpawns.add(new Point3D(10014.5, 4, 15.5));
        arenaArrowSpawns.add(new Point3D(10014.5, 4, 32.5));
        arenaArrowSpawns.add(new Point3D(10014.5 + 10000, 4, 15.5));
        arenaArrowSpawns.add(new Point3D(10014.5 + 10000, 4, 32.5));
        arenaArrowSpawns.add(new Point3D(10014.5 + 20000, 4, 15.5));
        arenaArrowSpawns.add(new Point3D(10014.5 + 20000, 4, 32.5));
        arenaArrowSpawns.add(new Point3D(10014.5 + 30000, 4, 15.5));
        arenaArrowSpawns.add(new Point3D(10014.5 + 30000, 4, 32.5));
    }

}
