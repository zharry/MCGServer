package ca.zharry.MinecraftGamesServer.Players;

import ca.zharry.MinecraftGamesServer.Listeners.ListenerElytraRun;
import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Servers.ServerElytraRun;
import ca.zharry.MinecraftGamesServer.Servers.ServerParkour;
import org.bukkit.ChatColor;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PlayerElytraRun extends PlayerInterface {

    public double[] maxDistance = new double[ServerElytraRun.tunnels.length];
    public long[] completedTime = new long[ServerElytraRun.tunnels.length];

    public boolean dead;

    public boolean inBlock;
    public int inBlockTimer = 0;

    public long startingTime;

    public boolean hintsEnabled = true;

    // Minigame variables

    public ServerElytraRun server;

    public PlayerElytraRun(ServerElytraRun server, UUID uuid, String username) {
        super(server, uuid, username);
        this.server = server;
    }

    @Override
    public void updateSidebar() {
        // This is a spacer
        sidebar.add("                          ");

        sidebar.add(ChatColor.BLUE + "" + ChatColor.BOLD + "Game: " + ChatColor.RESET + MCGMain.serverNames.get(server.minigame));

        if (server.state == ServerParkour.GAME_WAITING) {
            sidebar.add(ChatColor.WHITE + "Waiting for game start...");
        } else if (server.state == ServerParkour.GAME_STARTING) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Game begins: " + ChatColor.RESET + server.timerStartGame);
        } else if (server.state == ServerParkour.GAME_INPROGRESS) {
            sidebar.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Tunnel: " + ChatColor.RESET + (server.tunnel + 1) + "/" + ServerElytraRun.tunnels.length);
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Time left: " + ChatColor.RESET + server.timerInProgress);
        } else if (server.state == ServerParkour.GAME_FINISHED) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Back to lobby: " + ChatColor.RESET + server.timerFinished);
        }

        sidebar.add("");
        if (server.state == ServerParkour.GAME_STARTING || server.state == ServerParkour.GAME_INPROGRESS) {
            List<PlayerElytraRun> sortedPlayers = server.getSortedPlayers();
            Collections.reverse(sortedPlayers);
            setRankedDisplayForSidebar(sortedPlayers, this, (player, position, bold) -> {
                if (player.maxDistance[server.tunnel] == Double.POSITIVE_INFINITY)
                    return " " + player.myTeam.chatColor + position + ". " + bold + player.name + " " + ChatColor.RESET + "Finished " + ListenerElytraRun.timeToString(player.completedTime[server.tunnel]) + "s";
                else
                    return " " + player.myTeam.chatColor + position + ". " + bold + player.name + " " + Math.round(100.0 * player.maxDistance[server.tunnel] / server.tunnelLength(server.tunnel)) + "%";
            });
        } else {
            setTeamScoresForSidebar(server.minigame, myTeam.id);
        }
        sidebar.add("");
        sidebar.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Team Score: " + ChatColor.RESET + "" + myTeam.getScore(server.minigame));
        sidebar.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Your Score: " + ChatColor.RESET + "" + getCurrentScore());
        sidebar.end();
    }
}
