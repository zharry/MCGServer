package ca.zharry.MinecraftGamesServer.Servers;

import ca.zharry.MinecraftGamesServer.Commands.CommandCutsceneStart;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerPause;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerResume;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerSet;
import ca.zharry.MinecraftGamesServer.Listeners.ListenerSpleef;
import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Players.PlayerSpleef;
import ca.zharry.MinecraftGamesServer.Timer.Cutscene;
import ca.zharry.MinecraftGamesServer.Timer.CutsceneStep;
import ca.zharry.MinecraftGamesServer.Timer.Timer;
import ca.zharry.MinecraftGamesServer.Utils.PlayerUtils;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;
import java.util.UUID;

public class ServerSpleef extends ServerInterface {

    public ProtocolManager protocolManager;

    // Game config
    public static final int TIMER_STARTING = 60 * 20;
    public static final int TIMER_BEGIN = 10 * 20;
    public static final int TIMER_INPROGRESS = 6 * 60 * 20;
    public static final int TIMER_FINISHED = 45 * 20;
    public static final int COMPETITION_MAX_HEIGHT = 73;
    public static final int COMPETITION_MIN_HEIGHT = -16;
    public static final int TOTAL_GAMES = 3;
    public static final int TELEPORT_MAX_Y = 64;
    public static final int TELEPORT_MIN_Y = 61;
    public static final int TELEPORT_SUDDENDEATH_MAX_Y = 25;
    public static final int TELEPORT_SUDDENDEATH_MIN_Y = 22;
    public static final int TELEPORT_SPREAD_RADIUS = 30;

    // Server state
    public static final int ERROR = -1;
    public static final int GAME_WAITING = 0;
    public static final int GAME_STARTING = 1;
    public static final int GAME_BEGIN = 4;
    public static final int GAME_INPROGRESS = 2;
    public static final int GAME_FINISHED = 3;

    // Sudden death states
    public static final int SUDDENDEATH_PENDING = 0;
    public static final int SUDDENDEATH_COUNTDOWN = 1;
    public static final int SUDDENDEATH_COMPLETED = 2;

    public int state = ERROR;
    public int currentGame = 1;
    public boolean firstRun = true;
    public int tntCooldown = 0;
    public int suddenDeathState = SUDDENDEATH_PENDING;
    public int suddenDeathCounter;

    // Server tasks
    public Timer timerStartGame;
    public Timer timerBegin;
    public Timer timerInProgress;
    public Timer timerFinished;
    public Cutscene startGameTutorial;

    public ServerSpleef(JavaPlugin plugin) {
        super(plugin);
        serverSpawn = new Location(world, 14.5, 75, 17.5);

        // Add existing players (for hot-reloading)
        ArrayList<Player> currentlyOnline = new ArrayList<>(Bukkit.getOnlinePlayers());
        for (Player player : currentlyOnline) {
            PlayerUtils.resetPlayer(player, GameMode.ADVENTURE);
            player.teleport(serverSpawn);
        }

        timerStartGame = new Timer(plugin) {
            @Override
            public void onStart() {
                if (firstRun) {
                    state = GAME_BEGIN;
                    spleefBegin();
                    sendTitleAll("Last one standing wins!", "Good luck! Game begins in 30 seconds!");
                    sendMultipleMessageAll(new String[]{
                            ChatColor.GREEN + "" + ChatColor.BOLD + "Here's a recap:\n" + ChatColor.RESET +
                                    "This map is " + ChatColor.BOLD + "Makers Spleef 2" + ChatColor.RESET + ", by MineMakers Team\n" +
                                    " \n" +
                                    " \n",
                            ChatColor.GREEN + "" + ChatColor.BOLD + "How to play:\n" + ChatColor.RESET +
                                    "1. Destroy blocks to drop other players into the void\n" +
                                    "2. Survive longer as long as you can, winning the game is worth 250 points!\n" +
                                    "3. Each time a player is eliminated everyone will receive +75 points\n" +
                                    "4. If you are the last team on the layer, TNT will start spawning at your feet!"
                    }, new int[]{
                            120,
                            45,
                    });
                    this.set(30 * 20);
                } else {
                    state = GAME_STARTING;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            sendTitleAll("Next game starts in 50 seconds!", "");
                        }
                    }.runTaskLater(plugin, 200);
                }
            }

            @Override
            public void onTick() {
                if (firstRun) {
                    countdownTimer(this, 11,
                            "",
                            "",
                            "",
                            "Begin!");
                } else {
                    countdownTimer(this, 0,
                            "Teleporting to arena...",
                            "",
                            "",
                            "Teleporting to arena...");
                }
            }

            @Override
            public void onEnd() {
                if (firstRun) {
                    timerInProgress.start();
                    firstRun = false;
                } else {
                    timerBegin.start();
                }
            }
        }.set(TIMER_STARTING);

        timerBegin = new Timer(plugin) {
            @Override
            public void onStart() {
                state = GAME_BEGIN;
                spleefBegin();
            }

            @Override
            public void onTick() {
                countdownTimer(this, 11,
                        "",
                        "",
                        "",
                        ChatColor.GREEN + "Begin!");
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
            }

            @Override
            public void onTick() {
                spleefTick();
                countdownTimer(this, 11,
                        "Time's running out!",
                        "",
                        "seconds left!",
                        "");
            }

            @Override
            public void onEnd() {
                spleefEnd();
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
                firstRun = true;
                timerStartGame.set(TIMER_STARTING);
                timerBegin.set(TIMER_BEGIN);
                timerInProgress.set(TIMER_INPROGRESS);
                timerFinished.set(TIMER_FINISHED);
            }
        }.set(TIMER_FINISHED);

        ArrayList<CutsceneStep> steps = new ArrayList<>();
        int time = 0;
        steps.add(new CutsceneStep(time)
                .pos(45, 65, 35, 125, 30)
                .title("Welcome to Spleef", "Map made by MineMakers Team", 60)
                .freeze(50));
        steps.add(new CutsceneStep(time += 100)
                .pos(14.5, 65, 15.5, 90, 0)
                .title("Survive as long as you can", "and try to eliminate other players", 60)
                .linear()
                .freeze(50));
        steps.add(new CutsceneStep(time += 100)
                .pos(0.5, 42, 9.5, -60, 12)
                .title("The last player standing wins", "earning 250 additional points!", 60)
                .linear()
                .freeze(50));
        steps.add(new CutsceneStep(time += 100)
                .pos(-11, 28.5, 2, -64, 33)
                .title("Each time a player is eliminated", "everyone still alive will be awarded 75 survival points!", 60)
                .linear());

        startGameTutorial = new Cutscene(plugin, this, steps) {
            @Override
            public void onStart() {
                spleefRestore(world);
                for (PlayerInterface p : players) {
                    p.bukkitPlayer.setGameMode(GameMode.SPECTATOR);
                }
            }

            @Override
            public void onEnd() {
                timerStartGame.start();
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
        plugin.getServer().getPluginManager().registerEvents(new ListenerSpleef(this), plugin);
        //plugin.getServer().getPluginManager().registerEvents(new DisableHunger(), plugin);
        //plugin.getServer().getPluginManager().registerEvents(new DisableDamage(), plugin);
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

        protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new PacketAdapter(this.plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.EXPLOSION) {
            @Override
            public void onPacketSending(PacketEvent event) {
                event.setCancelled(true);
            }
        });
    }

    @Override
    public PlayerInterface createNewPlayerInterface(UUID uuid, String name) {
        return new PlayerSpleef(this, uuid, name);
    }

    /* GAME LOGIC */

    private void spleefBegin() {
        spleefRestore(world);
        world.getEntitiesByClass(TNTPrimed.class).forEach(Entity::remove);
        players.forEach((player) -> ((PlayerSpleef) player).dead = false);

        for (PlayerInterface player : players) {
            sendToArena((PlayerSpleef) player, TELEPORT_MIN_Y, TELEPORT_MAX_Y);
        }
    }

    private void spleefTick() {
        int counter = 0;
        for (PlayerInterface player : players) {
            PlayerSpleef playerSpleef = (PlayerSpleef) player;
            if (!playerSpleef.dead) {
                counter++;
            }
        }
        if (counter <= 1) {
            timerInProgress.set(0);
        }

        // Every second, lose half a hunger
        if (timerInProgress.get() % 20 == 0) {
            for (PlayerInterface player : players) {
                // Decrement hunger bar
                int hunger = player.bukkitPlayer.getFoodLevel();
                hunger = Math.max(hunger - 1, 0);
                player.bukkitPlayer.setFoodLevel(hunger);

                // Decrement health bar
                int health = (int) player.bukkitPlayer.getHealth();
                if (hunger <= 0) {
                    player.bukkitPlayer.damage(1);
                }
                if (hunger >= 18) {
                    health = Math.min(health + 1, 20);
                    player.bukkitPlayer.setHealth(health);
                }
            }
        }

        // Sudden death
        if (timerInProgress.get() < 2400 && timerInProgress.get() > 0) {
            if(suddenDeathState == SUDDENDEATH_PENDING) {
                spleefRestore(world);
                for (PlayerInterface player : players) {
                    PlayerSpleef playerSpleef = (PlayerSpleef) player;
                    if (!playerSpleef.dead) {
                        sendToArena((PlayerSpleef) player, TELEPORT_SUDDENDEATH_MIN_Y, TELEPORT_SUDDENDEATH_MAX_Y);
                    }
                }
                suddenDeathState = SUDDENDEATH_COUNTDOWN;
                suddenDeathCounter = 4 * 20;
                sendTitleAll("Sudden death!", "", 0, 20, 20);
            } else if(suddenDeathState == SUDDENDEATH_COUNTDOWN) {
                suddenDeathCounter--;
                if(suddenDeathCounter <= 3 * 20) {
                    int suddenDeathTime = (int) Math.ceil(suddenDeathCounter / 20.0);
                    sendTitleAll("Sudden death!", "" + (suddenDeathTime == 0 ? "Fight!" : suddenDeathTime), 0, 20, 20);
                }
                if(suddenDeathCounter <= 0) {
                    suddenDeathState = SUDDENDEATH_COMPLETED;
                }
            }
        } else {
            suddenDeathState = SUDDENDEATH_PENDING;
        }
    }

    private void spleefEnd() {
        ArrayList<PlayerSpleef> playerSpleefs = new ArrayList<>();
        for (PlayerInterface player : players) {
            playerSpleefs.add((PlayerSpleef) player);
            // Race condition fix for last player alive being stuck in spectator mode after round ends
            new BukkitRunnable() {
                @Override
                public void run() {
                    PlayerUtils.resetPlayer(player.bukkitPlayer, GameMode.ADVENTURE);
                    player.bukkitPlayer.teleport(serverSpawn);
                }
            }.runTaskLater(plugin, 5); // Spectator is set in 1 tick delay

            //
            if (!((PlayerSpleef) player).dead) {
                player.addScore(250, "last player alive");
                player.bukkitPlayer.sendTitle(ChatColor.GREEN + "Last player(s) alive!", "You have received 250 additional points!", 0, 60, 10);
                sendMessageAll(ChatColor.RESET + player.bukkitPlayer.getDisplayName() + " is the last player alive, and has received 250 additional points!");
            } else {
                player.bukkitPlayer.sendTitle(ChatColor.RED + "Round Over!", "", 0, 60, 10);
            }
            player.commit();
        }

        // Check if we played enough rounds
        if (currentGame++ < TOTAL_GAMES) {
            timerStartGame.set(TIMER_STARTING);
            timerBegin.set(TIMER_BEGIN);
            timerInProgress.set(TIMER_INPROGRESS);
            timerStartGame.start();
            return;
        }

        // All rounds completed logic
        timerFinished.start();
        currentGame = 1;

        String topPlayers = "";
        int count = 0;
        playerSpleefs.sort(Comparator.comparingInt(o -> -o.getCurrentScore()));
        for (PlayerSpleef player : playerSpleefs) {
            topPlayers += ChatColor.RESET + "[" + player.getCurrentScore() + "] " + player.bukkitPlayer.getDisplayName() + ChatColor.RESET + "\n";
            if (++count > 5) {
                break;
            }
        }

        String topTeams = "";
        ArrayList<MCGTeam> teamSpleefs = getRealTeams();
        teamSpleefs.sort(Comparator.comparingInt(o -> -o.getScore("spleef")));
        for (MCGTeam team : teamSpleefs) {
            topTeams += ChatColor.RESET + "[" + team.getScore("spleef") + "] " + team.chatColor + "" + team.teamname + "\n";
        }

        sendMultipleMessageAll(new String[]{
                ChatColor.BOLD + "Top Players:\n" + topPlayers +
                        " \n",
                ChatColor.BOLD + "Final Team Score for Spleef:\n" + topTeams,
        }, new int[]{
                10,
                60,
        });
    }

    /* SUPPORTING LOGIC */

    public int getPlayersAlive() {
        int counter = 0;
        for (PlayerInterface player : players) {
            PlayerSpleef playerSpleef = (PlayerSpleef) player;
            if (!playerSpleef.dead) {
                counter++;
            }
        }
        return counter;
    }

    public void sendToArena(PlayerSpleef player, int minY, int maxY) {
        Random random = new Random();

        Location spreadStart = serverSpawn;
        while (true) {
            int x = random.nextInt(TELEPORT_SPREAD_RADIUS * 2 + 1) - TELEPORT_SPREAD_RADIUS + (int) spreadStart.getX();
            int z = random.nextInt(TELEPORT_SPREAD_RADIUS * 2 + 1) - TELEPORT_SPREAD_RADIUS + (int) spreadStart.getZ();
            int y = minY;

            boolean found = false;
            for (; y <= maxY; y++) {
                if (world.getBlockAt(x, y, z).getType() != Material.AIR &&
                        world.getBlockAt(x, y + 1, z).getType() == Material.AIR) {
                    spreadStart = new Location(world, x + 0.5, y + 1, z + 0.5);
                    found = true;
                    break;
                }
            }
            if (found) {
                break;
            }
        }

        PlayerUtils.resetPlayer(player.bukkitPlayer, GameMode.SURVIVAL);
        ItemStack pickaxe = new ItemStack(Material.DIAMOND_PICKAXE, 1);
        ItemMeta pickaxeMeta = pickaxe.getItemMeta();
        pickaxeMeta.addEnchant(Enchantment.DIG_SPEED, 32767, true);
        pickaxeMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        pickaxeMeta.setUnbreakable(true);
        pickaxe.setItemMeta(pickaxeMeta);
        player.bukkitPlayer.getInventory().addItem(pickaxe);

        player.bukkitPlayer.teleport(spreadStart);
    }

    /* MAP LOGIC */

    public void spleefRestore(World world) {
        for (int x = -24; x <= 52; ++x) {
            for (int y = 19; y <= 73; ++y) {
                for (int z = -19; z <= 53; ++z) {
                    Material mat = world.getBlockAt(x + 10000, y, z + 10000).getType();
                    if (mat != Material.AIR) {
                        Block block = world.getBlockAt(x, y, z);
                        block.setType(mat);
                    }
                }
            }
        }
    }

}
