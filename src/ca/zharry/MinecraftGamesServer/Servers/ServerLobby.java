package ca.zharry.MinecraftGamesServer.Servers;

import ca.zharry.MinecraftGamesServer.Commands.CommandLobbySetNextGame;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerPause;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerResume;
import ca.zharry.MinecraftGamesServer.Commands.CommandTimerSet;
import ca.zharry.MinecraftGamesServer.Listeners.DisableDamage;
import ca.zharry.MinecraftGamesServer.Listeners.DisableHunger;
import ca.zharry.MinecraftGamesServer.Listeners.ListenerLobby;
import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import ca.zharry.MinecraftGamesServer.Players.PlayerLobby;
import ca.zharry.MinecraftGamesServer.Timer.Timer;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class ServerLobby extends ServerInterface {

    // Game config
    public static final int TIMER_START = 60 * 20;

    // Server states
    public static final int ERROR = -1;
    public static final int LOBBY_WAITING = 0;
    public static final int LOBBY_STARTED = 1;
    public int state = ERROR;
    public String nextMinigame;

    // Server tasks
    public Timer timerNextGame;

    public ServerLobby(JavaPlugin plugin) {
        super(plugin);
        serverSpawn = new Location(world, 1484.5, 4, 530, 90, 0);

        timerNextGame = new Timer(plugin) {
            @Override
            public void onStart() {
                state = LOBBY_STARTED;
                sendTitleAll(minigames.get(nextMinigame), "is starting in 60 seconds!");
            }

            @Override
            public void onTick() {
                countdownTimer(this, 10,
                        "",
                        "",
                        "Teleporting to " + minigames.get(nextMinigame),
                        "Loading " + minigames.get(nextMinigame) + "...");
            }

            @Override
            public void onEnd() {
                sendPlayersToGame(nextMinigame);

                state = LOBBY_WAITING;
                timerNextGame.set(TIMER_START);
            }
        }.set(TIMER_START);
    }

    public enum VanillaClickType {
        PICKUP,
        QUICK_MOVE,
        SWAP,
        CLONE,
        THROW,
        QUICK_CRAFT,
        PICKUP_ALL
    }

    @Override
    public void onEnableCall() {
        super.onEnableCall();
        this.state = LOBBY_WAITING;
        MCGMain.protocolManager.addPacketListener(new PacketAdapter(this.plugin, PacketType.Play.Client.WINDOW_CLICK) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                PacketContainer packet = event.getPacket();
//                System.out.println(packet);
                int containerId = packet.getIntegers().read(0);
                int slotNum = packet.getIntegers().read(1);
                int buttonNum = packet.getIntegers().read(2);
                short uid = packet.getShorts().read(0);
                ItemStack itemStack = packet.getItemModifier().read(0);
                VanillaClickType clickType = packet.getEnumModifier(VanillaClickType.class, 5).read(0);
                System.out.println(String.format("ContainerId=%d, slotId=%d, buttonNum=%d, uid=%d, itemstack=%s, clickType=%s", containerId, slotNum, buttonNum, uid, itemStack.toString(), clickType.toString()));
            }
        });
    }

    @Override
    public void onDisableCall() {
        super.onDisableCall();
        this.state = ERROR;
    }

    @Override
    public void registerCommands() {
        plugin.getCommand("setgame").setExecutor(new CommandLobbySetNextGame(this, timerNextGame));
        plugin.getCommand("timernextset").setExecutor(new CommandTimerSet(timerNextGame));
        plugin.getCommand("timernextpause").setExecutor(new CommandTimerPause(timerNextGame));
        plugin.getCommand("timernextresume").setExecutor(new CommandTimerResume(timerNextGame));
    }

    @Override
    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(new ListenerLobby(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DisableHunger(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DisableDamage(), plugin);
    }

    @Override
    public PlayerInterface createNewPlayerInterface(UUID uuid, String name) {
        return new PlayerLobby(this, uuid, name);
    }
}
