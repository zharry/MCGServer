package ca.zharry.MinecraftGamesServer.Utils;

import net.minecraft.server.v1_16_R3.*;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class NMSHelper {
	public static void teleport(Player player, double x, double y, double z, float yaw, float pitch, boolean relX, boolean relY, boolean relZ, boolean relYaw, boolean relPitch) {
		int tpRelative = (relX ? 1 : 0) | (relY ? 2 : 0) | (relZ ? 4 : 0) | (relYaw ? 8 : 0) | (relPitch ? 16 : 0);
		EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
		if (relX) x += nmsPlayer.locX();
		if (relY) y += nmsPlayer.locY();
		if (relZ) z += nmsPlayer.locZ();
		if (relYaw) yaw += nmsPlayer.yaw;
		if (relPitch) pitch += nmsPlayer.pitch;

		float matrix_offset = -0.1047950f; // Offset needed to cancel out the arbitrary offset Matrix Anti-cheat adds in

		nmsPlayer.playerConnection.a(x, y, z, yaw + matrix_offset, pitch, PacketPlayOutPosition.EnumPlayerTeleportFlags.a(tpRelative));
	}
}
