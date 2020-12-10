package ca.zharry.MinecraftGamesServer.Players;

import org.bukkit.entity.Player;

public abstract class PlayerInterface {

    public Player bukkitPlayer;
    public PlayerInterface(Player bukkitPlayer) {
        this.bukkitPlayer = bukkitPlayer;
    }

    public abstract void updateScoreboard();
    public abstract void commit();

}
