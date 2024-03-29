package ca.zharry.MinecraftGamesServer.Servers;

import ca.zharry.MinecraftGamesServer.Commands.CommandStart;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimer;
import ca.zharry.MinecraftGamesServer.Listeners.DisableHunger;
import ca.zharry.MinecraftGamesServer.Listeners.ListenerElytraRun;
import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Players.PlayerElytraRun;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Players.PlayerParkour;
import ca.zharry.MinecraftGamesServer.Timer.Cutscene;
import ca.zharry.MinecraftGamesServer.Timer.CutsceneStep;
import ca.zharry.MinecraftGamesServer.Timer.Timer;
import ca.zharry.MinecraftGamesServer.Utils.*;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServerElytraRun extends ServerInterface<PlayerElytraRun> {

    // Game config
    public static final int TIMER_STARTING = 30 * 20;
    public static final int TIMER_STARTING_NEXT = 60 * 20;
    public static final int TIMER_INPROGRESS = 5 * 60 * 20;
    public static final int TIMER_FINISHED = 45 * 20;

    // Server state
    public static final int ERROR = -1;
    public static final int GAME_WAITING = 0;
    public static final int GAME_STARTING = 1;
    public static final int GAME_INPROGRESS = 2;
    public static final int GAME_FINISHED = 3;
    public int state = ERROR;
    public boolean startDone = false;

    public long roundStartTime;

    public BarrierSet barriers = new BarrierSet()
            .fill(new Coord3D(319, 238, 675), new Coord3D(314, 234, 675))
            .fill(new Coord3D(326, 228, 4559), new Coord3D(344, 246, 4559))
            .fill(new Coord3D(277, 245, 5697), new Coord3D(295, 227, 5697));

    public int tunnel = 0;

    public Location practiceChooser = new Location(world, 10000.5, 64, 0.5);

    public static Zone[] tunnels = new Zone[]{
            new Zone().minZ(300).maxZ(2100),
            new Zone().minZ(3100).maxZ(4660),
            new Zone().minZ(5597).maxZ(7150)
    };

    public static Zone[] dangerZones = new Zone[]{
            new Zone().minZ(670).maxZ(2027).maxY(227),
            new Zone().minZ(3189).maxZ(4560).maxY(232),
            new Zone().minZ(5697).maxZ(7080).maxY(231)
    };

    public static Zone[] tunnelFinish = new Zone[]{
            new Zone().minZ(2027).maxZ(2100),
            new Zone().minZ(3100).maxZ(3189),
            new Zone().minZ(7080).maxZ(7150)
    };

    public static Zone endDrop = new Zone().minZ(7121).maxZ(7139).minX(262).maxX(280).maxY(10);

    public static Point3D[] tunnelAirlockStart = new Point3D[]{
            null,
            new Point3D(308.5, 223, 4583.5),
            new Point3D(313.5, 222, 5673.5)
    };

    public static Point3D[] tunnelAirlockEnd = new Point3D[]{
            new Point3D(345.5, 12, 2061.5),
            new Point3D(305.5, 12, 3154.5),
            null
    };

    public Location[] jumpPlatform = new Location[]{
            new Location(world, 317, 234, 673, 0, 0),
            new Location(world, 335.5, 232.5, 4561.5, 180, 0),
            new Location(world, 286.5, 231.5, 5695.5, 0, 0),
    };

    public Location[] startingLocation = new Location[]{
            jumpPlatform[0],
            tunnelAirlockStart[1].toLocation(world, -90, 0),
            tunnelAirlockStart[2].toLocation(world, 90, 0),
    };

    public Location oldSpawn = new Location(world, 280.5, 240, 545.5);
    public Location endArea = new Location(world, 290, 232, 676.5);

    // Server tasks
    public Timer timerStartGame;
    public Timer timerInProgress;
    public Timer timerFinished;
    public Cutscene startGameTutorial;

    public static ArrayList<Point3D> hints;

    void loadHintPaths() {
        hints = new ArrayList<>();
        for (int i = 0; i < tunnels.length; i++) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(ServerElytraRun.class.getResourceAsStream("/ElytraRun/hints-" + i + ".txt")));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] coords = line.split(",");
                    double x = Double.parseDouble(coords[0]), y = Double.parseDouble(coords[1]), z = Double.parseDouble(coords[2]);

                    hints.add(new Point3D(x, y, z));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public ServerElytraRun(JavaPlugin plugin, World world, String minigame) {
        super(plugin, world, minigame);

        loadHintPaths();

        final int HINT_DISTANCE = 16 * 5;
        new BukkitRunnable() {
            @Override
            public void run() {
                for (PlayerElytraRun p : players) {
                    if (!p.hintsEnabled) continue;
                    Point3D pos = new Point3D(p.getLocation());
                    Vector v = p.bukkitPlayer.getVelocity();
                    int tunnel = getTunnel(p.getLocation());
                    if(tunnel != -1) {
                        double sign = Math.signum(tunnelFinish[tunnel].min.z - jumpPlatform[tunnel].getZ());
                        for (int i = 0; i < hints.size() - 1; ++i) {
                            Point3D hint = hints.get(i);
                            Point3D dist = hint.subtract(pos).multiply(sign);
                            if (dist.z > 0 && dist.z < HINT_DISTANCE) {
                                Point3D vec = hints.get(i + 1).subtract(hint).multiply(0.1);
                                p.bukkitPlayer.spawnParticle(Particle.FLAME, hint.x, hint.y, hint.z, 0, vec.x, vec.y, vec.z, 1);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 10);

//        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this.plugin, PacketType.Play.Client.SPECTATE) {
//            @Override
//            public void onPacketReceiving(PacketEvent event) {
//                System.out.println("SPECTATE " + event.getPacket().getUUIDs().read(0));
//            }
//        });

        // CRASHES MINECRAFT CLIENT
//        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this.plugin, PacketType.Play.Server.UNLOAD_CHUNK) {
//            @Override
//            public void onPacketSending(PacketEvent event) {
//                event.setCancelled(true);
//            }
//        });

        serverSpawn = startingLocation[0];

        // Add existing players (for hot-reloading)
        for (PlayerInterface player : players) {
//            player.teleport(serverSpawn);
            player.reset(GameMode.ADVENTURE);
        }
        barriers.clearBarrier(world);

        timerStartGame = new Timer(plugin, "start", TIMER_STARTING) {
            @Override
            public void onStart() {
                state = GAME_STARTING;
                if (!startDone) {
                    startDone = true;

                    sendTitleAll("Good luck, have fun!", "The game will begin in 30 seconds!");
                    sendMultipleMessageAll(new String[]{
                            ChatColor.GREEN + "" + ChatColor.BOLD + "Here's a recap:\n" + ChatColor.RESET +
                                    "This map is " + ChatColor.BOLD + "Terra Swoop Force" + ChatColor.RESET + ", by Noxcrew\n" +
                                    " \n" +
                                    " \n",
                            ChatColor.GREEN + "" + ChatColor.BOLD + "How to play:\n" + ChatColor.RESET +
                                    "1. Use your elytra to fly through the tunnels \n" +
                                    "2. Finishing gives you 400 points!\n" +
                                    "3. You get 50 points for every person you come in front of\n" +
                                    "4. Go as far as you can (you can still get points for partial completion)!",
                    }, new int[]{
                            120,
                            45,
                    });

                    barriers.setBarrier(world);
                    players.forEach(player -> {
                        for (int i = 0; i < 3; i++) {
                            player.maxDistance[i] = 0;
                            player.completedTime[i] = Integer.MAX_VALUE;
                        }
                    });
                    offlinePlayers.forEach(offlinePlayer -> {
                        for (int i = 0; i < 3; i++) {
                            offlinePlayer.maxDistance[i] = 0;
                            offlinePlayer.completedTime[i] = Integer.MAX_VALUE;
                        }
                    });
                }

                barriers.setBarrier(world);
                players.forEach(player -> {
                    player.reset(GameMode.ADVENTURE);
                    giveInventory(player);
                    player.teleport(startingLocation[tunnel]);
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
                        ChatColor.GREEN + "Go!");
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
                barriers.clearBarrier(world);
                roundStartTime = System.nanoTime();
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
                elytraRunRoundEnd();
                barriers.setBarrier(world);

                tunnel++;
                if (tunnel < tunnels.length) {
                    timerStartGame.set(TIMER_STARTING_NEXT);
                    timerInProgress.set(TIMER_INPROGRESS);
                    timerStartGame.start();
                } else {
                    elytraRunEnd();
                    timerFinished.start();
                }
            }
        };

        timerFinished = new Timer(plugin, "finished", TIMER_FINISHED) {
            @Override
            public void onStart() {
                state = GAME_FINISHED;
                commitAllPlayers();
                for (PlayerInterface player : players) {
                    player.teleport(jumpPlatform[0]);
                }
            }

            @Override
            public void onTick() {
            }

            @Override
            public void onEnd() {
                sendTitleAll("Joining Lobby...", "", 5, 20, 30);
                MCGMain.bungeeManager.sendAllPlayers(MCGMain.lobbyId, false, true);

                barriers.clearBarrier(world);
                tunnel = 0;
                state = GAME_WAITING;
                startDone = false;
                timerStartGame.set(TIMER_STARTING);
                timerInProgress.set(TIMER_INPROGRESS);
                timerFinished.set(TIMER_FINISHED);
            }
        };

        ArrayList<CutsceneStep> steps = new ArrayList<>();
        int time = 0;
        steps.add(new CutsceneStep(time)
                .pos(317.5, 240, 679.5, 0, 90)
                .title("Welcome to ElytraRun", "Map (Terra Swoop Force) made by Noxcrew", 60)
                .freeze(20));
        steps.add(new CutsceneStep(time += 30)
                .pos(317.5, 227, 679.5, 0, 45)
                .linear());
        steps.add(new CutsceneStep(time += 20)
                .pos(316.5, 212, 688.5, 0, 10)
                .linear());
        steps.add(new CutsceneStep(time += 60)
                .pos(315.5, 204, 753.5, 0, 12)
                .title("Fly through the tunnels", "", 60)
                .linear());
        steps.add(new CutsceneStep(time += 60)
                .pos(314.2, 194, 850.5, -10, 10)
                .title("Avoid hitting obstacles", "Avoid the ground too", 60)
                .linear().freeze(60));
        steps.add(new CutsceneStep(time += 60)
                .pos(349.5, 47, 1898, 13, 16)
                .title("Race to the finish", "Or as far as you can go", 60)
                .freeze(10));
        steps.add(new CutsceneStep(time += 90)
                .pos(318, 21, 2055, 2, 20)
                .title("You get 50 points per player you beat", "And 300 points per stage you complete!", 60)
                .linear().freeze(90));

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
        if (state == GAME_INPROGRESS) {
            for (PlayerElytraRun player : players) {
                if (player.inBlock) {
                    player.inBlockTimer++;
                    if (player.inBlockTimer > 3) {
                        killPlayer(player);
                    }
                } else {
                    player.inBlockTimer = 0;
                }
            }
        }
    }

    public void elytraRunRoundEnd() {
        List<PlayerElytraRun> sortedPlayers = getSortedPlayers(this.tunnel);

        int prizePool = 0;
        int position = sortedPlayers.size();
        for (PlayerElytraRun player : sortedPlayers) {
            player.addScore(prizePool += 50, "finished " + --position);
            if(player.bukkitPlayer != null) {
                player.bukkitPlayer.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "[+" + prizePool + "] " +
                        ChatColor.RESET + "you completed tunnel " + (this.tunnel + 1) + " in position " + (position + 1) + "/" + sortedPlayers.size());
            }
        }

        String roundPlacements = "";
        Collections.reverse(sortedPlayers);
        int count = 0;
        for (PlayerElytraRun player : sortedPlayers) {
            roundPlacements += ChatColor.RESET + "" + (count + 1) + ". ";
            if (player.maxDistance[this.tunnel] == Double.POSITIVE_INFINITY)
                roundPlacements += player.getDisplayName() + "" + ChatColor.RESET + " - " + ListenerElytraRun.timeToString(player.completedTime[this.tunnel]) + "s\n";
            else
                roundPlacements += player.getDisplayName() + "" + ChatColor.RESET + " - " + Math.round(100.0 * player.maxDistance[this.tunnel] / tunnelLength(this.tunnel)) + "%\n";
            if (++count > 5) {
                break;
            }
        }

        sendMessageAll("");
        sendMessageAll(ChatColor.BOLD + "Stage " + (this.tunnel+1) + " Placements:\n" + roundPlacements + "\n");
        sendMessageAll("");
    }

    public void elytraRunEnd() {
        ArrayList<PlayerElytraRun> playerElytraRuns = new ArrayList<>();
        for (PlayerElytraRun player : players) {
            playerElytraRuns.add(player);
            player.commit();
        }

        String topTimes = "";
        for (int s = 0; s < 3; s++) {
            List<PlayerElytraRun> sortedPlayers = getSortedPlayers(s);
            Collections.reverse(sortedPlayers);

            topTimes += ChatColor.RESET + "Stage " + (s+1) + ": ";
            PlayerElytraRun stageWinner = sortedPlayers.get(0);
            if (stageWinner.maxDistance[s] == Double.POSITIVE_INFINITY)
                topTimes += stageWinner.getDisplayName() + "" + ChatColor.RESET + " - " + ListenerElytraRun.timeToString(stageWinner.completedTime[s]) + "s\n";
            else
                topTimes += stageWinner.getDisplayName() + "" + ChatColor.RESET + " - " + Math.round(100.0 * stageWinner.maxDistance[s] / tunnelLength(s)) + "%\n";
        }

        String topPlayers = "";
        int count = 0;
        playerElytraRuns.sort(Comparator.comparingInt(o -> -o.getCurrentScore()));
        for (PlayerElytraRun player : playerElytraRuns) {
            topPlayers += ChatColor.RESET + "[" + player.getCurrentScore() + "] " + player.getDisplayName() + "\n";
            if (++count > 5) {
                break;
            }
        }

        String topTeams = "";
        ArrayList<MCGTeam> teamElytraRuns = getRealTeams();
        teamElytraRuns.sort(Comparator.comparingInt(o -> -o.getScore("elytrarun")));
        for (MCGTeam team : teamElytraRuns) {
            topTeams += ChatColor.RESET + "[" + team.getScore("elytrarun") + "] " + team.chatColor + "" + team.teamname + "\n";
        }

        sendMultipleMessageAll(new String[]{
                ChatColor.BOLD + "Best Stage Times:\n" + topTimes + " \n",
                ChatColor.BOLD + "Top Scoring Players:\n" + topPlayers + " \n",
                ChatColor.BOLD + "Final Team Score for Elytra Run:\n" + topTeams,
        }, new int[]{
                10,
                60,
                100,
        });
    }

    public List<PlayerElytraRun> getSortedPlayers(int tunnel) {
        return Stream.concat(players.stream(), offlinePlayers.stream())
                .filter(p -> p.isOnline() || p.maxDistance[tunnel] > 1e-9)
                .sorted((a, b) -> {
                    double aDist = a.maxDistance[tunnel];
                    long aTime = a.completedTime[tunnel];
                    double bDist = b.maxDistance[tunnel];
                    long bTime = b.completedTime[tunnel];

                    if (aDist == Double.POSITIVE_INFINITY && bDist == Double.POSITIVE_INFINITY)
                        return Long.compare(bTime, aTime);
                    return Double.compare(aDist, bDist);
                })
                .collect(Collectors.toList());
    }

    public int getTunnel(Location location) {
        for (int i = 0; i < tunnels.length; ++i) {
            if (tunnels[i].contains(location)) {
                return i;
            }
        }
        return -1;
    }

    public void giveInventory(PlayerElytraRun player) {
        player.bukkitPlayer.getInventory().clear();

        ItemStack item = new ItemStack(Material.ELYTRA, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        player.bukkitPlayer.getInventory().setChestplate(item);

        ItemStack waypoints = new ItemStack(Material.END_ROD, 1);
        ItemMeta waypointsItemMeta = waypoints.getItemMeta();
        waypointsItemMeta.setDisplayName(ChatColor.GREEN + "Toggle in-game hints");
        waypoints.setItemMeta(waypointsItemMeta);
        player.bukkitPlayer.getInventory().setItem(0, waypoints);

        if (state == GAME_WAITING) {
            ItemStack levelSelect = new ItemStack(Material.TARGET, 1);
            meta = levelSelect.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + "Level select");
            levelSelect.setItemMeta(meta);
            player.bukkitPlayer.getInventory().setItem(8, levelSelect);
        }

    }

    public Location getPlayerStartLocation(PlayerElytraRun player) {
        if(state == GAME_WAITING) {
            return jumpPlatform[player.lastTunnel];
        } else {
            return jumpPlatform[tunnel];
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
        if (jumpZ - tunnelMinZ > tunnelMaxZ - jumpZ) {
            distance = tunnelLength(tunnel) - distance;
        }
        return distance;
    }

    public void killPlayer(PlayerElytraRun player) {
        if (!player.dead) {
            spawnDeathEffect(player);
            player.dead = true;
            player.inBlock = false;
            player.inBlockTimer = 0;
            player.bukkitPlayer.getInventory().clear();
            player.bukkitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20, 0, false, false, false));
            new BukkitRunnable() {
                public void run() {
                    player.setHealth(0);
                }
            }.runTaskLater(plugin, 10);
        }
    }

    public void spawnDeathEffect(PlayerElytraRun player) {
        world.playSound(player.getLocation(), "tsf:glider.death", SoundCategory.PLAYERS, 0.5f, 1);
        world.spawnParticle(Particle.EXPLOSION_LARGE, player.getLocation(), 5);
        player.bukkitPlayer.spawnParticle(Particle.EXPLOSION_HUGE, player.getLocation(), 10);
    }

    @Override
    public void onEnableCall() {
        super.onEnableCall();
        MCGMain.resourcePackManager.forceResourcePack("http://play.mcg-private.tk:25599/resource.zip", new File(MCGMain.resourcePackRoot, "resource.zip"));
        this.state = GAME_WAITING;

        for (PlayerElytraRun p : players){
            giveInventory(p);
        }
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
        plugin.getCommand("start").setExecutor(new CommandStart(this, startGameTutorial));
        plugin.getCommand("timer").setExecutor(new CommandTimer(this, timerStartGame, timerInProgress, timerFinished));
    }

    @Override
    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(new ListenerElytraRun(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DisableHunger(), plugin);
//        plugin.getServer().getPluginManager().registerEvents(new Listener() {
//            @EventHandler
//            public void onWorldLoad(WorldLoadEvent e) {
//                NMSHelper.replaceChunkProvider(e.getWorld());
//            }
//        }, plugin);
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
