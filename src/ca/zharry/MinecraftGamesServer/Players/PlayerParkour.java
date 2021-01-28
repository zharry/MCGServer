package ca.zharry.MinecraftGamesServer.Players;

import ca.zharry.MinecraftGamesServer.Servers.ServerParkour;
import org.bukkit.ChatColor;

import java.util.UUID;

public class PlayerParkour extends PlayerInterface {

    // Minigame variables
    public int stage = 0; // What stage they are currently on
    public int level = 0; // What level of the stage they have completed

    public boolean waypointsEnabled = true;

    public ServerParkour server;

    public PlayerParkour(ServerParkour server, UUID uuid, String username) {
        super(server, uuid, username);
        this.server = server;
    }

    @Override
    public void updateSidebar() {
        // This is a spacer
        sidebar.add("                          ");

        sidebar.add(ChatColor.BLUE + "" + ChatColor.BOLD + "Game: " + ChatColor.RESET + "Parkour");

        if (server.state == ServerParkour.GAME_WAITING) {
            sidebar.add(ChatColor.WHITE + "Waiting for game start...");
        } else if (server.state == ServerParkour.GAME_STARTING) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Game begins: " + ChatColor.RESET + server.timerStartGame);
        } else if (server.state == ServerParkour.GAME_INPROGRESS) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Time left: " + ChatColor.RESET + server.timerInProgress);
        } else if (server.state == ServerParkour.GAME_FINISHED) {
            sidebar.add(ChatColor.RED + "" + ChatColor.BOLD + "Back to lobby: " + ChatColor.RESET + server.timerFinished);
        }

        if (server.state == ServerParkour.GAME_INPROGRESS || server.state == ServerParkour.GAME_FINISHED) {
            sidebar.add("");
            sidebar.add(ChatColor.AQUA + "" + ChatColor.BOLD + "Completed Stage: " + ChatColor.RESET + stage + "-" + level);
        }
        sidebar.add("");

        setTeamScoresForSidebar("parkour", myTeam.id);
        sidebar.add("");
        sidebar.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Team Score: " + ChatColor.RESET + "" + myTeam.getScore("parkour"));
        sidebar.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Your Score: " + ChatColor.RESET + "" + getCurrentScore());
        sidebar.end();
    }

    @Override
    public void loadMetadata(String metadata) {
        if(metadata != null) {
            try {
                String[] data = metadata.split("-");
                stage = Integer.parseInt(data[0]);
                level = Integer.parseInt(data[1]);
            } catch (Exception ignore) {
            }
        }
    }

    @Override
    public String saveMetadata() {
        return stage + "-" + level;
    }
}
