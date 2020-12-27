package ca.zharry.MinecraftGamesServer.Utils;

import org.bukkit.Location;

public class Coord3D {
	public int x, y, z;
	public Coord3D(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Coord3D(Location location) {
		this.x = (int) Math.round(location.getX());
		this.y = (int) Math.round(location.getY());
		this.z = (int) Math.round(location.getZ());
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	public boolean equals(Object o) {
		Coord3D p = (Coord3D) o;
		if(o == null) {
			return false;
		}
		return x == p.x && y == p.y && z == p.z;
	}

	public int hashCode() {
		return Integer.hashCode(x) * 127 + Integer.hashCode(y) * 31 + Integer.hashCode(z);
	}

	public String toString() {
		return "Coord3D[x=" + x + ", y=" + y + ", z=" + z + "]";
	}

}
