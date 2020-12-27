package ca.zharry.MinecraftGamesServer.Listeners;

import org.bukkit.GameRule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

public class ChangeGameRule implements Listener {
    public GameRule gamerule;
    public Object value;

    public ChangeGameRule(GameRule gamerule, Object value) {
        this.gamerule = gamerule;
        this.value = value;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        event.getWorld().setGameRule(gamerule, value);
    }
}
