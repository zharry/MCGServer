package ca.zharry.MinecraftGamesServer.Servers;

import ca.zharry.MinecraftGamesServer.Commands.CommandTimerPause;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerResume;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerSet;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerStart;
import ca.zharry.MinecraftGamesServer.Listeners.DisableDamage;
import ca.zharry.MinecraftGamesServer.Listeners.DisableHunger;
import ca.zharry.MinecraftGamesServer.Listeners.ListenerSpleef;
import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Players.PlayerSpleef;
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
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;

public class ServerSpleef extends ServerInterface {

    // Game config
    public static final int TIMER_STARTING = 60 * 20;
    public static final int TIMER_INPROGRESS = 15 * 60 * 20;
    public static final int TIMER_FINISHED = 45 * 20;
    public static final int COMPETITION_MAX_HEIGHT = 73;
    public static final int TOTAL_GAMES = 3;

    public static final int TNT_WARNING_TIME = 6 * 20;
    public static final int TNT_PRIME_TIME = 1 * 20;

    // Server state
    public static final int ERROR = -1;
    public static final int GAME_WAITING = 0;
    public static final int GAME_STARTING = 1;
    public static final int GAME_INPROGRESS = 2;
    public static final int GAME_FINISHED = 3;
    public int state = ERROR;
    public int currentGame = 1;
    public boolean displayedWelcomeMessage = false;
    public int tntCooldown = 0;

    // Server tasks
    public Timer timerStartGame;
    public Timer timerInProgress;
    public Timer timerFinished;

    public ProtocolManager protocolManager;

    public ServerSpleef(JavaPlugin plugin) {
        super(plugin);

        // Add existing players (for hot-reloading)
        ArrayList<Player> currentlyOnline = new ArrayList<>(Bukkit.getOnlinePlayers());
        for (Player player : currentlyOnline) {
            addPlayer(new PlayerSpleef(player, this));
            PlayerUtils.resetPlayer(player, GameMode.ADVENTURE);
            Location start = new Location(player.getWorld(), 14.5, 75, 17.5);
            player.teleport(start);
        }

        timerStartGame = new Timer(plugin) {
            @Override
            public void onStart() {
                state = GAME_STARTING;
                spleefRestore(Bukkit.getWorld("world"));

                if (!displayedWelcomeMessage) {
                    displayedWelcomeMessage = true;
                    sendTitleAll("Welcome to Spleef!", "");
                    sendMultipleMessageAll(new String[]{
                            ChatColor.RED + "" + ChatColor.BOLD + "Welcome to Spleef!\n" + ChatColor.RESET +
                                    "This map is " + ChatColor.BOLD + "Makers Spleef 2" + ChatColor.RESET + ", by MineMakers Team\n" +
                                    " \n" +
                                    " \n",
                            ChatColor.GREEN + "" + ChatColor.BOLD + "How to play:\n" + ChatColor.RESET +
                                    "1. Destroy blocks to drop other players into the void\n" +
                                    "2. Survive longer than all of the other players\n" +
                                    "3. Each time a player is eliminated everyone who is alive receives +100 points",
                    }, new int[]{
                            10,
                            45,
                    });
                }
            }

            @Override
            public void onTick() {
                countdownTimer(this, 11,
                        "Game is starting!",
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
                spleefStart();
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
                displayedWelcomeMessage = false;
                timerStartGame.set(TIMER_STARTING);
                timerInProgress.set(TIMER_INPROGRESS);
                timerFinished.set(TIMER_FINISHED);
            }
        }.set(TIMER_FINISHED);

        protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new PacketAdapter(this.plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.EXPLOSION) {
            @Override
            public void onPacketSending(PacketEvent event) {
                event.setCancelled(true);
            }
        });
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
        plugin.getServer().getPluginManager().registerEvents(new ListenerSpleef(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DisableHunger(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DisableDamage(), plugin);
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

    private void spleefStart() {
        players.forEach((player) -> ((PlayerSpleef) player).dead = false);
        Player dummyPlayer = players.get(0).bukkitPlayer;

        int maxY = 64, minY = 61, spreadRadius = 30;
        Random random = new Random();

        for (PlayerInterface player : players) {
            Location spreadStart = new Location(dummyPlayer.getWorld(), 14.5, 62, 17.5);

            while (true) {
                int x = random.nextInt(spreadRadius * 2 + 1) - spreadRadius + (int) spreadStart.getX();
                int z = random.nextInt(spreadRadius * 2 + 1) - spreadRadius + (int) spreadStart.getZ();
                int y = minY;

                boolean found = false;
                for (; y <= maxY; y++) {
                    if (player.bukkitPlayer.getWorld().getBlockAt(x, y, z).getType() != Material.AIR &&
                            player.bukkitPlayer.getWorld().getBlockAt(x, y + 1, z).getType() == Material.AIR) {
                        spreadStart = new Location(dummyPlayer.getWorld(), x + 0.5, y + 2, z + 0.5);
                        found = true;
                        break;
                    }
                }
                if (found) {
                    break;
                }
            }

            ItemStack pickaxe = new ItemStack(Material.DIAMOND_PICKAXE, 1);
            ItemMeta pickaxeMeta = pickaxe.getItemMeta();
            pickaxeMeta.addEnchant(Enchantment.DIG_SPEED, 32767, true);
            pickaxeMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            pickaxeMeta.setUnbreakable(true);
            pickaxe.setItemMeta(pickaxeMeta);

            PlayerUtils.resetPlayer(player.bukkitPlayer, GameMode.SURVIVAL);
            player.bukkitPlayer.getInventory().addItem(pickaxe);
            player.bukkitPlayer.teleport(spreadStart);
        }
    }

    private void spleefTick() {
        int counter = 0;
        PlayerInterface lastOneAlive = players.get(0);
        for (PlayerInterface player : players) {
            PlayerSpleef playerSpleef = (PlayerSpleef) player;
            if (!playerSpleef.dead) {
                lastOneAlive = player;
                counter++;
            }
        }

        if (counter == 1) {
            timerInProgress.set(0);
            return;
        }

        ArrayList<PlayerInterface>[] playerLayers = (ArrayList<PlayerInterface>[]) new ArrayList[2];
        for (int i = 0; i < playerLayers.length; ++i) {
            playerLayers[i] = new ArrayList<>();
        }

        // Finds players on with layer of the map they are on
        for (PlayerInterface player : players) {
            if (player.bukkitPlayer.getGameMode() != GameMode.SURVIVAL) {
                continue;
            }
            double ypos = player.bukkitPlayer.getLocation().getY();
            if (ypos < COMPETITION_MAX_HEIGHT && ypos >= 56) {
                // upper layer
                playerLayers[0].add(player);
            } else if (ypos < 56 && ypos >= 36) {
                // lower layer
                playerLayers[1].add(player);
            }
        }

        // Find all the players that need to blow up
        ArrayList<PlayerInterface> playersToBlowUp = new ArrayList<>();
        outer:
        for (int i = 0; i < playerLayers.length; ++i) {
            int teamId = -1;
            for (PlayerInterface player : playerLayers[i]) {
                if (teamId == -1) {
                    teamId = player.myTeam.id;
                } else if (teamId != player.myTeam.id) {
                    break outer;
                }
            }

            if (teamId != -1) {
                for (PlayerInterface player : playerLayers[i]) {
                    playersToBlowUp.add(player);
                }
                break;
            }
        }

        // Blow em' up
        if (playersToBlowUp.size() > 0) {
            // Send the warning message
            if (tntCooldown == TNT_WARNING_TIME) {
                for (PlayerInterface player : playersToBlowUp) {
                    player.bukkitPlayer.sendTitle(ChatColor.GOLD + "Warning: TNT spawning!", "You are the last team alive on this layer.", 0, 60, 20);
                }
            }

            tntCooldown--;
            if (tntCooldown <= 0) {
                for (PlayerInterface player : playersToBlowUp) {
                    TNTPrimed tnt = player.bukkitPlayer.getWorld().spawn(player.bukkitPlayer.getLocation(), TNTPrimed.class);
                    tnt.setYield(8);
                    tnt.setGravity(false);
                    tnt.setVelocity(new Vector(0, 0, 0));
                }
                tntCooldown = TNT_PRIME_TIME;
            }
        } else {
            tntCooldown = TNT_WARNING_TIME;
        }

        // Try to stop TNT from moving
        players.get(0).bukkitPlayer.getWorld().getEntitiesByClass(TNTPrimed.class).forEach(tnt -> tnt.setVelocity(new Vector(0, 0, 0)));
    }

    private void spleefEnd() {
        Player dummyPlayer = players.get(0).bukkitPlayer;
        Location mapEnd = new Location(dummyPlayer.getWorld(), 14.5, 75, 17.5);

        ArrayList<PlayerSpleef> playerSpleefs = new ArrayList<>();
        for (PlayerInterface player : players) {
            playerSpleefs.add((PlayerSpleef) player);
            // Race condition fix for last player alive being stuck in spectator mode after round ends
            new BukkitRunnable(){
                @Override
                public void run() {
                    PlayerUtils.resetPlayer(player.bukkitPlayer, GameMode.ADVENTURE);
                }
            }.runTaskLater(plugin, 5); // Spectator is set in 1 tick delay
            player.bukkitPlayer.teleport(mapEnd);
            if (!((PlayerSpleef) player).dead) {
                ((PlayerSpleef) player).currentScore += 500;
                player.bukkitPlayer.sendTitle(ChatColor.GREEN + "Last player(s) alive!", "You have received 500 additional points!", 0, 60, 10);
            } else {
                player.bukkitPlayer.sendTitle(ChatColor.RED + "Round Over!", "", 0, 60, 10);
            }
            player.commit();
        }

        if (currentGame++ < TOTAL_GAMES) {
            timerStartGame.set(TIMER_STARTING);
            timerInProgress.set(TIMER_INPROGRESS);
            timerStartGame.start();
            return;
        }

        // All rounds completed logic
        timerFinished.start();
        currentGame = 1;

        String topPlayers = "";
        int count = 0;
        playerSpleefs.sort(Comparator.comparingInt(o -> -o.currentScore));
        for (PlayerSpleef player : playerSpleefs) {
            topPlayers += ChatColor.RESET + "[" + player.currentScore + "] " + player.myTeam.chatColor + "" + player.bukkitPlayer.getDisplayName() + ChatColor.RESET + "\n";
            if (++count > 5) {
                break;
            }
        }

        String topTeams = "";
        ArrayList<MCGTeam> teamSpleefs = new ArrayList<>(teams.values());
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

    private void spleefRestore(World world) {
        for (int x = -24; x < 53; ++x) {
            for (int y = 38; y < 74; ++y) {
                for (int z = -19; z < 54; ++z) {
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
