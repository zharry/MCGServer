package ca.zharry.MinecraftGamesServer.Servers;

import ca.zharry.MinecraftGamesServer.Commands.CommandCutsceneStart;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerPause;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerResume;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerSet;
import ca.zharry.MinecraftGamesServer.Listeners.DisableHunger;
import ca.zharry.MinecraftGamesServer.Listeners.ListenerDodgeball;
import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Players.PlayerDodgeball;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Timer.Cutscene;
import ca.zharry.MinecraftGamesServer.Timer.CutsceneStep;
import ca.zharry.MinecraftGamesServer.Timer.Timer;
import ca.zharry.MinecraftGamesServer.Utils.PlayerUtils;
import ca.zharry.MinecraftGamesServer.Utils.Point3D;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class ServerDodgeball extends ServerInterface {

    // Ingame variables
    public ArrayList<ArrayList<Integer>> roundRobin; // 2D Array of when (i,j) should play each other
    public HashMap<Integer, Integer> rrIndexToTeamId;
    public int currentGame = 0;
    public int totalGames = 0;
    public boolean displayedWelcomeMessage = false;
    public int spawnArrowsTick = 0;

    // Points of interest
    public Location practiceArenaSelect;

    // Game config
    public static final int TIMER_STARTING = 60 * 20;
    public static final int TIMER_INPROGRESS = 5 * 60 * 20;
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
    public Cutscene startGameTutorial;
    public BukkitTask practiceModeTimer;

    public ServerDodgeball(JavaPlugin plugin) {
        super(plugin);
        serverSpawn = new Location(world, -15.5, 4, 1.5);
        practiceArenaSelect = new Location(world, 0.5, 5, 0.5, 180, 0);
        initArenaSpawns();

        roundRobin = new ArrayList<ArrayList<Integer>>();
        rrIndexToTeamId = new HashMap<Integer, Integer>();

        // Add existing players (for hot-reloading)
        ArrayList<Player> currentlyOnline = new ArrayList<>(Bukkit.getOnlinePlayers());
        for (Player player : currentlyOnline) {
            player.teleport(serverSpawn);
            PlayerUtils.resetPlayer(player, GameMode.ADVENTURE);
            givePracticeModeSelect(player);
        }

        dodgeballPracticeMode();
        dodgeballRoundRobinSetup();

        timerStartGame = new Timer(plugin) {
            @Override
            public void onStart() {
                state = GAME_STARTING;
                dodgeballPreStart();

                if (!displayedWelcomeMessage) {
                    displayedWelcomeMessage = true;
                    sendTitleAll("Good luck, have fun!", "Game begins in 60 seconds!");
                    sendMultipleMessageAll(new String[]{
                            ChatColor.GREEN + "" + ChatColor.BOLD + "Here's a recap:\n" + ChatColor.RESET +
                                    "This map is " + ChatColor.BOLD + "SG Dodgeball" + ChatColor.RESET + ", by SkyGames Team\n" +
                                    " \n" +
                                    " \n",
                            ChatColor.GREEN + "" + ChatColor.BOLD + "How to play:\n" + ChatColor.RESET +
                                    "1. You have three lives\n" +
                                    "2. Arrows will spawn on the beacon every 20 seconds\n" +
                                    "3. Kill the other team, each kill is worth +50 points!\n" +
                                    "4. Eliminating all 3 lives of every opposing team's players awards everyone on your team +250 points each!"
                    }, new int[]{
                            120,
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

        ArrayList<CutsceneStep> steps = new ArrayList<>();
        int time = 0;
        steps.add(new CutsceneStep(time)
                .pos(10000, 21, 24, -90, 40)
                .title("Welcome to Dodgeball", "Map made by SkyGames Team", 60)
                .freeze(50));
        steps.add(new CutsceneStep(time += 70)
                .pos(10015.75, 3.5, 6.5, -180, 8)
                .comment("To Spawn Door")
                .linear());
        steps.add(new CutsceneStep(time += 30)
                .pos(10014.5, 6.5, 0.25, 0, 52)
                .title("This is your spawn", "You will start with three lives", 60)
                .linear()
                .freeze(50));
        steps.add(new CutsceneStep(time += 60)
                .pos(10014.5, 3.5, 6.5, -180, 8)
                .comment("To Spawn Door")
                .linear());
        steps.add(new CutsceneStep(time += 30)
                .pos(10011, 5.3, 10.5, -140, 20)
                .title("You will have invincibility", "until you leave the spawn door", 60)
                .linear()
                .freeze(50));
        steps.add(new CutsceneStep(time += 65)
                .pos(10017, 4, 17.3, 126, 29.5f)
                .title("Arrows will spawn here", "every 20 seconds", 60)
                .linear()
                .freeze(50));
        steps.add(new CutsceneStep(time += 75)
                .pos(10014.5, 4.5, 4, 0, 0)
                .title("Each kill is worth 50 points", "Winning the game will award each team member 250 bonus points!", 80)
                .linear());

        startGameTutorial = new Cutscene(plugin, this, steps) {
            @Override
            public void onStart() {
                for (PlayerInterface p : players) {
                    p.bukkitPlayer.setGameMode(GameMode.SPECTATOR);
                }
            }

            @Override
            public void onEnd() {
                timerStartGame.start();
                for (PlayerInterface player : players) {
                    PlayerUtils.resetPlayer(player.bukkitPlayer, GameMode.ADVENTURE);
                    player.bukkitPlayer.teleport(serverSpawn);
                    player.bukkitPlayer.setBedSpawnLocation(serverSpawn, true);
                }
            }
        };
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
        plugin.getCommand("start").setExecutor(new CommandCutsceneStart(startGameTutorial));
        plugin.getCommand("timerstartset").setExecutor(new CommandTimerSet(timerStartGame));
        plugin.getCommand("timerstartpause").setExecutor(new CommandTimerPause(timerStartGame));
        plugin.getCommand("timerstartresume").setExecutor(new CommandTimerResume(timerStartGame));
        plugin.getCommand("timergameset").setExecutor(new CommandTimerSet(timerInProgress));
        plugin.getCommand("timergamepause").setExecutor(new CommandTimerPause(timerInProgress));
        plugin.getCommand("timergameresume").setExecutor(new CommandTimerResume(timerInProgress));
        plugin.getCommand("timerfinishedset").setExecutor(new CommandTimerSet(timerFinished));
        plugin.getCommand("timerfinishedpause").setExecutor(new CommandTimerPause(timerFinished));
        plugin.getCommand("timerfinishedresume").setExecutor(new CommandTimerResume(timerFinished));
    }

    @Override
    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(new ListenerDodgeball(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DisableHunger(), plugin);
    }

    @Override
    public void applyGameRules(World world) {
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.setGameRule(GameRule.MOB_GRIEFING, false);
        world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setFullTime(18000);

        // Arena 1
        for (int x = 624; x <= 625 + 2; ++x)
            for (int z = -1; z <= 3; ++z)
                world.setChunkForceLoaded(x, z, true);
        // Arena 2
        for (int x = 1249; x <= 1250 + 2; ++x)
            for (int z = -1; z <= 3; ++z)
                world.setChunkForceLoaded(x, z, true);
        // Arena 3
        for (int x = 1874; x <= 1875 + 2; ++x)
            for (int z = -1; z <= 3; ++z)
                world.setChunkForceLoaded(x, z, true);
        // Arena 4
        for (int x = 2499; x <= 1875 + 2; ++x)
            for (int z = -1; z <= 3; ++z)
                world.setChunkForceLoaded(x, z, true);
    }

    @Override
    public PlayerInterface createNewPlayerInterface(UUID uuid, String name) {
        return new PlayerDodgeball(this, uuid, name);
    }

    /* GAME LOGIC */

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
                    MCGTeam team1 = getTeamFromTeamID(rrIndexToTeamId.get(i));
                    MCGTeam team2 = getTeamFromTeamID(rrIndexToTeamId.get(j));

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
        // Clear Practice mode variables
        Arrays.fill(practiceArenaNum, 0);

        // Remove all lingering item drops
        List<Entity> entList = world.getEntities();
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
                    MCGTeam team1 = getTeamFromTeamID(rrIndexToTeamId.get(i));
                    MCGTeam team2 = getTeamFromTeamID(rrIndexToTeamId.get(j));

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
                            Location redSpawn = new Location(world,
                                    redSpawnLocation.getX(), redSpawnLocation.getY(), redSpawnLocation.getZ());
                            player.bukkitPlayer.teleport(redSpawn);
                            player.bukkitPlayer.setBedSpawnLocation(redSpawn, true);
                        }
                        // Send team 2 to RED spawn
                        if (player.myTeam.id == team2.id) {
                            playerDodgeball.opponentTeam = team1;
                            playerDodgeball.arena = arenaNo;
                            Location blueSpawn = new Location(world,
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

        if (timerInProgress.get() > 12 * 20) {
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
            ((PlayerDodgeball) player).arena = -1;
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
        for (PlayerDodgeball player : playerDodgeballs) {
            topPlayers += ChatColor.RESET + "[" + player.currentScore + "] " + player.bukkitPlayer.getDisplayName() + ChatColor.RESET + "\n";
            if (++count > 5) {
                break;
            }
        }

        String topKillers = "";
        count = 0;
        playerDodgeballs.sort(Comparator.comparingInt(o -> -o.totalKills));
        for (PlayerDodgeball player : playerDodgeballs) {
            topKillers += ChatColor.RESET + "" + player.totalKills + " kills - " + player.bukkitPlayer.getDisplayName() + ChatColor.RESET + "\n";
            if (++count > 5) {
                break;
            }
        }

        String topTeams = "";
        ArrayList<MCGTeam> teamDodgeballs = getRealTeams();
        teamDodgeballs.sort(Comparator.comparingInt(o -> -o.getScore("dodgeball")));
        for (MCGTeam team : teamDodgeballs) {
            topTeams += ChatColor.RESET + "[" + team.getScore("dodgeball") + "] " + team.chatColor + "" + team.teamname + "\n";
        }

        sendMultipleMessageAll(new String[]{
                ChatColor.BOLD + "Top Players:\n" + topPlayers +
                        " \n",
                ChatColor.BOLD + "Top Killers:\n" + topKillers +
                        " \n",
                ChatColor.BOLD + "Final Team Score for Dodgeball:\n" + topTeams,
        }, new int[]{
                10,
                60,
                60,
        });

    }

    /* SUPPORTING LOGIC */

    public void giveBow(PlayerDodgeball player) {
        ItemStack bow = new ItemStack(Material.BOW, 1);
        ItemMeta bowMeta = bow.getItemMeta();
        bowMeta.addEnchant(Enchantment.ARROW_DAMAGE, 32767, true);
        bowMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        bowMeta.setUnbreakable(true);
        bow.setItemMeta(bowMeta);
        player.bukkitPlayer.getInventory().addItem(bow);
    }

    public void givePracticeModeSelect(Player player) {
        ItemStack levelSelect = new ItemStack(Material.TARGET, 1);
        ItemMeta meta = levelSelect.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Arena select");
        levelSelect.setItemMeta(meta);
        player.getInventory().setItem(7, levelSelect);
    }

    public int practiceArenaNum[]= new int[] {0,0,0,0,0};
    public int practiceArrowTick = 0;
    public void dodgeballPracticeMode() {
        practiceModeTimer = new BukkitRunnable() {
            @Override
            public void run() {
                if (state == GAME_WAITING) {
                    // Spawn arrows
                    practiceArrowTick--;
                    if (practiceArrowTick <= 0) {
                        practiceArrowTick = SPAWN_ARROWS_TICK;
                        if (practiceArenaNum[1] > 0) {
                            Location arrowSpawn0 = new Location(world, arenaArrowSpawns.get(0).getX(), arenaArrowSpawns.get(0).getY(), arenaArrowSpawns.get(0).getZ());
                            Location arrowSpawn1 = new Location(world, arenaArrowSpawns.get(1).getX(), arenaArrowSpawns.get(1).getY(), arenaArrowSpawns.get(1).getZ());
                            ItemStack arrow0 = new ItemStack(Material.ARROW, 1);
                            ItemStack arrow1 = new ItemStack(Material.ARROW, 1);
                            world.dropItem(arrowSpawn0, arrow0);
                            world.dropItem(arrowSpawn1, arrow1);
                            MCGMain.logger.info("Practice Mode: Spawned arrow in arena 1");
                        }
                        if (practiceArenaNum[2] > 0) {
                            Location arrowSpawn2 = new Location(world, arenaArrowSpawns.get(2).getX(), arenaArrowSpawns.get(2).getY(), arenaArrowSpawns.get(2).getZ());
                            Location arrowSpawn3 = new Location(world, arenaArrowSpawns.get(3).getX(), arenaArrowSpawns.get(3).getY(), arenaArrowSpawns.get(3).getZ());
                            ItemStack arrow2 = new ItemStack(Material.ARROW, 1);
                            ItemStack arrow3 = new ItemStack(Material.ARROW, 1);
                            world.dropItem(arrowSpawn2, arrow2);
                            world.dropItem(arrowSpawn3, arrow3);
                            MCGMain.logger.info("Practice Mode: Spawned arrow in arena 2");
                        }
                        if (practiceArenaNum[3] > 0) {
                            Location arrowSpawn4 = new Location(world, arenaArrowSpawns.get(4).getX(), arenaArrowSpawns.get(4).getY(), arenaArrowSpawns.get(4).getZ());
                            Location arrowSpawn5 = new Location(world, arenaArrowSpawns.get(5).getX(), arenaArrowSpawns.get(5).getY(), arenaArrowSpawns.get(5).getZ());
                            ItemStack arrow4 = new ItemStack(Material.ARROW, 1);
                            ItemStack arrow5 = new ItemStack(Material.ARROW, 1);
                            world.dropItem(arrowSpawn4, arrow4);
                            world.dropItem(arrowSpawn5, arrow5);
                            MCGMain.logger.info("Practice Mode: Spawned arrow in arena 3");
                        }
                        if (practiceArenaNum[4] > 0) {
                            Location arrowSpawn6 = new Location(world, arenaArrowSpawns.get(6).getX(), arenaArrowSpawns.get(6).getY(), arenaArrowSpawns.get(6).getZ());
                            Location arrowSpawn7 = new Location(world, arenaArrowSpawns.get(7).getX(), arenaArrowSpawns.get(7).getY(), arenaArrowSpawns.get(7).getZ());
                            ItemStack arrow6 = new ItemStack(Material.ARROW, 1);
                            ItemStack arrow7 = new ItemStack(Material.ARROW, 1);
                            world.dropItem(arrowSpawn6, arrow6);
                            world.dropItem(arrowSpawn7, arrow7);
                            MCGMain.logger.info("Practice Mode: Spawned arrow in arena 4");
                        }
                    }

                    // Change arena label
                    Sign arena1Sign = (Sign) world.getBlockAt(3, 7, -1).getState();
                    arena1Sign.setLine(2, practiceArenaNum[1] + " Players");
                    arena1Sign.update();
                    Sign arena2Sign = (Sign) world.getBlockAt(3, 7, 1).getState();
                    arena2Sign.setLine(2, practiceArenaNum[2] + " Players");
                    arena2Sign.update();
                    Sign arena3Sign = (Sign) world.getBlockAt(-3, 7, 1).getState();
                    arena3Sign.setLine(2, practiceArenaNum[3] + " Players");
                    arena3Sign.update();
                    Sign arena4Sign = (Sign) world.getBlockAt(-3, 7, -1).getState();
                    arena4Sign.setLine(2, practiceArenaNum[4] + " Players");
                    arena4Sign.update();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    /* MAP LOGIC */

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
