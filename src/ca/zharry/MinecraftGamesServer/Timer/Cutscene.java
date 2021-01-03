package ca.zharry.MinecraftGamesServer.Timer;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Servers.ServerInterface;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public abstract class Cutscene {

    private TreeMap<Integer, CutsceneStep> positions = new TreeMap<>();
    private TreeMap<Integer, CutsceneStep> events = new TreeMap<>();

    private Plugin plugin;
    private ServerInterface server;

    private HashMap<PlayerInterface, ActivePlayer> activePlayers = new HashMap<>();
    public int entityId;

    public int cutsceneLength;
    public int currentTick = Integer.MAX_VALUE;

    public MoveCameraTimer cameraMover;
    public PlayerJoinQuitListener joinQuitListener;

    public double lastX, lastY, lastZ, lastYaw, lastPitch;
    public int playbackSpeed = 1;

    public Cutscene(Plugin plugin, ServerInterface server, ArrayList<CutsceneStep> steps) {
        this.plugin = plugin;
        this.server = server;

        for(CutsceneStep step : steps) {
            if(step.hasPosition) {
                // Add player eye offset, take away boat eye offset
                step.y = step.y + 1.8 * 0.85 - 0.5625;
                this.positions.put(step.timestamp, step);
                currentTick = Math.min(currentTick, step.timestamp);
                if(step.freeTick != 0) {
                    int newTimestamp = step.timestamp + step.freeTick;
                    this.positions.put(newTimestamp, new CutsceneStep(newTimestamp).pos(step.x, step.y, step.z, step.yaw, step.pitch).jumplerp());
                }
            }
            this.events.put(step.timestamp, step);
            cutsceneLength = Math.max(cutsceneLength, step.timestamp + step.titleDelay + 20);
        }

        this.entityId = -6900000;
    }

    public abstract void onStart();

    public abstract void onEnd();

    public void start() {
        onStart();

        // Cache players
//        activePlayers.clear();
        for(PlayerInterface player : server.players) {
//            activePlayers.put(player, new ActivePlayer(player));
//            try {
//                player.bukkitPlayer.getGameMode();
//                player.bukkitPlayer.setGameMode(GameMode.SPECTATOR);
//            } catch(Exception e) {
//                e.printStackTrace();
//            }
            addPlayerToCutscene(player);
        }
//
//        // Hide all cutscene players from each other
//        for(PlayerInterface player1 : activePlayers.keySet()) {
//            for(PlayerInterface player2 : activePlayers.keySet()) {
//                player1.bukkitPlayer.hidePlayer(plugin, player2.bukkitPlayer);
//            }
//        }

        joinQuitListener = new PlayerJoinQuitListener();
        plugin.getServer().getPluginManager().registerEvents(joinQuitListener, plugin);

        cameraMover = new MoveCameraTimer();
        cameraMover.runTaskTimer(plugin, 0, 1);

//        for (int i = 0; i < steps.size(); i++) {
//            int iteration = i;
//            new BukkitRunnable() {
//                @Override
//                public void run() {
//                    // Send title message
//                    server.sendTitleAll(steps.get(iteration).title, steps.get(iteration).subtitle, 10, steps.get(iteration).titleDelay, 10);
//
//
//                    Location location = steps.get(iteration).location;
//
//
//
//                    // Teleport all players
//                    for (PlayerInterface p : playingForPlayer) {
//                        p.bukkitPlayer.teleport(steps.get(iteration).location);
//                        if(!p.isOnline()) {
//                            continue;
//                        }
//                        try {
//                            MCGMain.protocolManager.sendServerPacket(p.bukkitPlayer, teleportEntity, false);
//                            System.out.println("sent");
//                        } catch(Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            }.runTaskLater(plugin, steps.get(iteration).delay);
//        }
//
//        new BukkitRunnable() {
//            @Override
//            public void run() {
//
//
//                onEnd();
//            }
//        }.runTaskLater(plugin, steps.get(steps.size() - 1).delay + steps.get(steps.size() - 1).titleDelay + 40);
    }

    public void end() {
        if(cameraMover != null) {
            spectateEntity(false);
            removeFakeEntity();
            ArrayList<PlayerInterface> playersCopy = new ArrayList<>(activePlayers.keySet());
            for (PlayerInterface player : playersCopy) {
                removePlayerFromCutscene(player);
            }
            cameraMover.cancel();
            HandlerList.unregisterAll(joinQuitListener);
            joinQuitListener = null;
            cameraMover = null;
            onEnd();
        }
    }

    public void addPlayerToCutscene(PlayerInterface player) {
        if(player.cutscene != null) {
            player.cutscene.removePlayerFromCutscene(player);
        }
        // Hide player from all existing cutscene viewers
        for(PlayerInterface player2 : activePlayers.keySet()) {
            player.bukkitPlayer.hidePlayer(plugin, player2.bukkitPlayer);
            player2.bukkitPlayer.hidePlayer(plugin, player.bukkitPlayer);
        }

        player.cutscene = this;
        ActivePlayer activePlayer = new ActivePlayer(player);
        player.bukkitPlayer.setGameMode(GameMode.SPECTATOR);
        player.cutscene = this;
        if(cameraMover != null) {
            teleportPlayerNMS(player.bukkitPlayer, new Location(player.bukkitPlayer.getWorld(), lastX, lastY, lastZ, (float) lastYaw, (float) lastPitch));
            spawnFakeEntity(player, lastX, lastY, lastZ, lastYaw, lastPitch);
            spectateEntity(player, true);
        }
        activePlayers.put(player, activePlayer);
    }

    public void removePlayerFromCutscene(PlayerInterface player) {
        if(activePlayers.containsKey(player)) {
            // Show players to all existing cutscene viewers
            for(PlayerInterface player2 : activePlayers.keySet()) {
                player.bukkitPlayer.showPlayer(plugin, player2.bukkitPlayer);
                player2.bukkitPlayer.showPlayer(plugin, player.bukkitPlayer);
            }
            ActivePlayer activePlayer = activePlayers.get(player);
            activePlayer.player.bukkitPlayer.teleport(activePlayer.startLocation);
            activePlayer.player.bukkitPlayer.setGameMode(activePlayer.startGameMode);
            player.cutscene = null;
            activePlayers.remove(player);
        }
    }

    public void spawnFakeEntity(PlayerInterface player, double x, double y, double z, double yaw, double pitch) {
        PacketContainer createEntity = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);
        // id
        createEntity.getIntegers().write(0, entityId);
        // uuid
        createEntity.getUUIDs().write(0, UUID.randomUUID());
        // position
        createEntity.getDoubles().write(0, x).write(1, y).write(2, z);
        // delta movement
        createEntity.getIntegers().write(1, 0).write(2, 0).write(3, 0);
        // rotation
        createEntity.getIntegers().write(4, (int) Math.round(pitch * 256.0 / 360.0)).write(5, (int) Math.round(yaw * 256.0 / 360.0));
        // type
        createEntity.getEntityTypeModifier().write(0, EntityType.BOAT);
        // data
        createEntity.getIntegers().write(6, 0);

        try {
            MCGMain.protocolManager.sendServerPacket(player.bukkitPlayer, createEntity, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void spawnFakeEntity(double x, double y, double z, double yaw, double pitch) {
        for(PlayerInterface player : activePlayers.keySet()) {
            spawnFakeEntity(player, x, y, z, yaw, pitch);
        }
    }

    public void spectateEntity(PlayerInterface player, boolean spectate) {
        try {
            PacketContainer changeCamera = new PacketContainer(PacketType.Play.Server.CAMERA);
            changeCamera.getIntegers().write(0, spectate ? entityId : player.bukkitPlayer.getEntityId());
            MCGMain.protocolManager.sendServerPacket(player.bukkitPlayer, changeCamera, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void spectateEntity(boolean spectate) {
        for(PlayerInterface player : activePlayers.keySet()) {
            spectateEntity(player, spectate);
        }
    }

    public void teleportPlayerNMS(Player player, Location location) {
        try {
            Object handle = player.getClass().getDeclaredMethod("getHandle").invoke(player);
            Object playerConnection = handle.getClass().getDeclaredField("playerConnection").get(handle);
            playerConnection.getClass().getDeclaredMethod("teleport", Location.class).invoke(playerConnection, location);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void teleportEntity(double x, double y, double z, double yaw, double pitch, boolean isCut) {
        PacketContainer teleportEntity = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
        // id
        teleportEntity.getIntegers().write(0, entityId);
        // position
        teleportEntity.getDoubles().write(0, x).write(1, y).write(2, z);
        // rotation
        teleportEntity.getBytes().write(0, (byte) Math.round(yaw * 256.0 / 360.0)).write(1, (byte) Math.round(pitch * 256.0 / 360.0));

        if(isCut) {
            System.out.println("Cut to " + x + ", " + y + ", " + z);
            for(PlayerInterface player : activePlayers.keySet()) {
                spectateEntity(false);
                spawnFakeEntity(x, y, z, yaw, pitch);
                try {
                    MCGMain.protocolManager.sendServerPacket(player.bukkitPlayer, teleportEntity, false);
                } catch(Exception e) {
                    e.printStackTrace();
                }
                teleportPlayerNMS(player.bukkitPlayer, new Location(server.world, x, y, z, (float) yaw, (float) pitch));
                spectateEntity(true);
            }
        }

        for(PlayerInterface player : activePlayers.keySet()) {
            try {
                MCGMain.protocolManager.sendServerPacket(player.bukkitPlayer, teleportEntity, false);
                teleportPlayerNMS(player.bukkitPlayer, new Location(server.world, x, y, z, (float) yaw, (float) pitch));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void removeFakeEntity() {
        PacketContainer destroy_entity = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
        destroy_entity.getIntegerArrays().write(0, new int[]{entityId});
        for(PlayerInterface player : activePlayers.keySet()) {
            try {
                MCGMain.protocolManager.sendServerPacket(player.bukkitPlayer, destroy_entity, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static class ActivePlayer {
        public PlayerInterface player;
        public GameMode startGameMode;
        public Location startLocation;
        public ActivePlayer(PlayerInterface player) {
            this.player = player;
            startGameMode = player.bukkitPlayer.getGameMode();
            startLocation = player.bukkitPlayer.getLocation();
        }
    }

    private class MoveCameraTimer extends BukkitRunnable {
        public void run() {
            for(int tickcnt = 0; tickcnt < playbackSpeed; ++tickcnt) {
                try {
                    Map.Entry<Integer, CutsceneStep> curr = positions.floorEntry(currentTick); // last entry with timestamp <= currentTick
                    Map.Entry<Integer, CutsceneStep> next = positions.ceilingEntry(currentTick + 1); // first entry with timestamp > currentTick
                    if (next == null) {
                        if (currentTick > cutsceneLength) {
                            end();
                            return;
                        }
                        next = curr;
                    }
                    int timeDelta = next.getKey() - curr.getKey();
                    double interpFactor = timeDelta == 0 ? 0 : (double) (currentTick - curr.getKey()) / timeDelta;

                    if (next.getValue().transition == CutsceneStep.Transition.CUT || next.getValue().transition == CutsceneStep.Transition.JUMP_LERP) {
                        interpFactor = 0;
                    }

                    boolean isCut = false;
                    if (curr.getKey() == currentTick && curr.getValue().transition == CutsceneStep.Transition.CUT) {
                        System.out.println("Cut");
                        isCut = true;
                    }

                    double x = next.getValue().x * interpFactor + curr.getValue().x * (1 - interpFactor);
                    double y = next.getValue().y * interpFactor + curr.getValue().y * (1 - interpFactor);
                    double z = next.getValue().z * interpFactor + curr.getValue().z * (1 - interpFactor);

                    double nextYaw = next.getValue().yaw;
                    double currYaw = curr.getValue().yaw;

                    if (nextYaw - currYaw > 180) {
                        nextYaw -= 360;
                    }
                    if (currYaw - nextYaw > 180) {
                        currYaw -= 360;
                    }

                    double yaw = nextYaw * interpFactor + currYaw * (1 - interpFactor);
                    yaw = yaw % 360;

                    if (yaw >= 180) {
                        yaw -= 360;
                    }

                    if (yaw < -180) {
                        yaw += 360;
                    }
                    double pitch = next.getValue().pitch * interpFactor + curr.getValue().pitch * (1 - interpFactor);

                    lastX = x;
                    lastY = y;
                    lastZ = z;
                    lastYaw = yaw;
                    lastPitch = pitch;

                    teleportEntity(x, y, z, yaw, pitch, isCut);

                    CutsceneStep event = events.get(currentTick);
                    if (event != null) {
                        if (event.title != null) {
                            for (PlayerInterface player : activePlayers.keySet()) {
                                player.bukkitPlayer.sendTitle(event.title, event.subtitle, 10, event.titleDelay, 10);
                            }
                        }
                        if (event.executor != null) {
                            event.executor.run(event);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                currentTick += 1;
            }
        }
    }

    public class PlayerJoinQuitListener implements Listener {
        @EventHandler(priority = EventPriority.HIGHEST)
        public void onPlayerJoin(PlayerJoinEvent event) {
            PlayerInterface player = server.playerLookup.get(event.getPlayer().getUniqueId());
            addPlayerToCutscene(player);
        }
        @EventHandler(priority = EventPriority.LOWEST)
        public void onPlayerQuit(PlayerQuitEvent event) {
            PlayerInterface player = server.playerLookup.get(event.getPlayer().getUniqueId());
            removePlayerFromCutscene(player);
        }
    }

}
