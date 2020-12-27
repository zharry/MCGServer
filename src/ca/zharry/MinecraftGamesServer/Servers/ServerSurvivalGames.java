package ca.zharry.MinecraftGamesServer.Servers;

import ca.zharry.MinecraftGamesServer.Commands.CommandTimerPause;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerResume;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerSet;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerStart;
import ca.zharry.MinecraftGamesServer.Listeners.ListenerSurvivalGames;
import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Players.PlayerSurvivalGames;
import ca.zharry.MinecraftGamesServer.Timer.Timer;
import ca.zharry.MinecraftGamesServer.Utils.Coord3D;
import ca.zharry.MinecraftGamesServer.Utils.PlayerUtils;
import ca.zharry.MinecraftGamesServer.Utils.Point3D;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.entity.Player;
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

public class ServerSurvivalGames extends ServerInterface {

    // Ingame variables
    public int stage = 0;

    // Game config
    public static final int COMPETITION_MAX_HEIGHT = 170;
    public static final int TIMER_STARTING = 60 * 20;
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

    public ServerSurvivalGames(JavaPlugin plugin) {
        super(plugin);

        initCornucopiaSpawns();
        initChestLocations();
        initLootTables();

        WorldBorder border = javaPlugin.getServer().getWorld("world").getWorldBorder();
        border.setSize(500 + 1);
        border.setCenter(0.5, 0.5);
        border.setDamageBuffer(0);

        // Add existing players (for hot-reloading)
        ArrayList<Player> currentlyOnline = new ArrayList<>(Bukkit.getOnlinePlayers());
        for (Player player : currentlyOnline) {
            addPlayer(new PlayerSurvivalGames(player, this));
            PlayerUtils.resetPlayer(player, GameMode.SURVIVAL);
            player.teleport(new Location(player.getWorld(), 0.5, 176, 0.5));
        }

        timerStartGame = new Timer(plugin) {
            @Override
            public void onStart() {
                state = GAME_STARTING;
                survivalGamesPreStart();
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
        }.set(TIMER_STARTING);

        timerBegin = new Timer(plugin) {
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
        }.set(TIMER_BEGIN);

        timerInProgress = new Timer(plugin) {
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
                stage = 0;
                timerStartGame.set(TIMER_STARTING);
                timerBegin.set(TIMER_BEGIN);
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
        plugin.getServer().getPluginManager().registerEvents(new ListenerSurvivalGames(this), plugin);
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
        world.setFullTime(6000);
    }


    public int getWorldBorder() {
        WorldBorder border = javaPlugin.getServer().getWorld("world").getWorldBorder();
        int size = (int) border.getSize();
        // Ask Jacky about this one...
        if (size == 501)
            size = 500;
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

    public int getPlayersAlive() {
        int counter = 0;
        for (PlayerInterface player : players) {
            PlayerSurvivalGames playerSurvivalGames = (PlayerSurvivalGames) player;
            if (!playerSurvivalGames.dead) {
                counter++;
            }
        }
        return counter;
    }

    private void survivalGamesPreStart() {
        fillChestsStage1();

        sendTitleAll("Welcome to Survival Games!", "Game will begin in 60 seconds!");
        sendMultipleMessageAll(new String[]{
                ChatColor.RED + "" + ChatColor.BOLD + "Welcome to Survival Games!\n" + ChatColor.RESET +
                        "This map is " + ChatColor.BOLD + "Breeze Island 2" + ChatColor.RESET + ", by xBayani\n" +
                        " \n" +
                        " \n",
                ChatColor.GREEN + "" + ChatColor.BOLD + "How to play:\n" + ChatColor.RESET +
                        "1. Collect loot from chests around the map\n" +
                        "2. Kill other players (+50pts)\n" +
                        "3. Survive longer than the other players (+25pts)\n" +
                        "4. Watch out for server events (border shrink, chest refills, and etc...)",
        }, new int[]{
                10,
                45,
        });
    }

    private void survivalGamesBegin() {
        filledSpawnpoints.clear();

        // Assign Players a unique pad
        int spawnPads = spawnpoints.size();
        for (PlayerInterface player : players) {
            PlayerUtils.resetPlayer(player.bukkitPlayer, GameMode.SURVIVAL);

            // Get a free spawn slot
            int padNo;
            do {
                padNo = new Random().nextInt(spawnPads);
            }
            while (filledSpawnpoints.contains(padNo));
            filledSpawnpoints.add(padNo);

            // Send player to that slot
            Point3D spawnPad = spawnpoints.get(padNo);
            Location spawnPadLoc = new Location(player.bukkitPlayer.getWorld(), spawnPad.x + 0.5, spawnPad.y, spawnPad.z + 0.5);
            spawnPadLoc.setPitch(0);
            spawnPadLoc.setYaw((float) Math.toDegrees(Math.atan2(spawnPad.x, -spawnPad.z)));
            player.bukkitPlayer.teleport(spawnPadLoc);
            player.bukkitPlayer.setGameMode(GameMode.SURVIVAL);
        }
    }

    private void survivalGamesStageStart() {
        timerInProgress.set(stageTimes.get(stage));
        WorldBorder border = javaPlugin.getServer().getWorld("world").getWorldBorder();
        switch (stage) {
            case 1:
                border.setSize(200 + 1, 60);
                border.setCenter(0.5, 0.5);
                sendTitleAll("Border is shrinking to 200 blocks", "over the next 60 seconds!");
                break;
            case 2:
                fillChestsStage2();
                sendTitleAll("Chests have been refilled!", "It is now possible to get diamond gear and weapons!");
                break;
            case 3:
                border.setSize(50 + 1, 60);
                border.setCenter(0.5, 0.5);
                sendTitleAll("Border is shrinking to 50 blocks", "over the next 60 seconds!");
                break;
            case 4:
                border.setSize(25, 30);
                border.setCenter(0.5, 0.5);
                sendTitleAll(ChatColor.RED + "Deathmatch!" + ChatColor.RESET, "Border is shrinking to 25 blocks");
                break;
            default:
                break;
        }
        stage++;
    }

    private void survivalGamesTick() {
        int counter = 0;
        int lastTeamAlive = teamIDs.get(0);

        // Go through all the teams
        for (int teamID : teamIDs) {
            MCGTeam team = teams.get(teamID);
            // Check if each team still has at least one player alive
            boolean alive = false;
            for (UUID uuid : team.players) {
                PlayerSurvivalGames player = (PlayerSurvivalGames) playerLookup.get(uuid);
                if (player != null)
                    if (!player.dead) {
                        alive = true;
                    }
            }
            // If they do, this is a candidate to be the last team standing
            if (alive) {
                counter++;
                lastTeamAlive = teamID;
            }
        }

        if (counter == 1) {
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
        WorldBorder border = javaPlugin.getServer().getWorld("world").getWorldBorder();
        border.setSize(500 + 1);
        border.setCenter(0.5, 0.5);

        timerInProgress.pause();
        timerFinished.start();

        ArrayList<PlayerSurvivalGames> playerSurvivalGamess = new ArrayList<>();
        for (PlayerInterface player : players) {
            player.bukkitPlayer.setGameMode(GameMode.SPECTATOR);
            PlayerSurvivalGames playerSurvivalGames = (PlayerSurvivalGames) player;
            playerSurvivalGamess.add(playerSurvivalGames);
            if (!playerSurvivalGames.dead) {
                playerSurvivalGames.currentScore += 500;
                playerSurvivalGames.bukkitPlayer.sendTitle(ChatColor.GREEN + "Last one standing!", "You have received 500 additional points!", 10, 60, 10);
            } else {
                playerSurvivalGames.bukkitPlayer.sendTitle(ChatColor.RED + "Game Over!", "", 10, 60, 10);
            }
            player.commit();
        }

        String topPlayers = "";
        int count = 0;
        playerSurvivalGamess.sort(Comparator.comparingInt(o -> -o.currentScore));
        for (PlayerSurvivalGames player: playerSurvivalGamess) {
            topPlayers += ChatColor.RESET + "[" + player.currentScore + "] " + player.myTeam.chatColor + "" + player.bukkitPlayer.getDisplayName() + ChatColor.RESET + "\n";
            if (++count > 5) {
                break;
            }
        }

        String topKillers = "";
        count = 0;
        playerSurvivalGamess.sort(Comparator.comparingInt(o -> -o.kills));
        for (PlayerSurvivalGames player: playerSurvivalGamess) {
            topKillers += ChatColor.RESET + "" + player.kills + " kills - " + player.myTeam.chatColor + "" + player.bukkitPlayer.getDisplayName() + ChatColor.RESET + "\n";
            if (++count > 5) {
                break;
            }
        }

        String topTeams = "";
        ArrayList<MCGTeam> teamParkours = new ArrayList<>(teams.values());
        teamParkours.sort(Comparator.comparingInt(o -> -o.getScore("survivalgames")));
        for (MCGTeam team: teamParkours) {
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
        World world = javaPlugin.getServer().getWorld("world");
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
        World world = javaPlugin.getServer().getWorld("world");
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

    private void initLootTables() {
        ItemStack fireResistance = new ItemStack(Material.POTION, 1);
        PotionMeta potionMeta = (PotionMeta) fireResistance.getItemMeta();
        potionMeta.setBasePotionData(new PotionData(PotionType.FIRE_RESISTANCE, false, false));
        fireResistance.setItemMeta(potionMeta);

        ItemStack instantDamage = new ItemStack(Material.SPLASH_POTION, 1);
        potionMeta = (PotionMeta) fireResistance.getItemMeta();
        potionMeta.setBasePotionData(new PotionData(PotionType.INSTANT_DAMAGE, false, true));
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
        lootTable.put(4.000041, new ItemChoice(instantDamage, 1, 0));
        lootTable.put(3.000050, new ItemChoice(Material.APPLE, 5, 1));
        lootTable.put(3.000051, new ItemChoice(Material.COOKIE, 4, 1.5));
        lootTable.put(3.000052, new ItemChoice(Material.COOKED_CHICKEN, 1));
        lootTable.put(1.500060, new ItemChoice(Material.ENCHANTED_BOOK, 1));
        lootTable.put(1.000061, new ItemChoice(Material.EXPERIENCE_BOTTLE, 3, 1));
        lootTable.put(1.000062, new ItemChoice(Material.ENDER_PEARL, 1.5, 0.5));
        lootTable.put(2.000060, new ItemChoice(Material.STICK, 1.5, 0.5));
        lootTable.put(1.000060, new ItemChoice(Material.OAK_PLANKS, 2, 0.5));
        lootTable.put(1.250060, new ItemChoice(Material.GOLD_INGOT, 1));
        lootTable.put(0.500060, new ItemChoice(Material.IRON_INGOT, 1));

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
        lootTableTier2.put(1.000030, new ItemChoice(Material.ARROW, 8, 2));
        lootTableTier2.put(1.000031, new ItemChoice(Material.FISHING_ROD, 1));
        lootTableTier2.put(1.250032, new ItemChoice(Material.LAVA_BUCKET, 1));
        lootTableTier2.put(1.000033, new ItemChoice(Material.TNT, 1));
        lootTableTier2.put(3.000034, new ItemChoice(Material.CROSSBOW, 1));
        lootTableTier2.put(1.000035, new ItemChoice(Material.BOW, 1));
        lootTableTier2.put(2.500036, new ItemChoice(Material.COBWEB, 3, 0.5));
        lootTableTier2.put(1.500037, new ItemChoice(Material.FLINT_AND_STEEL, 1));
        lootTableTier2.put(3.000040, new ItemChoice(fireResistance, 1, 0));
        lootTableTier2.put(4.000041, new ItemChoice(instantDamage, 1, 0));
        lootTableTier2.put(3.000052, new ItemChoice(Material.COOKED_CHICKEN, 3, 0.5));
        lootTableTier2.put(1.500060, new ItemChoice(Material.ENCHANTED_BOOK, 1));
        lootTableTier2.put(3.000061, new ItemChoice(Material.EXPERIENCE_BOTTLE, 3, 1));
        lootTableTier2.put(2.000060, new ItemChoice(Material.STICK, 1.5, 0.5));
        lootTableTier2.put(1.000060, new ItemChoice(Material.OAK_PLANKS, 2, 0.5));
        lootTableTier2.put(1.250060, new ItemChoice(Material.IRON_INGOT, 1));
        lootTableTier2.put(0.500060, new ItemChoice(Material.DIAMOND, 1));

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
            if(player.getGameMode() == GameMode.SPECTATOR) {
                player.closeInventory();
            }
            if(player.getGameMode() == GameMode.SURVIVAL) {
                if(player.getOpenInventory().getType() == InventoryType.CHEST) {
                    try {
                        openedChests.add(new Coord3D(player.getOpenInventory().getTopInventory().getLocation()));
                    } catch(Exception e) {
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

                    if(newItem.getType() == Material.ENCHANTED_BOOK) {
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
            for(int tries = 0; tries < 100; ++tries) {
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
        World world = javaPlugin.getServer().getWorld("world");
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
        guardiansScaledVestMeta.addEnchant(EnchantmentWrapper.PROTECTION_ENVIRONMENTAL, 1, false);
        guardiansScaledVestMeta.addEnchant(EnchantmentWrapper.PROTECTION_FIRE, 1, false);
        guardiansScaledVestMeta.addEnchant(EnchantmentWrapper.THORNS, 1, false);
        guardiansScaledVestMeta.addEnchant(EnchantmentWrapper.VANISHING_CURSE, 1, false);
        guardiansScaledVestMeta.addAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE, new AttributeModifier(UUID.randomUUID(), "generic.knockback_resistance", 1.0, AttributeModifier.Operation.ADD_SCALAR, EquipmentSlot.CHEST));
        guardiansScaledVestMeta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier(UUID.randomUUID(), "generic.movement_speed", -0.2, AttributeModifier.Operation.ADD_SCALAR, EquipmentSlot.CHEST));
        guardiansScaledVest.setItemMeta(guardiansScaledVestMeta);

        ItemStack solitudesScimitar = new ItemStack(Material.IRON_SWORD, 1);
        ItemMeta solitudesScimitarMeta = solitudesScimitar.getItemMeta();
        solitudesScimitarMeta.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "Solitude's Scimitar" + ChatColor.RESET);
        solitudesScimitarMeta.addEnchant(EnchantmentWrapper.DAMAGE_ALL, 2, false);
        solitudesScimitarMeta.addEnchant(EnchantmentWrapper.VANISHING_CURSE, 1, false);
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

        placeItemInChest(world, new Coord3D(37, 37, -158), rebornGod); // Legendary Chest, under the volcano, north of spawn
        placeItemInChest(world, new Coord3D(-135, 124, -14), solitudesScimitar); // Special Chest, on top of big pirate ship, west of spawn
        placeItemInChest(world, new Coord3D(26, 80, 51), assassinsWindbreaker); // Special Chest, inside the cave, south east of spawn
        placeItemInChest(world, new Coord3D(-3, 84, -34), guardiansScaledVest); // Special Chest, at spawn, waterfall, north of spawn
    }

    private void fillChestsStage2() {
        World world = javaPlugin.getServer().getWorld("world");
        fillChestWithLootTableIntegrated(world, lootTableTier2Integrated);
    }

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
