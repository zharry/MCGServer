package ca.zharry.MinecraftGamesServer.Players;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Servers.ServerLobby;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

public class PlayerLobby extends PlayerInterface {

    public ServerLobby server;

    public PlayerLobby(Player bukkitPlayer, ServerLobby server) {
        super(bukkitPlayer, server, "lobby");
    }

    public int counter = 0;

    @Override
    public void updateScoreboard() {
        try {
            scoreboard.getObjective("scoreboard").unregister();
        } catch (Exception ignored) {
        }
        Objective objective = scoreboard.registerNewObjective("scoreboard", "dummy", "MCG Season " + MCGMain.SEASON);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // This is a spacer
        objective.getScore("                          ").setScore(15);

        if (server.state == ServerLobby.LOBBY_WAITING) {
            objective.getScore(ChatColor.RED + "" + ChatColor.BOLD + "Welcome! ").setScore(10);
            objective.getScore(ChatColor.WHITE + "Waiting for players...").setScore(9);
        } else if (server.state == ServerLobby.LOBBY_STARTED) {
            objective.getScore(ChatColor.RED + "" + ChatColor.BOLD + "Next event: ").setScore(12);
            objective.getScore(ChatColor.WHITE + "" + server.minigames.get(server.nextMinigame)).setScore(11);
            objective.getScore(ChatColor.RED + "" + ChatColor.BOLD + "Begins in: ").setScore(10);
            objective.getScore(ChatColor.WHITE + "" + server.timerNextGame.getString() + (server.timerNextGame.isPaused() ? " (Paused)" : "")).setScore(9);
        }
        objective.getScore("").setScore(8);
        objective.getScore(ChatColor.GREEN + "" + ChatColor.BOLD + "Players: " + ChatColor.RESET + "" + server.players.size() + "/" + MCGMain.PLAYER_TARGET).setScore(7);
        objective.getScore(" ").setScore(6);
        objective.getScore(ChatColor.WHITE + "" + ChatColor.BOLD + "Your team: ").setScore(5);
        objective.getScore(myTeam.teamname).setScore(4);
        objective.getScore("  ").setScore(3);
        objective.getScore(ChatColor.GREEN + "" + ChatColor.BOLD + "Score: " + ChatColor.RESET + "" + getScore()).setScore(2);
        counter++;
    }

    @Override
    public void commit() {

    }

}
