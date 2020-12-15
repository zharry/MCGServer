package ca.zharry.MinecraftGamesServer.Servers;

import ca.zharry.MinecraftGamesServer.Commands.CommandTimerPause;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerResume;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerSet;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerStart;
import ca.zharry.MinecraftGamesServer.Listeners.*;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Players.PlayerSpleef;
import ca.zharry.MinecraftGamesServer.Timer.Timer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Random;

public class ServerSpleef extends ServerInterface {

    // Game config
    public static final int TIMER_STARTING = 60 * 20;
    public static final int TIMER_INPROGRESS = 15 * 60 * 20;
    public static final int TIMER_FINISHED = 45 * 20;
    public static final int COMPETITION_MAX_HEIGHT = 73;

    // Server state
    public static final int ERROR = -1;
    public static final int GAME_WAITING = 0;
    public static final int GAME_STARTING = 1;
    public static final int GAME_INPROGRESS = 2;
    public static final int GAME_FINISHED = 3;
    public int state = ERROR;
    public ArrayList<PlayerSpleef> dead;

    // Server tasks
    public Timer timerStartGame;
    public Timer timerInProgress;
    public Timer timerFinished;

    public ServerSpleef(JavaPlugin plugin) {
        super(plugin);
        dead = new ArrayList<PlayerSpleef>();

        // Add existing players (for hot-reloading)
        ArrayList<Player> currentlyOnline = new ArrayList<>(Bukkit.getOnlinePlayers());
        for (Player player : currentlyOnline) {
            addPlayer(new PlayerSpleef(player, this));
        }

        timerStartGame = new Timer(plugin) {
            @Override
            public void onStart() {
                state = GAME_STARTING;
            }

            @Override
            public void onTick() {
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
            }

            @Override
            public void onEnd() {
                spleefEnd();
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
        plugin.getServer().getPluginManager().registerEvents(new ListenerOnPlayerJoinSpleef(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ListenerOnPlayerQuitSpleef(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DisableHunger(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DisableDamage(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ListenerOnPlayerDeathSpleef(this), plugin);
    }

    private void spleefStart() {
        dead.clear();

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
            pickaxeMeta.addEnchant(Enchantment.DIG_SPEED, 100, true);
            pickaxeMeta.addEnchant(Enchantment.DURABILITY, 10, true);
            pickaxe.setItemMeta(pickaxeMeta);

            player.bukkitPlayer.getInventory().addItem(pickaxe);
            player.bukkitPlayer.teleport(spreadStart);
            player.bukkitPlayer.setGameMode(GameMode.SURVIVAL);
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
            lastOneAlive.bukkitPlayer.sendTitle("Last one alive!", "", 10, 60, 10);
        }
    }

    private void spleefEnd() {
        Player dummyPlayer = players.get(0).bukkitPlayer;
        Location mapEnd = new Location(dummyPlayer.getWorld(), 14.5, 75, 17.5);

        for (PlayerInterface player : players) {
            if (player.bukkitPlayer.getLocation().getY() < COMPETITION_MAX_HEIGHT) {
                player.bukkitPlayer.teleport(mapEnd);
                player.bukkitPlayer.setGameMode(GameMode.ADVENTURE);
                player.bukkitPlayer.getInventory().clear();
            }
            if (!((PlayerSpleef) player).dead) {
                ((PlayerSpleef) player).currentScore += 500;
            }
            player.commit();
        }
    }

}
