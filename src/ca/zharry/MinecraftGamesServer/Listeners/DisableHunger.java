package ca.zharry.MinecraftGamesServer.Listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class DisableHunger implements Listener {

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent e) {
        if (e.getFoodLevel() < 20) {
            e.setFoodLevel(20);
        }
    }

}
