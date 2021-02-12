package ca.zharry.MinecraftGamesServer.Utils;

import ca.zharry.MinecraftGamesServer.Players.PlayerInterface;
import org.bukkit.Location;

public class Zone {
	public Point3D min, max;
	public Zone() {
		this(new Point3D(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY), new Point3D(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
	}

	public Zone(Point3D min, Point3D max) {
		this.min = min;
		this.max = max;
	}

	public Zone expand(double x, double y, double z) {
		min.x -= x;
		min.y -= y;
		min.z -= z;
		max.x += x;
		max.y += y;
		max.z += z;
		return this;
	}

	public Zone rangeX(double minX, double maxX) {
		min.x = minX;
		max.x = maxX;
		return this;
	}

	public Zone rangeY(double minY, double maxY) {
		min.y = minY;
		max.y = maxY;
		return this;
	}

	public Zone rangeZ(double minZ, double maxZ) {
		min.z = minZ;
		max.z = maxZ;
		return this;
	}

	public Zone minX(double minX) {
		min.x = minX;
		return this;
	}
	public Zone minY(double minY) {
		min.y = minY;
		return this;
	}
	public Zone minZ(double minZ) {
		min.z = minZ;
		return this;
	}

	public Zone maxX(double maxX) {
		max.x = maxX;
		return this;
	}
	public Zone maxY(double maxY) {
		max.y = maxY;
		return this;
	}
	public Zone maxZ(double maxZ) {
		max.z = maxZ;
		return this;
	}

	public boolean contains(PlayerInterface player) {
		return contains(player.getLocation());
	}
	public boolean contains(Location loc) {
		double x = loc.getX();
		double y = loc.getY();
		double z = loc.getZ();
		return min.x <= x && x <= max.x && min.y <= y && y <= max.y && min.z <= z && z <= max.z;
	}
}
