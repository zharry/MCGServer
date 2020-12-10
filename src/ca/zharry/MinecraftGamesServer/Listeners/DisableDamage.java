package ca.zharry.MinecraftGamesServer.Listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class DisableDamage implements Listener {

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.VOID)
            e.setCancelled(true);
    }

}
