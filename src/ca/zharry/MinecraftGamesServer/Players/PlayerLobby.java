package ca.zharry.MinecraftGamesServer.Players;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Servers.ServerLobby;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class PlayerLobby extends PlayerInterface {

    public ServerLobby server;

    public PlayerLobby(Player bukkitPlayer, ServerLobby server) {
        super(bukkitPlayer, server, "lobby");
        this.server = server;
    }

    @Override
    public void updateScoreboard() {
        // This is a spacer
        sidebar.add("                          ");

        if (server.state == ServerLobby.LOBBY_WAITING) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Welcome! ");
            sidebar.add(ChatColor.WHITE + "Waiting for players...");
        } else if (server.state == ServerLobby.LOBBY_STARTED) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Next event: ");
            sidebar.add(ChatColor.WHITE + "" + server.minigames.get(server.nextMinigame));
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Begins in: ");
            sidebar.add(ChatColor.WHITE + "" + server.timerNextGame.getString() + (server.timerNextGame.isPaused() ? " (Paused)" : ""));
        }
        sidebar.add("");
        sidebar.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Players: " + ChatColor.RESET + "" + server.players.size() + "/" + MCGMain.PLAYER_TARGET);

        sidebar.add("");
        sidebar.add(ChatColor.WHITE + "" + ChatColor.BOLD + "Your team: ");
        sidebar.add(myTeam.chatColor + myTeam.teamname);
        sidebar.add("");
        sidebar.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Score: " + ChatColor.RESET + "" + getScore());
        sidebar.end();

//        int[] jj = new int[]{14, 10, 12, 11, 13, 9, 6, 5};
//
//        for(int j = 0; j < 8; ++j) {
//            int i = jj[j];//(j >> 1) | ((j & 1) << 3);
////            if(i == 0 || i == 1 || i == 7 || i == 8 || i == 4 || i == 5 || i == 2 || i == 15)
////                continue;
//            sidebar.add("§" + Integer.toHexString(i) + " This is some test string " + i);
//        }
//        sidebar.end();
    }

    @Override
    public void commit() {

    }

}
