package ca.zharry.MinecraftGamesServer.Utils;

import org.bukkit.Location;
import org.bukkit.World;

public class Point3D {
    public double x, y, z;

    public Point3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Point3D(Location loc) {
        this(loc.getX(), loc.getY(), loc.getZ());
    }

    public double distanceSquared() {
        return x * x + y * y + z * z;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public boolean equals(Object o) {
        Point3D p = (Point3D) o;
        if (o == null) {
            return false;
        }
        return x == p.x && y == p.y && z == p.z;
    }

    public int hashCode() {
        return java.lang.Double.hashCode(x) * 127 + java.lang.Double.hashCode(y) * 31 + java.lang.Double.hashCode(z);
    }

    public Point3D add(double x, double y, double z) {
        return new Point3D(this.x + x, this.y + y, this.z + z);
    }

    public Point3D subtract(Point3D p3d) {
        return new Point3D(this.x - p3d.x, this.y - p3d.y, this.z - p3d.z);
    }

    public Point3D multiply(double fac) {
         return new Point3D(this.x * fac, this.y * fac, this.z * fac);
    }

    public Location toLocation(World world, float yaw, float pitch) {
        return new Location(world, x, y, z, yaw, pitch);
    }

}
