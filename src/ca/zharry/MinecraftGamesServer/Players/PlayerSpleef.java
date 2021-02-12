package ca.zharry.MinecraftGamesServer.Players;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Servers.ServerSpleef;
import org.bukkit.ChatColor;

import java.util.UUID;

public class PlayerSpleef extends PlayerInterface {

    // Minigame variables
    public boolean dead = false;

    public ServerSpleef server;

    public PlayerSpleef(ServerSpleef server, UUID uuid, String username) {
        super(server, uuid, username);
        this.server = server;
    }

    @Override
    public void updateSidebar() {
        // This is a spacer
        sidebar.add("                          ");

        sidebar.add(ChatColor.BLUE + "" + ChatColor.BOLD + "Game: " + ChatColor.RESET + MCGMain.serverNames.get(server.minigame));
        if (server.state == ServerSpleef.GAME_STARTING || server.state == ServerSpleef.GAME_INPROGRESS) {
            sidebar.add(ChatColor.BLUE + "" + ChatColor.BOLD + "Round: " + ChatColor.RESET + "" + server.currentGame + "/" + ServerSpleef.TOTAL_GAMES);
        }
        if (server.state == ServerSpleef.GAME_WAITING) {
            sidebar.add(ChatColor.WHITE + "Waiting for game start...");
        } else if (server.state == ServerSpleef.GAME_STARTING) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Round starts: " + ChatColor.RESET + server.timerStartGame);
        } else if (server.state == ServerSpleef.GAME_BEGIN && server.firstRun) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Game begins: " + ChatColor.RESET + server.timerStartGame);
        } else if (server.state == ServerSpleef.GAME_BEGIN) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Game begins: " + ChatColor.RESET + server.timerBegin);
        } else if (server.state == ServerSpleef.GAME_INPROGRESS) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Time left: " + ChatColor.RESET + server.timerInProgress);
        } else if (server.state == ServerSpleef.GAME_FINISHED) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Back to lobby: " + ChatColor.RESET + server.timerFinished);
        }
        sidebar.add("");
        setTeamScoresForSidebar(server.minigame, myTeam.id);
        sidebar.add("  ");
        sidebar.add(ChatColor.BLUE + "" + ChatColor.BOLD + "Still alive: " + ChatColor.RESET + "" + server.getPlayersAlive());
        sidebar.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Team Score: " + ChatColor.RESET + "" + myTeam.getScore(server.minigame));
        sidebar.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Your Score: " + ChatColor.RESET + "" + getCurrentScore());
        sidebar.end();
    }

    @Override
    public String getPlayerNameForTabMenu() {
        if (this.dead) {
            return super.getPlayerNameForTabMenu(true);
        }
        return super.getPlayerNameForTabMenu();
    }

}
