package ca.zharry.MinecraftGamesServer.Servers;

import ca.zharry.MinecraftGamesServer.Commands.CommandCutsceneStart;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimer;
import ca.zharry.MinecraftGamesServer.Listeners.DisableHunger;
import ca.zharry.MinecraftGamesServer.Listeners.ListenerElytraRun;
import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Players.PlayerElytraRun;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Timer.Cutscene;
import ca.zharry.MinecraftGamesServer.Timer.CutsceneStep;
import ca.zharry.MinecraftGamesServer.Timer.Timer;
import ca.zharry.MinecraftGamesServer.Utils.MusicManager;
import ca.zharry.MinecraftGamesServer.Utils.NMSHelper;
import ca.zharry.MinecraftGamesServer.Utils.Point3D;
import ca.zharry.MinecraftGamesServer.Utils.Zone;
import org.bukkit.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;

public class ServerElytraRun extends ServerInterface<PlayerElytraRun> {

    // Game config
    public static final int TIMER_STARTING = 60 * 20;
    public static final int TIMER_INPROGRESS = 15 * 60 * 20;
    public static final int TIMER_FINISHED = 45 * 20;

    // Server state
    public static final int ERROR = -1;
    public static final int GAME_WAITING = 0;
    public static final int GAME_STARTING = 1;
    public static final int GAME_INPROGRESS = 2;
    public static final int GAME_FINISHED = 3;
    public int state = ERROR;

    public Location practiceChooser = new Location(world, 10000.5, 64, 0.5);

    public static Zone[] tunnels = new Zone[] {
            new Zone().minZ(300).maxZ(2100),
            new Zone().minZ(3100).maxZ(4660),
            new Zone().minZ(5597).maxZ(7150)
    };

    public static Zone[] dangerZones = new Zone[] {
            new Zone().minZ(670).maxZ(2027).maxY(227),
            new Zone().minZ(3189).maxZ(4560).maxY(232),
            new Zone().minZ(5697).maxZ(7080).maxY(231)
    };

    public static Zone[] tunnelFinish = new Zone[] {
            new Zone().minZ(2027).maxZ(2100),
            new Zone().minZ(3100).maxZ(3189),
            new Zone().minZ(7080).maxZ(7150)
    };

    public static Zone endDrop = new Zone().minZ(7121).maxZ(7139).minX(262).maxX(280).maxY(10);

    public static Point3D[] tunnelAirlockStart = new Point3D[] {
            null,
            new Point3D(308.5, 223, 4583.5),
            new Point3D(313.5, 222, 5673.5)
    };

    public static Point3D[] tunnelAirlockEnd = new Point3D[] {
            new Point3D(345.5, 12, 2061.5),
            new Point3D(305.5, 12, 3154.5),
            null
    };

    public Location[] jumpPlatform = new Location[] {
            new Location(world, 317, 234, 673, 0, 0),
            new Location(world, 335.5, 232.5, 4561.5, 180, 0),
            new Location(world, 286.5, 231.5, 5695.5, 0, 0),
    };

    // Server tasks
    public Timer timerStartGame;
    public Timer timerInProgress;
    public Timer timerFinished;
    public Cutscene startGameTutorial;

    public ServerElytraRun(JavaPlugin plugin, World world, String minigame) {
        super(plugin, world, minigame);
        serverSpawn = new Location(world, 280.5, 240, 545.5);
//        NMSHelper.replaceChunkProvider(world);

        // Add existing players (for hot-reloading)
        for (PlayerInterface player : players) {
            player.teleport(serverSpawn);
            player.reset(GameMode.ADVENTURE);
        }

        timerStartGame = new Timer(plugin, "start", TIMER_STARTING) {
            @Override
            public void onStart() {
                state = GAME_STARTING;
                players.forEach(player -> {
                    giveElytra(player);
                    player.teleport(jumpPlatform[0]);
                    player.tunnel = 0;
                    player.dead = false;
                });
                MusicManager.Music music1 = new MusicManager.Music("tsf:music.glidermusic1", 140.8);
                MusicManager.Music music2 = new MusicManager.Music("tsf:music.glidermusic2", 140.8);
                MCGMain.musicManager.playMusicBackgroundSequence(p -> p.index == 0 ? music1 : music2);
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
        };

        timerInProgress = new Timer(plugin, "game", TIMER_INPROGRESS) {
            @Override
            public void onStart() {
                state = GAME_INPROGRESS;
            }

            @Override
            public void onTick() {
                elytraRunTick();
                countdownTimer(this, 11,
                        "Round ends in...",
                        "",
                        "",
                        "Round Over!");
            }

            @Override
            public void onEnd() {
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
                timerStartGame.set(TIMER_STARTING);
                timerInProgress.set(TIMER_INPROGRESS);
                timerFinished.set(TIMER_FINISHED);
            }
        };

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
                .title("Arrows will spawn here", "every 10 seconds", 60)
                .linear()
                .freeze(50));
        steps.add(new CutsceneStep(time += 75)
                .pos(10014.5, 4.5, 4, 0, 0)
                .title("Each kill is worth 125 points", "Winning the game will award alive team members 250 bonus points each!", 80)
                .linear());
        steps.add(new CutsceneStep(time += 60)
                .pos(10014.5, 4.5, 4, 0, 0)
                .linear());

        startGameTutorial = new Cutscene(plugin, this, steps) {
            @Override
            public void onStart() {
            }

            @Override
            public void onEnd() {
                timerStartGame.start();
            }
        };
    }

    public void elytraRunTick() {
        if(state == GAME_INPROGRESS) {
            for(PlayerElytraRun player : players) {
                if(player.inBlock) {
                    player.inBlockTimer ++;
                    if(player.inBlockTimer > 3) {
                        killPlayer(player);
                    }
                } else {
                    player.inBlockTimer = 0;
                }
            }
        }
    }

    public int getTunnel(Location location) {
        for(int i = 0; i < tunnels.length; ++i) {
            if(tunnels[i].contains(location)) {
                return i;
            }
        }
        return -1;
    }

    public void giveElytra(PlayerElytraRun player) {
        ItemStack item = new ItemStack(Material.ELYTRA, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        player.bukkitPlayer.getInventory().setChestplate(item);
    }

    public Location getPlayerStartLocation(PlayerElytraRun player) {
        if(player.tunnel < ServerElytraRun.tunnels.length) {
            return jumpPlatform[player.tunnel];
        } else {
            return practiceChooser;
        }
    }

    public double tunnelLength(int tunnel) {
        return dangerZones[tunnel].max.z - dangerZones[tunnel].min.z;
    }

    public double getPlayerDistance(PlayerElytraRun player, int tunnel) {
        double playerZ = player.getLocation().getZ();
        double tunnelMinZ = dangerZones[tunnel].min.z;
        double tunnelMaxZ = dangerZones[tunnel].max.z;
        double distance = playerZ - tunnelMinZ;
        distance = Math.min(Math.max(distance, 0), tunnelLength(tunnel));
        double jumpZ = jumpPlatform[tunnel].getZ();
        if(jumpZ - tunnelMinZ > tunnelMaxZ - jumpZ) {
            distance = tunnelLength(tunnel) - distance;
        }
        return distance;
    }

    public void killPlayer(PlayerElytraRun player) {
        if(!player.dead) {
            spawnDeathEffect(player);
            player.dead = true;
            player.inBlock = false;
            player.inBlockTimer = 0;
            new BukkitRunnable() {
                public void run() {
                    player.setHealth(0);
                }
            }.runTaskLater(plugin, 10);
        }
    }

    public void spawnDeathEffect(PlayerElytraRun player) {
        world.playSound(player.getLocation(), "tsf:glider.death", SoundCategory.PLAYERS, 1, 1);
        world.spawnParticle(Particle.EXPLOSION_HUGE, player.getLocation(), 10);
    }

    @Override
    public void onEnableCall() {
        super.onEnableCall();
        MCGMain.resourcePackManager.forceResourcePack("https://play.mcg-private.tk/test.zip", new File(MCGMain.resourcePackRoot, "test.zip"));
        this.state = GAME_WAITING;
//        MCGMain.protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.MAP_CHUNK) {
//            @Override
//            public void onPacketSending(PacketEvent event) {
//                PacketContainer packet = event.getPacket();
//                int x = packet.getIntegers().read(0);
//                int z = packet.getIntegers().read(1);
//                if(!(620 <= z && z <= 630)) {
//                    event.setCancelled(true);
//                }
//            }
//        });
//        NMSHelper.replaceChunkProvider(world);
    }

    @Override
    public void onDisableCall() {
        super.onDisableCall();
        this.state = ERROR;
//        NMSHelper.restoreChunkProvider(world);
    }

    @Override
    public void registerCommands() {
        plugin.getCommand("start").setExecutor(new CommandCutsceneStart(startGameTutorial));
        plugin.getCommand("timer").setExecutor(new CommandTimer(timerStartGame, timerInProgress, timerFinished));
    }

    @Override
    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(new ListenerElytraRun(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DisableHunger(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onWorldLoad(WorldLoadEvent e) {
                NMSHelper.replaceChunkProvider(e.getWorld());
            }
        }, plugin);
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
        world.setFullTime(0);
    }

    @Override
    public PlayerElytraRun createNewPlayerInterface(UUID uuid, String name) {
        return new PlayerElytraRun(this, uuid, name);
    }
}
