package ca.zharry.MinecraftGamesServer.Utils;

import org.bukkit.Location;

public class LocationUtils {
	public static boolean positionEquals(Location l1, Location l2) {
		return l1.getWorld().equals(l2.getWorld()) && l1.getX() == l2.getX() && l1.getY() == l2.getY() && l1.getZ() == l2.getZ();
	}
}
