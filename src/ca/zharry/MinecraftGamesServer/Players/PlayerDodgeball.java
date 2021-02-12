package ca.zharry.MinecraftGamesServer.Players;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Servers.ServerDodgeball;
import ca.zharry.MinecraftGamesServer.Servers.ServerParkour;
import ca.zharry.MinecraftGamesServer.Utils.Saved;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;

import java.util.UUID;

public class PlayerDodgeball extends PlayerInterface {

    // Minigame variables
    @Saved
    public int totalKills = 0;
    @Saved
    public boolean invulnerable = true;
    public int kills = 0;
    public int lives = 0;
    public int arena = -1;
    public boolean inSpawn = true;
    public int spawnTimer = ServerDodgeball.SPAWN_TIMER;
    public MCGTeam opponentTeam;
    public Location lastDeathLocation;

    public ServerDodgeball server;

    public PlayerDodgeball(ServerDodgeball server, UUID uuid, String username) {
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

            if (getGameMode() == GameMode.ADVENTURE)
                sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Time left: " + ChatColor.RESET + server.timerInProgress);
            else
                sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Waiting for others: " + ChatColor.RESET + server.timerInProgress);

            sidebar.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Round: " + ChatColor.RESET + server.currentGame + "/" + server.totalGames);
        } else if (server.state == ServerParkour.GAME_FINISHED) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Back to lobby: " + ChatColor.RESET + server.timerFinished);
        }
        sidebar.add("");
        setTeamScoresForSidebar(server.minigame, myTeam.id);
        sidebar.add("");
        if (opponentTeam != null) {
            if (server.state == ServerParkour.GAME_STARTING) {
                sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Next Opponent: ");
                sidebar.add(opponentTeam.chatColor + "" + ChatColor.BOLD + opponentTeam.teamname + " ");
                sidebar.add("   ");
            } else if (server.state == ServerParkour.GAME_INPROGRESS) {
                sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Opponent: " + ChatColor.RESET + "" + opponentTeam.chatColor + "" + opponentTeam.teamname + " ");
                sidebar.add(ChatColor.WHITE + "" + ChatColor.BOLD + "Kills: " + ChatColor.RESET + "" + kills);
                sidebar.add(ChatColor.WHITE + "" + ChatColor.BOLD + "Lives: " + ChatColor.RESET + "" + lives);
            } else if (server.state == ServerParkour.GAME_FINISHED) {
                sidebar.add(ChatColor.WHITE + "" + ChatColor.BOLD + "Total Kills: " + ChatColor.RESET + "" + totalKills);
            }
        } else {
            sidebar.add(ChatColor.WHITE + "Please wait...");
            sidebar.add("");
        }
        sidebar.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Your Score: " + ChatColor.RESET + "" + getCurrentScore());
        sidebar.end();
    }

    @Override
    public String getPlayerNameForTabMenu() {
        if(server.state == ServerDodgeball.GAME_INPROGRESS) {
            if (this.lives <= 0) {
                return super.getPlayerNameForTabMenu(true);
            }
        }
        return super.getPlayerNameForTabMenu();
    }

}
