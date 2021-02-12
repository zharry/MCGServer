package ca.zharry.MinecraftGamesServer.Players;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.Servers.ServerSurvivalGames;
import ca.zharry.MinecraftGamesServer.Utils.Saved;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.UUID;

public class PlayerSurvivalGames extends PlayerInterface {

    // Minigame variables
    public boolean dead = false;
    @Saved
    public int kills = 0;
    public Location deathLocation;

    public ServerSurvivalGames server;

    public PlayerSurvivalGames(ServerSurvivalGames server, UUID uuid, String username) {
        super(server, uuid, username);
        this.server = server;
    }

    @Override
    public void updateSidebar() {
        // This is a spacer
        sidebar.add("                          ");

        sidebar.add(ChatColor.BLUE + "" + ChatColor.BOLD + "Game: " + ChatColor.RESET + MCGMain.serverNames.get(server.minigame));

        if (server.state == ServerSurvivalGames.GAME_WAITING) {
            sidebar.add(ChatColor.WHITE + "Waiting for game start...");
        } else if (server.state == ServerSurvivalGames.GAME_STARTING) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Teleporting: " + ChatColor.RESET + server.timerStartGame);
        } else if (server.state == ServerSurvivalGames.GAME_BEGIN) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Game Begins: " + ChatColor.RESET + server.timerBegin);
        } else if (server.state == ServerSurvivalGames.GAME_INPROGRESS) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Time left: " + ChatColor.RESET + server.timerInProgress);
            sidebar.add(ChatColor.BLUE + "" + ChatColor.BOLD + "Next event: " + ChatColor.RESET + "" + server.getNextEvent());
            sidebar.add(ChatColor.WHITE + "" + ChatColor.BOLD + "World Border: " + ChatColor.RESET + "" + server.getWorldBorder());
        } else if (server.state == ServerSurvivalGames.GAME_FINISHED) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Back to lobby: " + ChatColor.RESET + server.timerFinished);
        }
        sidebar.add("");
        setTeamScoresForSidebar(server.minigame, myTeam.id);
        sidebar.add("");
        sidebar.add(ChatColor.BLUE + "" + ChatColor.BOLD + "Still alive: " + ChatColor.RESET + "" + server.getPlayersAlive());
        sidebar.add(ChatColor.WHITE + "" + ChatColor.BOLD + "Kills: " + ChatColor.RESET + "" + kills);
        sidebar.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Your Score: " + ChatColor.RESET + "" + getCurrentScore());
        sidebar.end();
    }

    @Override
    public String getPlayerNameForTabMenu() {
        if(this.dead) {
            return super.getPlayerNameForTabMenu(true);
        }
        return super.getPlayerNameForTabMenu();
    }
}
