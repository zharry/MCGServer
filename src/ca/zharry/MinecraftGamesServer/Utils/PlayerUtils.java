package ca.zharry.MinecraftGamesServer.Utils;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class PlayerUtils {
    public static void resetAllPlayers(World world, GameMode gamemode) {
        world.getPlayers().forEach(player -> resetPlayer(player, gamemode));
    }
    public static void resetPlayer(Player player, GameMode gamemode) {
        player.setGameMode(gamemode);
        player.getInventory().clear();
        player.setWalkSpeed(0.2f);
        player.setAbsorptionAmount(0);
        player.setHealth(20);
        player.setHealthScaled(false);
        player.setFoodLevel(20);
        player.setSaturation(5);
        player.setExhaustion(0);
        player.setExp(0);
        player.setTotalExperience(0);
        player.getActivePotionEffects().clear();
        player.setFallDistance(0);
        player.setInvisible(false);
    }

    @Deprecated // UNTESTED DANGEROUS
    public static void resetWorld(World world, String templateWorld) {
        String name = world.getName();
        Bukkit.unloadWorld(world, false);
        try {
            Files.delete(Paths.get(name));
            Files.copy(Paths.get(templateWorld), Paths.get(name), StandardCopyOption.REPLACE_EXISTING);
        } catch(Exception e) {
        }
        Bukkit.createWorld(WorldCreator.name(name));
    }
}
