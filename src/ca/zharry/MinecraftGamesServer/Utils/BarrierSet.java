package ca.zharry.MinecraftGamesServer.Utils;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;

public class BarrierSet {
	public ArrayList<Coord3D> blocks = new ArrayList<>();
	public BarrierSet fill(Coord3D begin, Coord3D end) {
		int minX = Math.min(begin.x, end.x);
		int minY = Math.min(begin.y, end.y);
		int minZ = Math.min(begin.z, end.z);

		int maxX = Math.max(begin.x, end.x);
		int maxY = Math.max(begin.y, end.y);
		int maxZ = Math.max(begin.z, end.z);
		for(int x = minX; x <= maxX; ++x) {
			for(int y = minY; y <= maxY; ++y) {
				for(int z = minZ; z <= maxZ; ++z) {
					blocks.add(new Coord3D(x, y, z));
				}
			}
		}
		return this;
	}

	public void setBarrier(World world) {
		barrier(world, true);
	}

	public void clearBarrier(World world) {
		barrier(world, false);
	}

	public void barrier(World world, boolean placeBarrier) {
		for(Coord3D coord : blocks) {
			Block block = world.getBlockAt(coord.x, coord.y, coord.z);
			if(placeBarrier) {
				if (block.getType() == Material.AIR) {
					block.setType(Material.BARRIER);
				}
			} else {
				if (block.getType() == Material.BARRIER) {
					block.setType(Material.AIR);
				}
			}
		}
	}
}
