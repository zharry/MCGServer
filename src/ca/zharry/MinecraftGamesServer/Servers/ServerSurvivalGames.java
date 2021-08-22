package ca.zharry.MinecraftGamesServer.Servers;

import ca.zharry.MinecraftGamesServer.Commands.*;
import ca.zharry.MinecraftGamesServer.Listeners.ListenerSurvivalGames;
import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Players.PlayerSurvivalGames;
import ca.zharry.MinecraftGamesServer.Timer.Cutscene;
import ca.zharry.MinecraftGamesServer.Timer.CutsceneStep;
import ca.zharry.MinecraftGamesServer.Timer.Timer;
import ca.zharry.MinecraftGamesServer.Utils.Coord3D;
import ca.zharry.MinecraftGamesServer.Utils.Point3D;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.*;

public class ServerSurvivalGames extends ServerInterface<PlayerSurvivalGames> {

    // Ingame variables
    public int stage = 0;

    // Game config
    public static final int COMPETITION_MAX_HEIGHT = 170;
    public static final int TIMER_STARTING = 30 * 20;
    public static final int TIMER_BEGIN = 10 * 20;
    public static final int TIMER_INPROGRESS = 25 * 60 * 20;
    public static final int TIMER_FINISHED = 45 * 20;
    public static final ArrayList<Integer> stageTimes = new ArrayList<Integer>(Arrays.asList(
            11 * 60 * 20,
            2 * 60 * 20,
            7 * 60 * 20,
            5 * 60 * 20,
            5 * 60 * 20
    ));
    public static final ArrayList<String> stages = new ArrayList<String>(Arrays.asList(
            "Game Start",
            "Border to 200",
            "Chest refill",
            "Border to 50",
            "Deathmatch",
            "Game End"
    ));

    public static class SpecialItem {
        public String location;
        public ItemStack item;

        public SpecialItem(String location, ItemStack item) {
            this.location = location;
            this.item = item;
        }
    }

    public static final HashMap<Coord3D, SpecialItem> specialChests = new HashMap<>();

    public static final ArrayList<Point3D> spawnpoints = new ArrayList<Point3D>();
    public static final HashSet<Integer> filledSpawnpoints = new HashSet<Integer>();
    public static final HashMap<Coord3D, Double> chests = new HashMap<Coord3D, Double>();
    public static final TreeMap<Double, EnchantChoice> enchantTable = new TreeMap<>();
    public static final TreeMap<Double, EnchantChoice> enchantTableIntegrated = new TreeMap<>();
    public static final TreeMap<Double, ItemChoice> lootTable = new TreeMap<>();
    public static final TreeMap<Double, ItemChoice> lootTableIntegrated = new TreeMap<>();
    public static final TreeMap<Double, ItemChoice> lootTableTier2 = new TreeMap<>();
    public static final TreeMap<Double, ItemChoice> lootTableTier2Integrated = new TreeMap<>();
    public static final HashSet<Coord3D> openedChests = new HashSet<>();
    public static final Random random = new Random();

    // Server state
    public static final int ERROR = -1;
    public static final int GAME_WAITING = 0;
    public static final int GAME_STARTING = 1;
    public static final int GAME_BEGIN = 4;
    public static final int GAME_INPROGRESS = 2;
    public static final int GAME_FINISHED = 3;
    public int state = ERROR;

    // Server tasks
    public Timer timerStartGame;
    public Timer timerBegin;
    public Timer timerInProgress;
    public Timer timerFinished;
    public Cutscene startGameTutorial;

    public ServerSurvivalGames(JavaPlugin plugin, World world, String minigame) {
        super(plugin, world, minigame);
        serverSpawn = new Location(world, 0.5, 176, 0.5);

        initCornucopiaSpawns();
        initChestLocations();
        initLootTables();

        resetWorldBorder();

        // Add existing players (for hot-reloading)
        for (PlayerSurvivalGames player : players) {
            player.reset(GameMode.SURVIVAL);
            player.teleport(serverSpawn);
        }

        timerStartGame = new Timer(plugin, "start", TIMER_STARTING) {
            @Override
            public void onStart() {
                state = GAME_STARTING;
                survivalGamesPreStart();

                sendTitleAll("Good luck, have fun!", "Game will begin in 30 seconds!");
                sendMultipleMessageAll(new String[]{
                        ChatColor.GREEN + "" + ChatColor.BOLD + "Here's a recap:\n" + ChatColor.RESET +
                                "This map is " + ChatColor.BOLD + "Breeze Island 2" + ChatColor.RESET + ", by xBayani\n" +
                                " \n" +
                                " \n",
                        ChatColor.GREEN + "" + ChatColor.BOLD + "How to play:\n" + ChatColor.RESET +
                                "1. Collect loot from chests around the map\n" +
                                "2. Kill other players (+350pts)\n" +
                                "3. Survive longer than the other players (+125pts)\n" +
                                "4. Watch out for server events (border shrink, chest refills, and etc...)",
                }, new int[]{
                        10,
                        45,
                });
            }

            @Override
            public void onTick() {
                countdownTimer(this, 0,
                        "Teleporting to arena...",
                        "",
                        "",
                        "Teleporting to arena...");
            }

            @Override
            public void onEnd() {
                timerBegin.start();
            }
        };

        timerBegin = new Timer(plugin, "begin", TIMER_BEGIN) {
            @Override
            public void onStart() {
                state = GAME_BEGIN;
                survivalGamesBegin();
            }

            @Override
            public void onTick() {
                countdownTimer(this, 11,
                        "",
                        "",
                        "",
                        "Begin!");
            }

            @Override
            public void onEnd() {
                timerInProgress.start();
            }
        };

        timerInProgress = new Timer(plugin, "game", TIMER_INPROGRESS) {
            @Override
            public void onStart() {
                state = GAME_INPROGRESS;
                survivalGamesStageStart();
            }

            @Override
            public void onTick() {
                survivalGamesTick();
            }

            @Override
            public void onEnd() {
                survivalGamesStageEnd();
            }
        };

        timerFinished = new Timer(plugin, "finished", TIMER_FINISHED) {
            @Override
            public void onStart() {
                state = GAME_FINISHED;
                commitAllPlayers();
            }

            @Override
            public void onTick() {
            }

            @Override
            public void onEnd() {
                sendTitleAll("Joining Lobby...", "", 5, 20, 30);
                MCGMain.bungeeManager.sendAllPlayers(MCGMain.lobbyId, false, true);

                state = GAME_WAITING;
                stage = 0;
                timerStartGame.set(TIMER_STARTING);
                timerBegin.set(TIMER_BEGIN);
                timerInProgress.set(TIMER_INPROGRESS);
                timerFinished.set(TIMER_FINISHED);
            }
        };

        ArrayList<CutsceneStep> steps = new ArrayList<>();
        int time = 0;
        steps.add(new CutsceneStep(time)
                .pos(-20, 71, 12, -120, 25)
                .title("Welcome to Survival Games", "Map made by xBayani", 60)
                .freeze(50));


        //Intro
        steps.add(new CutsceneStep(time += 100)
                .pos(-6.5, 69.5, 0.5, -90, 10)
                .title("Collect loot from chests", "There is an additional chest refill at 13 mins!", 60)
                .linear()
                .freeze(50));
        steps.add(new CutsceneStep(time += 100)
                .pos(13.5, 90, 20, 145, 55)
                .title("Kill all other players", "and gain 350 points per kill!", 60)
                .linear()
                .freeze(50));
        steps.add(new CutsceneStep(time += 100)
                .pos(90, 102, 72, 135, 25)
                .title("You can also earn survival points", "125 points every time another player dies.", 60)
                .linear()
                .freeze(50));
        steps.add(new CutsceneStep(time += 100)
                .pos(60, 105, 212, 135, 12)
                .title("The map is 400 blocks wide", "and shrinks to 200 at 11 mins, and 50 at 20 mins!", 60)
                .linear()
                .freeze(75));
        steps.add(new CutsceneStep(time+=120)
                .pos(0.5, 125, 0, 0, 90)
                .title("", "", 60)
                .linear());
        steps.add(new CutsceneStep(time += 50)
                .pos(0.5, 176, 0.5, 0, 0)
                .title("Let the games begin", "and may the odds be ever in your favour", 60)
                .linear()
                .freeze(50));

        //Old Cinematics for Rare Items
        /*
        steps.add(new CutsceneStep(time += 100)
                .pos(0, 78, -100, -155, 10)
                .title("There are 4 special items", "which are guaranteed to generate in 4 special chests!", 60)
                .freeze(50));
        steps.add(new CutsceneStep(time += 100)
                .pos(38.5, 125, -156.5, -155, 90)
                .title("The first item is here", "inside the volcano on the north island!", 60)
                .freeze(50)
                .linear());
        steps.add(new CutsceneStep(time += 120)
                .pos(37.5, 38, -157.5, 55, 90)
                .comment("Go to the chest")
                .linear());
        steps.add(new CutsceneStep(time += 20).pos(39.5, 36.5, -159.5, 45, 10)
                .comment("Pan to chest")
                .linear());
        steps.add(new CutsceneStep(time += 30)
                .comment("Open Chest")
                .action((entry) -> {
                    Inventory inventory = ((Chest) world.getBlockAt(37, 37, -158).getState()).getInventory();
                    players.forEach(player -> player.bukkitPlayer.openInventory(inventory));
                }));

        steps.add(new CutsceneStep(time += 100)
                .action((entry) -> players.forEach(player -> player.bukkitPlayer.closeInventory()))
                .pos(13, 100, 7, 25, -16)
                .title("The second item is here", "inside the temple south of spawn", 60)
                .freeze(50));
        steps.add(new CutsceneStep(time += 100)
                .pos(27.5, 80, 51, -30, 35)
                .comment("Go to the chest")
                .linear());
        steps.add(new CutsceneStep(time += 20)
                .pos(27, 80, 53.5, 156, 30)
                .comment("Pan to the chest")
                .linear());
        steps.add(new CutsceneStep(time += 30)
                .comment("Open Chest")
                .action((entry) -> {
                    Inventory inventory = ((Chest) world.getBlockAt(26, 80, 51).getState()).getInventory();
                    players.forEach(player -> player.bukkitPlayer.openInventory(inventory));
                }));

        steps.add(new CutsceneStep(time += 100)
                .action((entry) -> players.forEach(player -> player.bukkitPlayer.closeInventory()))
                .pos(-159, 65, -49, -51, -19)
                .title("The third item is here", "on the top of the pirate ship west of spawn", 60)
                .freeze(50));
        steps.add(new CutsceneStep(time += 100)
                .pos(-136.5, 125, -16, -40, 37)
                .comment("Go to the chest")
                .linear());
        steps.add(new CutsceneStep(time += 30)
                .comment("Open Chest").action((entry) -> {
                    Inventory inventory = ((Chest) world.getBlockAt(-135, 124, -14).getState()).getInventory();
                    players.forEach(player -> player.bukkitPlayer.openInventory(inventory));
                }));

        steps.add(new CutsceneStep(time += 100)
                .action((entry) -> players.forEach(player -> player.bukkitPlayer.closeInventory()))
                .pos(-1.5, 84, -1.5, -180, -10)
                .title("The final item is here", "inside the north waterfall at spawn", 60)
                .freeze(50));
        steps.add(new CutsceneStep(time += 100)
                .pos(-1.5, 84, -31.5, 155, 20)
                .comment("Go to the chest")
                .linear());
        //-3 84 -31
        steps.add(new CutsceneStep(time += 30).comment("Open Chest")
                .action((entry) -> {
                    Inventory inventory = ((Chest) world.getBlockAt(-3, 84, -34).getState()).getInventory();
                    players.forEach(player -> player.bukkitPlayer.openInventory(inventory));
                }));
        steps.add(new CutsceneStep(time += 100).comment("Close chest")
                .action((entry) -> players.forEach(player -> player.bukkitPlayer.closeInventory())));
         */


        startGameTutorial = new Cutscene(plugin, this, steps) {
            @Override
            public void onStart() {
                for (PlayerInterface p : players) {
                    p.setGameMode(GameMode.SPECTATOR);
                }
            }

            @Override
            public void onEnd() {
                timerStartGame.start();
                for (PlayerInterface player : players) {
                    player.reset(GameMode.ADVENTURE);
                    player.teleport(serverSpawn);
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
        plugin.getCommand("start").setExecutor(new CommandStart(this, startGameTutorial));
        plugin.getCommand("timer").setExecutor(new CommandTimer(this, timerStartGame, timerBegin, timerInProgress, timerFinished));
    }

    @Override
    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(new ListenerSurvivalGames(this), plugin);
    }

    @Override
    public void applyGameRules(World world) {
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.setGameRule(GameRule.MOB_GRIEFING, false);
        world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, true);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setFullTime(6000);

        for (int x = -16; x < 16; ++x) {
            for (int z = -16; z < 16; ++z) {
                world.setChunkForceLoaded(x, z, true);
            }
        }
    }

    @Override
    public PlayerSurvivalGames createNewPlayerInterface(UUID uuid, String name) {
        return new PlayerSurvivalGames(this, uuid, name);
    }

    /* GAME LOGIC */

    private void survivalGamesPreStart() {
        fillChestsStage1();
    }

    private void survivalGamesBegin() {
        filledSpawnpoints.clear();

        // Assign Players a unique pad
        int spawnPads = spawnpoints.size();
        for (PlayerSurvivalGames player : players) {
            player.reset(GameMode.SURVIVAL);

            // Get a free spawn slot
            int padNo;
            do {
                padNo = new Random().nextInt(spawnPads);
            }
            while (filledSpawnpoints.contains(padNo));
            filledSpawnpoints.add(padNo);

            // Send player to that slot
            Point3D spawnPad = spawnpoints.get(padNo);
            Location spawnPadLoc = new Location(world, spawnPad.x + 0.5, spawnPad.y, spawnPad.z + 0.5);
            spawnPadLoc.setPitch(0);
            spawnPadLoc.setYaw((float) Math.toDegrees(Math.atan2(spawnPad.x, -spawnPad.z)));
            player.teleport(spawnPadLoc);
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    private void survivalGamesStageStart() {
        timerInProgress.set(stageTimes.get(stage));
        WorldBorder border = world.getWorldBorder();
        border.setCenter(0.5, 0.5);
        switch (stage) {
            case 1:
                border.setSize(200 + 1, 60);
                sendTitleAll("Border is shrinking to 200 blocks", "over the next 60 seconds!");
                break;
            case 2:
                fillChestsStage2();
                sendTitleAll("Chests have been refilled!", "It is now possible to get diamond gear and weapons!");
                break;
            case 3:
                border.setSize(50 + 1, 60);
                sendTitleAll("Border is shrinking to 50 blocks", "over the next 60 seconds!");
                break;
            case 4:
                border.setSize(25, 30);
                sendTitleAll(ChatColor.RED + "Deathmatch!" + ChatColor.RESET, "Border is shrinking to 25 blocks");
                break;
            default:
                break;
        }
        stage++;
    }

    private void survivalGamesTick() {
        int counter = 0;

        // Go through all the teams
        for (int teamID : teamIDs) {
            MCGTeam team = getTeamFromTeamID(teamID);
            // Check if each team still has at least one player alive
            boolean alive = false;
            for (UUID uuid : team.players) {
                PlayerSurvivalGames player = playerLookup.get(uuid);
                if (player != null)
                    if (!player.dead) {
                        alive = true;
                    }
            }
            if (alive) {
                counter++;
            }
        }

        if (counter <= 1) {
            survivalGamesEnd();
        }
    }

    private void survivalGamesStageEnd() {
        if (stage >= stageTimes.size()) {
            survivalGamesEnd();
        } else {
            timerInProgress.start();
        }
    }

    private void survivalGamesEnd() {
        resetWorldBorder();
        timerInProgress.cancel();
        timerFinished.start();

        ArrayList<PlayerSurvivalGames> playerSurvivalGamess = new ArrayList<>();
        for (PlayerSurvivalGames player : players) {
            player.setGameMode(GameMode.SPECTATOR);
            playerSurvivalGamess.add(player);
            if (!player.dead) {
                player.addScore(250, "last one standing");
                player.bukkitPlayer.sendTitle(ChatColor.GREEN + "Last one standing!", "You have received 250 additional points!", 10, 60, 10);
                sendMessageAll(ChatColor.RESET + player.bukkitPlayer.getDisplayName() + " is the last one standing, and has received 250 additional points!");
            } else {
                player.bukkitPlayer.sendTitle(ChatColor.RED + "Game Over!", "", 10, 60, 10);
            }
            player.commit();
        }

        String topPlayers = "";
        int count = 0;
        playerSurvivalGamess.sort(Comparator.comparingInt(o -> -o.getCurrentScore()));
        for (PlayerSurvivalGames player : playerSurvivalGamess) {
            topPlayers += ChatColor.RESET + "[" + player.getCurrentScore() + "] " + player.bukkitPlayer.getDisplayName() + ChatColor.RESET + "\n";
            if (++count > 5) {
                break;
            }
        }

        String topKillers = "";
        count = 0;
        playerSurvivalGamess.sort(Comparator.comparingInt(o -> -o.kills));
        for (PlayerSurvivalGames player : playerSurvivalGamess) {
            topKillers += ChatColor.RESET + "" + player.kills + " kills - " + player.bukkitPlayer.getDisplayName() + ChatColor.RESET + "\n";
            if (++count > 5) {
                break;
            }
        }

        String topTeams = "";
        ArrayList<MCGTeam> teamParkours = getRealTeams();
        teamParkours.sort(Comparator.comparingInt(o -> -o.getScore("survivalgames")));
        for (MCGTeam team : teamParkours) {
            topTeams += ChatColor.RESET + "[" + team.getScore("survivalgames") + "] " + team.chatColor + "" + team.teamname + "\n";
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

    /* SUPPORTING LOGIC */
    public int getPlayersAlive() {
        int counter = 0;
        for (PlayerSurvivalGames player : players) {
            if (!player.dead) {
                counter++;
            }
        }
        return counter;
    }

    public void resetWorldBorder() {
        WorldBorder border = world.getWorldBorder();
        border.setSize(400 + 1);
        border.setCenter(0.5, 0.5);
        border.setDamageBuffer(0);
    }

    public int getWorldBorder() {
        WorldBorder border = world.getWorldBorder();
        int size = (int) border.getSize();
        // Ask Jacky about this one...
        if (size == 401)
            size = 400;
        if (size == 201)
            size = 200;
        if (size == 51)
            size = 50;
        return size;
    }

    public String getNextEvent() {
        if (stage != -1)
            return stages.get(stage);
        return "Please wait...";
    }

    /* MAP LOGIC*/

    private void initCornucopiaSpawns() {
        spawnpoints.add(new Point3D(0, 67, -23));
        spawnpoints.add(new Point3D(6, 67, -22));
        spawnpoints.add(new Point3D(11, 67, -20));
        spawnpoints.add(new Point3D(16, 67, -16));
        spawnpoints.add(new Point3D(20, 67, -11));
        spawnpoints.add(new Point3D(22, 67, -6));
        spawnpoints.add(new Point3D(23, 67, 0));
        spawnpoints.add(new Point3D(22, 67, 6));
        spawnpoints.add(new Point3D(20, 67, 11));
        spawnpoints.add(new Point3D(16, 67, 16));
        spawnpoints.add(new Point3D(11, 67, 20));
        spawnpoints.add(new Point3D(6, 67, 22));
        spawnpoints.add(new Point3D(0, 67, 23));
        spawnpoints.add(new Point3D(-6, 67, 22));
        spawnpoints.add(new Point3D(-11, 67, 20));
        spawnpoints.add(new Point3D(-16, 67, 16));
        spawnpoints.add(new Point3D(-20, 67, 11));
        spawnpoints.add(new Point3D(-22, 67, 6));
        spawnpoints.add(new Point3D(-23, 67, 0));
        spawnpoints.add(new Point3D(-22, 67, -6));
        spawnpoints.add(new Point3D(-20, 67, -11));
        spawnpoints.add(new Point3D(-16, 67, -16));
        spawnpoints.add(new Point3D(-11, 67, -20));
        spawnpoints.add(new Point3D(-6, 67, -22));
    }

    public void initChestLocations() {
        Chunk[] loadedChunks = world.getLoadedChunks();
        for (Chunk chunk : loadedChunks) {
            BlockState[] states = chunk.getTileEntities();
            for (BlockState state : states) {
                if (state.getType() == Material.CHEST) {
                    addChest(new Coord3D(state.getX(), state.getY(), state.getZ()));
                }
            }
        }
    }

    public void addChest(Coord3D p) {
        Block block = world.getBlockAt(p.x, p.y, p.z);
        if (block.getType() == Material.CHEST) {
            int count = 0;
//            if (world.getBlockAt(p.x + 1, p.y, p.z).getType() == Material.AIR) count++;
//            if (world.getBlockAt(p.x - 1, p.y, p.z).getType() == Material.AIR) count++;
//            if (world.getBlockAt(p.x, p.y + 1, p.z).getType() == Material.AIR) count++;
//            if (world.getBlockAt(p.x, p.y - 1, p.z).getType() == Material.AIR) count++;
//            if (world.getBlockAt(p.x, p.y, p.z + 1).getType() == Material.AIR) count++;
//            if (world.getBlockAt(p.x, p.y, p.z - 1).getType() == Material.AIR) count++;
            chests.put(p, (double) count);
            // Each chest has a score
            // Each item has a score
            // When generating items, list out all items with score < chest score
            // Generate items based on the list using item score
        }
    }

    /* LOOT TABLE LOGIC */

    private void initLootTables() {
        ItemStack fireResistance = new ItemStack(Material.POTION, 1);
        PotionMeta potionMeta = (PotionMeta) fireResistance.getItemMeta();
        potionMeta.setBasePotionData(new PotionData(PotionType.FIRE_RESISTANCE, false, false));
        fireResistance.setItemMeta(potionMeta);

        ItemStack instantDamage = new ItemStack(Material.SPLASH_POTION, 1);
        potionMeta = (PotionMeta) fireResistance.getItemMeta();
        potionMeta.setBasePotionData(new PotionData(PotionType.INSTANT_DAMAGE, false, false));
        instantDamage.setItemMeta(potionMeta);

        lootTable.put(3.000010, new ItemChoice(Material.LEATHER_HELMET, 1));
        lootTable.put(3.000011, new ItemChoice(Material.LEATHER_CHESTPLATE, 1));
        lootTable.put(3.000012, new ItemChoice(Material.LEATHER_LEGGINGS, 1));
        lootTable.put(3.000013, new ItemChoice(Material.LEATHER_BOOTS, 1));
        lootTable.put(1.000010, new ItemChoice(Material.CHAINMAIL_HELMET, 1));
        lootTable.put(1.000011, new ItemChoice(Material.CHAINMAIL_CHESTPLATE, 1));
        lootTable.put(1.000012, new ItemChoice(Material.CHAINMAIL_LEGGINGS, 1));
        lootTable.put(1.000013, new ItemChoice(Material.CHAINMAIL_BOOTS, 1));
        lootTable.put(0.500010, new ItemChoice(Material.IRON_HELMET, 1));
        lootTable.put(0.500012, new ItemChoice(Material.IRON_CHESTPLATE, 1));
        lootTable.put(0.500013, new ItemChoice(Material.IRON_LEGGINGS, 1));
        lootTable.put(0.500014, new ItemChoice(Material.IRON_BOOTS, 1));
        lootTable.put(5.000020, new ItemChoice(Material.WOODEN_SWORD, 1));
        lootTable.put(2.500020, new ItemChoice(Material.STONE_SWORD, 1));
        lootTable.put(0.500020, new ItemChoice(Material.IRON_SWORD, 1));
        lootTable.put(3.000021, new ItemChoice(Material.WOODEN_AXE, 1));
        lootTable.put(2.000021, new ItemChoice(Material.STONE_AXE, 1));
        lootTable.put(1.000021, new ItemChoice(Material.IRON_AXE, 1));
        lootTable.put(2.000030, new ItemChoice(Material.ARROW, 5, 2));
        lootTable.put(1.000031, new ItemChoice(Material.FISHING_ROD, 1));
        lootTable.put(1.000032, new ItemChoice(Material.LAVA_BUCKET, 1));
        lootTable.put(1.000033, new ItemChoice(Material.TNT, 1));
        lootTable.put(1.000034, new ItemChoice(Material.CROSSBOW, 1));
        lootTable.put(2.000035, new ItemChoice(Material.BOW, 1));
        lootTable.put(2.500036, new ItemChoice(Material.COBWEB, 2, 0.5));
        lootTable.put(0.500037, new ItemChoice(Material.FLINT_AND_STEEL, 1));
        lootTable.put(3.000040, new ItemChoice(fireResistance, 1, 0));
        lootTable.put(3.000041, new ItemChoice(instantDamage, 1, 0));
        lootTable.put(3.000050, new ItemChoice(Material.CARROT, 4, 1.5));
        lootTable.put(3.000051, new ItemChoice(Material.APPLE, 5, 1));
        lootTable.put(3.000052, new ItemChoice(Material.COOKIE, 4, 1.5));
        lootTable.put(3.000053, new ItemChoice(Material.BREAD, 4, 1.5));
        lootTable.put(3.000054, new ItemChoice(Material.COOKED_CHICKEN, 1));
        lootTable.put(1.500060, new ItemChoice(Material.ENCHANTED_BOOK, 1));
        lootTable.put(1.500061, new ItemChoice(Material.EXPERIENCE_BOTTLE, 3, 1));
        lootTable.put(1.000062, new ItemChoice(Material.ENDER_PEARL, 1.5, 0.5));
        //lootTable.put(2.000060, new ItemChoice(Material.STICK, 1.5, 0.5));
        lootTable.put(0.500060, new ItemChoice(Material.OAK_PLANKS, 6));
        lootTable.put(0.500061, new ItemChoice(Material.IRON_INGOT, 1));
        //lootTable.put(1.250060, new ItemChoice(Material.GOLD_INGOT, 1));

        enchantTable.put(1.001, new EnchantChoice(EnchantmentWrapper.FIRE_ASPECT, 1));
        enchantTable.put(1.002, new EnchantChoice(EnchantmentWrapper.ARROW_FIRE, 1));
        enchantTable.put(1.003, new EnchantChoice(EnchantmentWrapper.KNOCKBACK, 1, 2));
        enchantTable.put(0.504, new EnchantChoice(EnchantmentWrapper.ARROW_KNOCKBACK, 1, 2));
        enchantTable.put(0.705, new EnchantChoice(EnchantmentWrapper.MULTISHOT, 1));
        enchantTable.put(0.706, new EnchantChoice(EnchantmentWrapper.PIERCING, 1, 3));
        enchantTable.put(0.507, new EnchantChoice(EnchantmentWrapper.ARROW_DAMAGE, 1));
        enchantTable.put(1.208, new EnchantChoice(EnchantmentWrapper.PROTECTION_ENVIRONMENTAL, 1, 2));
        enchantTable.put(1.009, new EnchantChoice(EnchantmentWrapper.DAMAGE_ALL, 1));

        lootTableTier2.put(3.000010, new ItemChoice(Material.GOLDEN_HELMET, 1));
        lootTableTier2.put(3.000011, new ItemChoice(Material.GOLDEN_CHESTPLATE, 1));
        lootTableTier2.put(3.000012, new ItemChoice(Material.GOLDEN_LEGGINGS, 1));
        lootTableTier2.put(3.000013, new ItemChoice(Material.GOLDEN_BOOTS, 1));
        lootTableTier2.put(1.000010, new ItemChoice(Material.IRON_HELMET, 1));
        lootTableTier2.put(1.000011, new ItemChoice(Material.IRON_CHESTPLATE, 1));
        lootTableTier2.put(1.000012, new ItemChoice(Material.IRON_LEGGINGS, 1));
        lootTableTier2.put(1.000013, new ItemChoice(Material.IRON_BOOTS, 1));
        lootTableTier2.put(0.500010, new ItemChoice(Material.DIAMOND_HELMET, 1));
        lootTableTier2.put(0.500012, new ItemChoice(Material.DIAMOND_CHESTPLATE, 1));
        lootTableTier2.put(0.500013, new ItemChoice(Material.DIAMOND_LEGGINGS, 1));
        lootTableTier2.put(0.500014, new ItemChoice(Material.DIAMOND_BOOTS, 1));
        lootTableTier2.put(5.000020, new ItemChoice(Material.GOLDEN_SWORD, 1));
        lootTableTier2.put(2.500020, new ItemChoice(Material.IRON_SWORD, 1));
        lootTableTier2.put(0.500020, new ItemChoice(Material.DIAMOND_SWORD, 1));
        lootTableTier2.put(3.000021, new ItemChoice(Material.GOLDEN_AXE, 1));
        lootTableTier2.put(2.000021, new ItemChoice(Material.IRON_AXE, 1));
        lootTableTier2.put(1.000021, new ItemChoice(Material.DIAMOND_AXE, 1));
        lootTableTier2.put(3.000030, new ItemChoice(Material.ARROW, 8, 2));
        lootTableTier2.put(1.000031, new ItemChoice(Material.FISHING_ROD, 1));
        lootTableTier2.put(1.250032, new ItemChoice(Material.LAVA_BUCKET, 1));
        lootTableTier2.put(1.000033, new ItemChoice(Material.TNT, 1));
        lootTableTier2.put(2.000034, new ItemChoice(Material.CROSSBOW, 1));
        lootTableTier2.put(2.000035, new ItemChoice(Material.BOW, 1));
        lootTableTier2.put(2.500036, new ItemChoice(Material.COBWEB, 3, 0.5));
        lootTableTier2.put(1.500037, new ItemChoice(Material.FLINT_AND_STEEL, 1));
        lootTableTier2.put(3.000040, new ItemChoice(fireResistance, 1, 0));
        lootTableTier2.put(3.000041, new ItemChoice(instantDamage, 1, 0));
        lootTableTier2.put(3.000050, new ItemChoice(Material.APPLE, 6, 1));
        lootTableTier2.put(3.000052, new ItemChoice(Material.COOKIE, 5, 1.5));
        lootTableTier2.put(3.000053, new ItemChoice(Material.BREAD, 5, 1.5));
        lootTableTier2.put(3.000054, new ItemChoice(Material.COOKED_CHICKEN, 3, 0.5));
        lootTableTier2.put(2.000053, new ItemChoice(Material.COOKED_BEEF, 1));
        lootTableTier2.put(1.500060, new ItemChoice(Material.ENCHANTED_BOOK, 1));
        lootTableTier2.put(3.000061, new ItemChoice(Material.EXPERIENCE_BOTTLE, 3, 1));
        //lootTableTier2.put(2.000060, new ItemChoice(Material.STICK, 1.5, 0.5));
        lootTableTier2.put(1.000060, new ItemChoice(Material.OAK_PLANKS, 6));
        lootTableTier2.put(1.000061, new ItemChoice(Material.IRON_INGOT, 1));
        //lootTableTier2.put(0.500060, new ItemChoice(Material.DIAMOND, 1));

        double sum = 0;
        for (Map.Entry<Double, ItemChoice> entry : lootTable.entrySet()) {
            sum += entry.getKey();
            lootTableIntegrated.put(sum, entry.getValue());
        }

        sum = 0;
        for (Map.Entry<Double, ItemChoice> entry : lootTableTier2.entrySet()) {
            sum += entry.getKey();
            lootTableTier2Integrated.put(sum, entry.getValue());
        }

        sum = 0;
        for (Map.Entry<Double, EnchantChoice> entry : enchantTable.entrySet()) {
            sum += entry.getKey();
            enchantTableIntegrated.put(sum, entry.getValue());
        }
    }

    private void fillChestWithLootTableIntegrated(World world, TreeMap<Double, ItemChoice> lootTableInt) {
        openedChests.clear();
        world.getPlayers().forEach(player -> {
            // Prevent spectators from just having chests open to view new chest content
            if (player.getGameMode() == GameMode.SPECTATOR) {
                player.closeInventory();
            }
            if (player.getGameMode() == GameMode.SURVIVAL) {
                if (player.getOpenInventory().getType() == InventoryType.CHEST) {
                    try {
                        openedChests.add(new Coord3D(player.getOpenInventory().getTopInventory().getLocation()));
                    } catch (Exception e) {
                    }
                }
            }
        });
        for (Map.Entry<Coord3D, Double> entry : chests.entrySet()) {
            Coord3D p = entry.getKey();
            Block block = world.getBlockAt(p.x, p.y, p.z);
            if (block.getType() == Material.CHEST) {
                Inventory inv = ((Chest) block.getState()).getBlockInventory();
                inv.clear();
                double items = 4;
                double std = 0.5;
//                double skew = Math.sqrt(p.x * p.x + p.z * p.z) / 25.0 + 1;
                double skew = 1;

                int toGenerate = (int) Math.round(random.nextGaussian() * std + items);
                for (int i = 0; i < toGenerate; ++i) {
                    double max = lootTableInt.lastKey();
                    double uniform = random.nextDouble();
                    double skewed = Math.pow(uniform, skew);
                    // double skewed = -Math.log(1 - uniform * (1 - Math.exp(-skew))) / skew;
                    double choice = skewed * max;
                    ItemChoice itemChoice = lootTableInt.ceilingEntry(choice).getValue();

                    ItemStack newItem = itemChoice.item.clone();

                    if (newItem.getType() == Material.ENCHANTED_BOOK) {
                        double enchantChoiceVal = random.nextDouble() * enchantTableIntegrated.lastKey();
                        EnchantChoice enchantChoice = enchantTableIntegrated.ceilingEntry(enchantChoiceVal).getValue();
                        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) newItem.getItemMeta();
                        meta.addStoredEnchant(enchantChoice.enchant, random.nextInt(enchantChoice.levelMax + 1 - enchantChoice.levelMin) + enchantChoice.levelMin, false);
                        newItem.setItemMeta(meta);
                    }
                    int stackSize = (int) Math.round(itemChoice.stackAvg + random.nextGaussian() * itemChoice.stackStdev);
                    if (stackSize <= 0) {
                        continue;
                    } else if (stackSize > 64) {
                        stackSize = 64;
                    }

                    newItem.setAmount(stackSize);
                    placeItemInChest(world, p, newItem);
                }
            }
        }
    }

    private void placeItemInChest(World world, Coord3D p, ItemStack item) {
        Block block = world.getBlockAt(p.x, p.y, p.z);
        if (block.getType() == Material.CHEST) {
            Inventory inv = ((Chest) block.getState()).getBlockInventory();
            for (int tries = 0; tries < 100; ++tries) {
                int slot = random.nextInt(27);
                if (inv.getItem(slot) == null) {
                    inv.setItem(slot, item);
                    break;
                }
            }
        } else {
            System.err.println("Block at " + p + " is not a chest, cannot place " + item + " inside");
        }
    }

    private void fillChestsStage1() {
        fillChestWithLootTableIntegrated(world, lootTableIntegrated);

        ItemStack rebornGod = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
        ItemMeta rebornGodMeta = rebornGod.getItemMeta();
        rebornGodMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Reborn God" + ChatColor.RESET);
        rebornGodMeta.addEnchant(EnchantmentWrapper.VANISHING_CURSE, 1, false);
        rebornGodMeta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH, new AttributeModifier(UUID.randomUUID(), "generic.max_health", 0.4, AttributeModifier.Operation.ADD_SCALAR, EquipmentSlot.OFF_HAND));
        rebornGodMeta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier(UUID.randomUUID(), "generic.movement_speed", 0.1, AttributeModifier.Operation.ADD_SCALAR, EquipmentSlot.OFF_HAND));
        rebornGod.setItemMeta(rebornGodMeta);

        ItemStack guardiansScaledVest = new ItemStack(Material.IRON_CHESTPLATE, 1);
        ItemMeta guardiansScaledVestMeta = guardiansScaledVest.getItemMeta();
        guardiansScaledVestMeta.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "Guardian's Scaled Vest" + ChatColor.RESET);
        guardiansScaledVestMeta.addEnchant(EnchantmentWrapper.PROTECTION_ENVIRONMENTAL, 2, false);
        guardiansScaledVestMeta.addEnchant(EnchantmentWrapper.THORNS, 1, false);
        guardiansScaledVestMeta.addEnchant(EnchantmentWrapper.VANISHING_CURSE, 1, false);
        guardiansScaledVestMeta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(UUID.randomUUID(), "generic.armor", 6, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.CHEST));
        guardiansScaledVestMeta.addAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE, new AttributeModifier(UUID.randomUUID(), "generic.knockback_resistance", 1.0, AttributeModifier.Operation.ADD_SCALAR, EquipmentSlot.CHEST));
        guardiansScaledVestMeta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier(UUID.randomUUID(), "generic.movement_speed", -0.2, AttributeModifier.Operation.ADD_SCALAR, EquipmentSlot.CHEST));
        guardiansScaledVest.setItemMeta(guardiansScaledVestMeta);

        ItemStack solitudesScimitar = new ItemStack(Material.IRON_SWORD, 1);
        ItemMeta solitudesScimitarMeta = solitudesScimitar.getItemMeta();
        solitudesScimitarMeta.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "Solitude's Scimitar" + ChatColor.RESET);
        solitudesScimitarMeta.addEnchant(EnchantmentWrapper.DAMAGE_ALL, 2, false);
        solitudesScimitarMeta.addEnchant(EnchantmentWrapper.VANISHING_CURSE, 1, false);
        solitudesScimitarMeta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(UUID.randomUUID(), "generic.attack_damage", 6, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
        solitudesScimitarMeta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier(UUID.randomUUID(), "generic.movement_speed", 0.3, AttributeModifier.Operation.ADD_SCALAR, EquipmentSlot.HAND));
        solitudesScimitarMeta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, new AttributeModifier(UUID.randomUUID(), "generic.attack_speed", 1, AttributeModifier.Operation.ADD_SCALAR, EquipmentSlot.HAND));
        solitudesScimitar.setItemMeta(solitudesScimitarMeta);

        ItemStack assassinsWindbreaker = new ItemStack(Material.BOW, 1);
        ItemMeta assassinsWindbreakerMeta = assassinsWindbreaker.getItemMeta();
        assassinsWindbreakerMeta.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "Assassin's Windbreaker" + ChatColor.RESET);
        assassinsWindbreakerMeta.addEnchant(EnchantmentWrapper.ARROW_DAMAGE, 1, false);
        assassinsWindbreakerMeta.addEnchant(EnchantmentWrapper.ARROW_KNOCKBACK, 1, false);
        assassinsWindbreakerMeta.addEnchant(EnchantmentWrapper.ARROW_INFINITE, 1, false);
        assassinsWindbreakerMeta.addEnchant(EnchantmentWrapper.VANISHING_CURSE, 1, false);
        assassinsWindbreakerMeta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier(UUID.randomUUID(), "generic.movement_speed", 0.2, AttributeModifier.Operation.ADD_SCALAR, EquipmentSlot.HAND));
        assassinsWindbreaker.setItemMeta(assassinsWindbreakerMeta);

        /*
        placeItemInChest(world, new Coord3D(37, 37, -158), rebornGod); // Legendary Chest, under the volcano, north of spawn
        specialChests.put(new Coord3D(37, 37, -158), new SpecialItem("under the volcano", rebornGod));

        placeItemInChest(world, new Coord3D(-135, 124, -14), solitudesScimitar); // Special Chest, on top of big pirate ship, west of spawn
        specialChests.put(new Coord3D(-135, 124, -14), new SpecialItem("on top of the pirate ship", solitudesScimitar));

        placeItemInChest(world, new Coord3D(26, 80, 51), assassinsWindbreaker); // Special Chest, inside the cave, south east of spawn
        specialChests.put(new Coord3D(26, 80, 51), new SpecialItem("inside a cave", assassinsWindbreaker));

        placeItemInChest(world, new Coord3D(-3, 84, -34), guardiansScaledVest); // Special Chest, at spawn, waterfall, north of spawn
        specialChests.put(new Coord3D(-3, 84, -34), new SpecialItem("inside the spawn waterfall", guardiansScaledVest));
         */
    }

    private void fillChestsStage2() {
        fillChestWithLootTableIntegrated(world, lootTableTier2Integrated);
        specialChests.clear();
    }

    /* SUPPORTING CLASSES */

    public static class ItemChoice {
        public ItemStack item;
        public double stackAvg;
        public double stackStdev;

        public ItemChoice(ItemStack item, double stackAvg, double stackStdev) {
            this.item = item;
            this.stackAvg = stackAvg;
            this.stackStdev = stackStdev;
        }

        public ItemChoice(Material item, double stackAvg, double stackStdev) {
            this(new ItemStack(item, 1), stackAvg, stackStdev);
        }

        public ItemChoice(Material item, double count) {
            this(item, count, 0);
        }
    }

    public static class EnchantChoice {
        public Enchantment enchant;
        public int levelMin;
        public int levelMax;

        public EnchantChoice(Enchantment enchant, int levelMin, int levelMax) {
            this.enchant = enchant;
            this.levelMin = levelMin;
            this.levelMax = levelMax;
        }

        public EnchantChoice(Enchantment enchant, int level) {
            this(enchant, level, level);
        }
    }

}
