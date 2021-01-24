package ca.zharry.MinecraftGamesServer.Servers;

import ca.zharry.MinecraftGamesServer.Commands.CommandCutsceneStart;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerPause;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerResume;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerSet;
import ca.zharry.MinecraftGamesServer.Listeners.DisableHunger;
import ca.zharry.MinecraftGamesServer.Listeners.ListenerParkour;
import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Players.PlayerParkour;
import ca.zharry.MinecraftGamesServer.Timer.Cutscene;
import ca.zharry.MinecraftGamesServer.Timer.CutsceneStep;
import ca.zharry.MinecraftGamesServer.Timer.Timer;
import ca.zharry.MinecraftGamesServer.Utils.EntityHider;
import ca.zharry.MinecraftGamesServer.Utils.PlayerUtils;
import ca.zharry.MinecraftGamesServer.Utils.Point3D;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;

// /kill @e[type=minecraft:armor_stand,distance=..2]
// /summon armor_stand ~ ~1 ~ {Invlunerable:1,NoGravity:1,Invisible:1,CustomNameVisible:1,Marker:1,CustomName:'{"color":"yellow","text":"Text"}'}

public class ServerParkour extends ServerInterface {

    private final EntityHider entityHider;

    // Game config
    public static final int TIMER_STARTING = 60 * 20;
    public static final int TIMER_INPROGRESS = 15 * 60 * 20;
    public static final int TIMER_FINISHED = 45 * 20;
    public final ArrayList<Point3D> stage1Checkpoints = new ArrayList<Point3D>();
    public final ArrayList<Point3D> stage2Checkpoints = new ArrayList<Point3D>();
    public final ArrayList<Point3D> stage3Checkpoints = new ArrayList<Point3D>();
    public final ArrayList<Point3D> stage4Checkpoints = new ArrayList<Point3D>();
    public final ArrayList<Point3D> stage5Checkpoints = new ArrayList<Point3D>();
    public final ArrayList<Point3D> stage6Checkpoints = new ArrayList<Point3D>();
    public final ArrayList<ArrayList<Point3D>> allCheckpoints = new ArrayList<>();
    public static final ArrayList<ArmorStand> armorStands = new ArrayList<ArmorStand>();

    // Points of interest
    public Location mapStart;
    public Location mapEnd;

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

    public ServerParkour(JavaPlugin plugin) {
        super(plugin);
        entityHider = new EntityHider(plugin, EntityHider.Policy.BLACKLIST);
        serverSpawn = new Location(world, 253.5, 134, -161.5);
        mapStart = new Location(world, 10000.5, 64, 0.5, 90, 0);
        mapEnd = new Location(world, 8.5, 131, 9.5);

        this.state = GAME_WAITING;

        initCheckpoints();
        setPlayerInventories();

        timerStartGame = new Timer(plugin) {
            @Override
            public void onStart() {
                state = GAME_STARTING;
                parkourStart();

                sendTitleAll("Good luck, have fun!", "The game will begin in 60 seconds!");
                sendMultipleMessageAll(new String[]{
                        ChatColor.GREEN + "" + ChatColor.BOLD + "Here's a recap:\n" + ChatColor.RESET +
                                "This map is " + ChatColor.BOLD + "Parkour Stripes 2" + ChatColor.RESET + ", by Tommycreeper\n" +
                                " \n" +
                                " \n",
                        ChatColor.GREEN + "" + ChatColor.BOLD + "How to play:\n" + ChatColor.RESET +
                                "1. Step on beacons to receive a checkpoint\n" +
                                "2. Each checkpoint is worth 150 points!\n" +
                                "3. You will be teleported to next stage upon reaching it's last checkpoint\n" +
                                "4. Go as far as you can!",
                }, new int[]{
                        120,
                        45,
                });
            }

            @Override
            public void onTick() {
                countdownTimer(this, 11,
                        "Get ready!",
                        "",
                        "",
                        ChatColor.GREEN + "Go!");
            }

            @Override
            public void onEnd() {
                timerInProgress.start();
            }
        }.set(TIMER_STARTING);

        timerInProgress = new Timer(plugin) {
            @Override
            public void onStart() {
                state = GAME_INPROGRESS;
            }

            @Override
            public void onTick() {
                parkourTick();
                countdownTimer(this, 11,
                        "Time's running out!",
                        "",
                        "seconds left!",
                        ChatColor.RED + "Game Over!");
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
            public void onTick() {
            }

            @Override
            public void onEnd() {
                sendTitleAll("Joining Lobby...", "", 5, 20, 30);
                sendPlayersToLobby();

                state = GAME_WAITING;
                timerStartGame.set(TIMER_STARTING);
                timerInProgress.set(TIMER_INPROGRESS);
                timerFinished.set(TIMER_FINISHED);
            }
        }.set(TIMER_FINISHED);

        ArrayList<CutsceneStep> steps = new ArrayList<>();
        int time = 0;
        steps.add(new CutsceneStep(time)
                .pos(16.5, 118, 16.5, 90, 40)
                .title("Welcome to Parkour", "Map made by Tommycreeper", 60));
        steps.add(new CutsceneStep(time += 100)
                .pos(9922.5, 64, 1.5, 90, 40)
                .title("Beacons are checkpoints", "Each checkpoint is worth 150 points!", 60));
        steps.add(new CutsceneStep(time += 100)
                .pos(20000, 74, -18, 50, 22)
                .title("Upon completing each stage", "you will be teleported to the next stage", 60));
        steps.add(new CutsceneStep(time += 100)
                .pos(30000, 74, -18, 50, 22)
                .title("Try to go as far as you can!", "There are six stages in total!", 60));

        startGameTutorial = new Cutscene(plugin, this, steps) {
            @Override
            public void onStart() {
                for (PlayerInterface p : players) {
                    p.bukkitPlayer.setGameMode(GameMode.SPECTATOR);
                    disableWaypoints((PlayerParkour) p);
                }
            }

            @Override
            public void onEnd() {
                for (PlayerInterface p : players) {
                    enableWaypoints((PlayerParkour) p);
                }
                timerStartGame.start();
            }
        };
    }

    @Override
    public void onEnableCall() {
        super.onEnableCall();
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
        plugin.getServer().getPluginManager().registerEvents(new ListenerParkour(this), plugin);
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
        world.setFullTime(6000);

        // Stage 1
        for (int x = 625; x >= 625 - 7; --x)
            for (int z = -1; z <= 0; ++z)
                world.setChunkForceLoaded(x, z, true);
        // Stage 2
        for (int x = 1250; x >= 1250 - 7; --x)
            for (int z = -1; z <= 0; ++z)
                world.setChunkForceLoaded(x, z, true);
        // Stage 3
        for (int x = 1875; x >= 1875 - 7; --x)
            for (int z = -1; z <= 0; ++z)
                world.setChunkForceLoaded(x, z, true);
        // Stage 4
        for (int x = 2500; x >= 1875 - 7; --x)
            for (int z = -1; z <= 0; ++z)
                world.setChunkForceLoaded(x, z, true);
        // Stage 5
        for (int x = 3125; x >= 1875 - 7; --x)
            for (int z = -1; z <= 0; ++z)
                world.setChunkForceLoaded(x, z, true);
        // Stage 6
        for (int x = 3750; x >= 1875 - 7; --x)
            for (int z = -1; z <= 0; ++z)
                world.setChunkForceLoaded(x, z, true);

        configureWaypoints();
    }

    @Override
    public PlayerInterface createNewPlayerInterface(UUID uuid, String name) {
        return new PlayerParkour(this, uuid, name);
    }

    /* GAME LOGIC */

    private void parkourStart() {
        for (PlayerInterface player : players) {
            PlayerParkour parkourPlayer = (PlayerParkour) player;
            PlayerUtils.resetPlayer(player.bukkitPlayer, GameMode.ADVENTURE);
            player.bukkitPlayer.teleport(mapStart);
            player.bukkitPlayer.setInvisible(true);
            parkourPlayer.stage = 1;
            parkourPlayer.level = 0;
        }
        setPlayerInventories();
    }

    private void parkourTick() {

    }

    private void parkourEnd() {
        ArrayList<PlayerParkour> playerParkours = new ArrayList<>();
        for (PlayerInterface player : players) {
            playerParkours.add((PlayerParkour) player);
            if (player.bukkitPlayer.getLocation().getY() < 130) {
                player.bukkitPlayer.teleport(mapEnd);
                player.bukkitPlayer.setInvisible(false);
            }
            player.commit();
        }

        String topPlayers = "";
        int count = 0;
        playerParkours.sort(Comparator.comparingInt(o -> -o.getCurrentScore()));
        for (PlayerParkour player : playerParkours) {
            topPlayers += ChatColor.RESET + "Stage " + player.currentMetadata + " [" + player.getCurrentScore() + "] " + player.bukkitPlayer.getDisplayName() + ChatColor.RESET + "\n";
            if (++count > 5) {
                break;
            }
        }

        String topTeams = "";
        ArrayList<MCGTeam> teamParkours = getRealTeams();
        teamParkours.sort(Comparator.comparingInt(o -> -o.getScore("parkour")));
        for (MCGTeam team : teamParkours) {
            topTeams += ChatColor.RESET + "[" + team.getScore("parkour") + "] " + team.chatColor + "" + team.teamname + "\n";
        }

        sendMultipleMessageAll(new String[]{
                ChatColor.BOLD + "Top Players:\n" + topPlayers +
                        " \n",
                ChatColor.BOLD + "Final Team Score for Parkour:\n" + topTeams,
        }, new int[]{
                10,
                60,
        });
    }

    /* SUPPORTING LOGIC */

    public void setPlayerInventories() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            PlayerParkour playerParkour = (PlayerParkour) playerLookup.get(player.getUniqueId());
            setPlayerInventoryContents(playerParkour);
        });
    }

    public void setPlayerInventoryContents(PlayerParkour player) {
        player.bukkitPlayer.getInventory().clear();
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS, 1);
        LeatherArmorMeta bootsMeta = (LeatherArmorMeta) boots.getItemMeta();
        bootsMeta.setColor(Color.fromRGB(player.myTeam.chatColor.asBungee().getColor().getRGB() & 0xFFFFFF));
        bootsMeta.setUnbreakable(true);
        boots.setItemMeta(bootsMeta);
        player.bukkitPlayer.getInventory().setBoots(boots);

        ItemStack unstuck = new ItemStack(Material.PAPER, 1);
        ItemMeta unstuckMeta = unstuck.getItemMeta();
        unstuckMeta.setDisplayName(ChatColor.RED + "Teleport to last checkpoint");
        unstuck.setItemMeta(unstuckMeta);
        player.bukkitPlayer.getInventory().setItem(8, unstuck);

        if (state == GAME_WAITING) {
            ItemStack levelSelect = new ItemStack(Material.TARGET, 1);
            ItemMeta meta = levelSelect.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + "Level select");
            levelSelect.setItemMeta(meta);
            player.bukkitPlayer.getInventory().setItem(7, levelSelect);
        }

        ItemStack waypoints = new ItemStack(Material.END_ROD, 1);
        ItemMeta waypointsItemMeta = waypoints.getItemMeta();
        waypointsItemMeta.setDisplayName(ChatColor.GREEN + "Toggle in-game waypoints");
        waypoints.setItemMeta(waypointsItemMeta);
        player.bukkitPlayer.getInventory().setItem(0, waypoints);
    }

    /* MAP LOGIC */

    public void initCheckpoints() {
        stage1Checkpoints.add(new Point3D(7 - 7 + 10000, 77 - 77 + 63, 9 - 9));
        stage1Checkpoints.add(new Point3D(-18 - 7 + 10000, 77 - 77 + 63, 11 - 9));
        stage1Checkpoints.add(new Point3D(-39 - 7 + 10000, 77 - 77 + 63, 11 - 9));
        stage1Checkpoints.add(new Point3D(-73 - 7 + 10000, 77 - 77 + 63, 10 - 9));
        stage1Checkpoints.add(new Point3D(-82 - 7 + 10000, 86 - 77 + 63, 5 - 9));
        allCheckpoints.add(stage1Checkpoints);

        stage2Checkpoints.add(new Point3D(7 - 7 + 20000, 77 - 77 + 63, 23 - 23));
        stage2Checkpoints.add(new Point3D(-26 - 7 + 20000, 77 - 77 + 63, 22 - 23));
        stage2Checkpoints.add(new Point3D(-29 - 7 + 20000, 85 - 77 + 63, 27 - 23));
        stage2Checkpoints.add(new Point3D(-65 - 7 + 20000, 77 - 77 + 63, 23 - 23));
        stage2Checkpoints.add(new Point3D(-85 - 7 + 20000, 90 - 77 + 63, 24 - 23));
        allCheckpoints.add(stage2Checkpoints);

        stage3Checkpoints.add(new Point3D(7 - 7 + 30000, 77 - 77 + 63, -5 + 5));
        stage3Checkpoints.add(new Point3D(-14 - 7 + 30000, 81 - 77 + 63, -9 + 5));
        stage3Checkpoints.add(new Point3D(-38 - 7 + 30000, 79 - 77 + 63, -5 + 5));
        stage3Checkpoints.add(new Point3D(-75 - 7 + 30000, 78 - 77 + 63, -5 + 5));
        stage3Checkpoints.add(new Point3D(-89 - 7 + 30000, 92 - 77 + 63, -11 + 5));
        allCheckpoints.add(stage3Checkpoints);

        stage4Checkpoints.add(new Point3D(7 - 7 + 40000, 77 - 77 + 63, 37 - 37));
        stage4Checkpoints.add(new Point3D(-21 - 7 + 40000, 80 - 77 + 63, 32 - 37));
        stage4Checkpoints.add(new Point3D(-41 - 7 + 40000, 83 - 77 + 63, 34 - 37));
        stage4Checkpoints.add(new Point3D(-66 - 7 + 40000, 77 - 77 + 63, 35 - 37));
        stage4Checkpoints.add(new Point3D(-89 - 7 + 40000, 77 - 77 + 63, 41 - 37));
        stage4Checkpoints.add(new Point3D(-92 - 7 + 40000, 89 - 77 + 63, 43 - 37));
        allCheckpoints.add(stage4Checkpoints);

        stage5Checkpoints.add(new Point3D(7 - 7 + 50000, 77 - 77 + 63, -19 + 19));
        stage5Checkpoints.add(new Point3D(-83 - 7 + 50000, 65 - 77 + 63, -17 + 19));
        stage5Checkpoints.add(new Point3D(-91 - 7 + 50000, 80 - 77 + 63, -18 + 19));
        stage5Checkpoints.add(new Point3D(-88 - 7 + 50000, 89 - 77 + 63, -22 + 19));
        allCheckpoints.add(stage5Checkpoints);

        stage6Checkpoints.add(new Point3D(7 - 7 + 60000, 77 - 77 + 63, 51 - 51));
        stage6Checkpoints.add(new Point3D(-27 - 7 + 60000, 80 - 77 + 63, 52 - 51));
        stage6Checkpoints.add(new Point3D(-35 - 7 + 60000, 84 - 77 + 63, 48 - 51));
        stage6Checkpoints.add(new Point3D(-45 - 7 + 60000, 92 - 77 + 63, 55 - 51));
        stage6Checkpoints.add(new Point3D(-61 - 7 + 60000, 94 - 77 + 63, 54 - 51));
        stage6Checkpoints.add(new Point3D(-67 - 7 + 60000, 98 - 77 + 63, 49 - 51));
        stage6Checkpoints.add(new Point3D(-74 - 7 + 60000, 106 - 77 + 63, 54 - 51));
        stage6Checkpoints.add(new Point3D(-74 - 7 + 60000, 111 - 77 + 63, 51 - 51));
        stage6Checkpoints.add(new Point3D(-85 - 7 + 60000, 118 - 77 + 63, 54 - 51));
        allCheckpoints.add(stage6Checkpoints);

    }
    // 4 4 4 5 3 7

    private void configureWaypoints() {
        world.getEntitiesByClass(ArmorStand.class).forEach(armorStand -> {
            if (armorStand.getLocation().getX() > 5000) {
                String name = ChatColor.stripColor(armorStand.getCustomName());
                name = ChatColor.GOLD + name;

                /*
                // Check if the name is a lone number
                try {
                    int numCheck = Integer.parseInt(name);
                    name = ChatColor.GOLD + name;
                } catch (Exception e1) {
                    String[] tokens = name.split(". ");

                    // Check if the step with text or just a message
                    try {
                        int numCheck = Integer.parseInt(tokens[0]);
                        name = ChatColor.GOLD + name;
                    } catch (Exception e2) {

                        // Its a message
                        name = ChatColor.GOLD + name;
                    }
                }*/
                armorStand.setCustomName(name);
                armorStands.add(armorStand);
            }
        });

    }

    public void disableWaypoints(PlayerParkour player) {
        for (ArmorStand e : armorStands) {
            entityHider.hideEntity(player.bukkitPlayer, e);
        }
    }

    public void enableWaypoints(PlayerParkour player) {
        for (ArmorStand e : armorStands) {
            entityHider.showEntity(player.bukkitPlayer, e);
        }
    }

}
