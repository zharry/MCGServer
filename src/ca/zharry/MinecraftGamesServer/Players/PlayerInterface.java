package ca.zharry.MinecraftGamesServer.Players;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.MCGScore;
import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Servers.ServerInterface;
import ca.zharry.MinecraftGamesServer.Timer.Cutscene;
import ca.zharry.MinecraftGamesServer.Utils.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;


public abstract class PlayerInterface {

    // Scores
    private int currentScore = 0;
    public ArrayList<MCGScore> previousScores = new ArrayList<>();

    public Player bukkitPlayer; // null if the player is offline
    public UUID uuid;
    public String name; // Unformatted raw username
    public ServerInterface<? extends PlayerInterface> server;
    public Scoreboard scoreboard;
    public SidebarDisplay sidebar;
    public MCGTeam myTeam;
    public Cutscene cutscene;
    public boolean disableCommit = false;

    public PlayerInterface(ServerInterface<? extends PlayerInterface> server, UUID uuid, String name) {
        this.server = server;
        this.uuid = uuid;
        this.name = name;

        fetchData();

        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        sidebar = new SidebarDisplay(scoreboard, "MCG Season " + MCGMain.SEASON);
        myTeam = server.getTeamFromPlayerUUID(uuid);

        ArrayList<PlayerInterface> combinedPlayers = new ArrayList<>();
        combinedPlayers.addAll(server.offlinePlayers);
        combinedPlayers.addAll(server.players);

        // For all other players
        for (PlayerInterface player : combinedPlayers) {
            // Get their team
            MCGTeam playerTeam = server.getTeamFromPlayerUUID(player.uuid);

            // Add their team to our own scoreboard
            addPlayerTeamToScoreboard(scoreboard, playerTeam, player);

            // Add our entry to their scoreboard
            addPlayerTeamToScoreboard(player.scoreboard, myTeam, this);
        }
        // Add our entry to our scoreboard
        addPlayerTeamToScoreboard(scoreboard, myTeam, this);
    }

    private void addPlayerTeamToScoreboard(Scoreboard scoreboard, MCGTeam team, PlayerInterface player) {
        Team minecraftTeam = scoreboard.getTeam("Team" + team.id);
        if (minecraftTeam == null) {
            minecraftTeam = scoreboard.registerNewTeam("Team" + team.id);
        }
        minecraftTeam.addEntry(player.name);
        minecraftTeam.setColor(team.chatColor);
        server.configureScoreboardTeam(minecraftTeam, team);
    }

    /* PLAYER LOGIC */

    public void playerJoin(Player player) {
        bukkitPlayer = player;
        name = player.getName();
        commitName();// TODO commit less for performance
        bukkitPlayer.setScoreboard(scoreboard);

        // Set display name and color properly
        bukkitPlayer.setDisplayName(server.getTeamFromPlayerUUID(uuid).chatColor + name + ChatColor.RESET);

        if (myTeam == server.defaultTeam) {
            server.defaultTeam.addPlayer(uuid);
        }
    }

    public void playerQuit(Player player) {
        commit();
        bukkitPlayer = null;
    }

    public boolean isOnline() {
        return bukkitPlayer != null;
    }

    public int getScore() {
        int val = currentScore;
        for (MCGScore score : previousScores) {
            if (score.season == MCGMain.SEASON)
                val += score.score;
        }
        return val;
    }

    public int getScore(String minigame) {
        int val = 0;
        if (minigame.equals(server.minigame)) {
            val += currentScore;
        }
        for (MCGScore score : previousScores) {
            if (score.minigame.equals(minigame)) {
                if (score.season == MCGMain.SEASON)
                    val += score.score;
            }
        }
        return val;
    }

    /* SCOREBOARD LOGIC */
    private static final StringAlignUtils strFmt = new StringAlignUtils(70, 5);
    private static final String EMPTY_LINE = "                                                                      \n";
    private static final String LINE = "&5&m                                                            &r";
    private static final String[] HEADERS = {
            "&bMCG Season " + MCGMain.SEASON,
            "",
            LINE,
            "&fOverall Game Scores:"
    };

    public void doStatsRefresh() {
        updateTabList();
        updateSidebar();
    }

    public void addScore(int scoreDelta, String notes) {
        this.currentScore += scoreDelta;
        MCGMain.sqlManager.executeQueryAsync(
                "INSERT INTO `logs` (`season`, `minigame`, `playeruuid`, `scoredelta`, `message`) " +
                        " VALUES (?, ?, ?, ?, ?)", MCGMain.SEASON, server.minigame, uuid.toString(), scoreDelta, notes);
    }

    public int getCurrentScore() {
        return this.currentScore;
    }

    /* TAB MENU */

    public void updateTabList() {
        StringBuilder sb = new StringBuilder(EMPTY_LINE);
        for (String header : HEADERS) {
            sb.append(strFmt.center(ChatColor.translateAlternateColorCodes('&', header))).append("\n");
        }

        ArrayList<MCGTeam> teams = server.getTeamsOrderedByScore();
        int columnCnt = 2;

        TableGenerator.Alignment[] alignments = new TableGenerator.Alignment[columnCnt];
        alignments[columnCnt - 1] = TableGenerator.Alignment.RIGHT;
        for (int i = 0; i < columnCnt - 1; i++) {
            alignments[i] = TableGenerator.Alignment.LEFT;
        }

        TableGenerator table = new TableGenerator(alignments);
        table.addRow("                                                  ");

        for (int place = 1; place <= teams.size(); place++) {
            MCGTeam team = teams.get(place - 1);
            String[] teamInfo = new String[columnCnt];
            teamInfo[0] = "§r" + place + ". §" + team.chatColor.getChar() + team.teamname;
            teamInfo[columnCnt - 1] = team.getScore() + "";
            table.addRow(teamInfo);
            ArrayList<UUID> uuids = team.players;
            StringBuilder nameStrs = new StringBuilder();

            if (uuids.size() > 0) {
                int maxLen = (50 - uuids.size() + 1) / uuids.size();
                for (UUID uuid : uuids) {
                    PlayerInterface p = server.getPlayerFromUUID(uuid);
                    if (p != null) {
                        String name = p.getPlayerNameForTabMenu();
                        nameStrs.append(ChatStringUtils.truncateChatString(name, maxLen)).append(" ");
                    }
                }
            }
            table.addRow(nameStrs.toString());
            table.addRow();
        }

        for (String line : table.generate(TableGenerator.Receiver.CLIENT, true, true)) {
            sb.append(line).append("\n");
        }

        sb.append(strFmt.center(ChatColor.translateAlternateColorCodes('&', LINE))).append("\n");

        StringBuilder noTeamSB = new StringBuilder();

        for (UUID uuid : server.defaultTeam.players) {
            PlayerInterface player = server.getPlayerFromUUID(uuid);
            if(player.isOnline()) {
                noTeamSB.append(player.getPlayerNameForTabMenu()).append("\n");
            }
        }

        if(noTeamSB.length() != 0) {
            sb.append("\n").append(server.defaultTeam.teamname).append(":\n\n");
            sb.append(noTeamSB);
        }

        StringBuilder spectatorSB = new StringBuilder();

        for (PlayerInterface player : server.spectators) {
            spectatorSB.append(player.getPlayerNameForTabMenu()).append("\n");
        }

        if(spectatorSB.length() != 0) {
            sb.append("\n").append("Spectators").append(":\n\n");
            sb.append(spectatorSB);
        }

        for (int i = 0; i < 100; i++) sb.append(EMPTY_LINE);
        bukkitPlayer.setPlayerListHeader(sb.toString());
    }

    public String getPlayerNameForTabMenu() {
        if (isOnline()) {
            return bukkitPlayer.getDisplayName();
        }
        return ChatColor.GRAY + name + ChatColor.RESET;
    }

    public String getPlayerNameForTabMenu(boolean strikethrough) {
        if (!strikethrough)
            return getPlayerNameForTabMenu();

        if (isOnline()) {
            return myTeam.chatColor + "" + ChatColor.STRIKETHROUGH + name + ChatColor.RESET;
        }
        return ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + name + ChatColor.RESET;
    }

    public String getDisplayName() {
        return myTeam.chatColor + name + ChatColor.RESET;
    }

    /* SIDEBAR */

    public abstract void updateSidebar();

    public void setTeamScoresForSidebar(String curMinigameStr, int curTeamID) {
        ArrayList<MCGTeam> sortedTeams = server.getTeamsOrderedByScore(curMinigameStr);
        setRankedDisplayForSidebar(sortedTeams, server.getTeamFromTeamID(curTeamID), (team, position, bold) -> {
            return " " + team.chatColor + position + ". " + bold + team.teamname + " " + ChatColor.RESET + team.getScore(curMinigameStr);
        });
    }

    public <T> void setRankedDisplayForSidebar(List<T> sortedList, T c, TriFunction<T, Integer, String, String> value) {
        int curPlace = -1;
        for (int i = 0; i < sortedList.size(); i++) {
            if (sortedList.get(i).equals(c)) {
                curPlace = i;
                break;
            }
        }

        for (int i = sortedList.size(); i < 4; i++) {
            sortedList.add(null);
        }

        int[] placements;

        // 1st, 2nd, or 3rd
        if (curPlace == 0 || curPlace == 1 || curPlace == 2 || curPlace == -1) {
            placements = new int[]{0, 1, 2, 3};
        // Dead last
        } else if (curPlace == sortedList.size() - 1) {
            placements = new int[]{0, 1, curPlace - 1, curPlace};
        // Every position in between
        } else {
            placements = new int[]{0, curPlace - 1, curPlace, curPlace + 1};
        }

        sidebar.add(ChatColor.BLUE + "" + ChatColor.BOLD + "Game scores: ");

        for (int i = 0; i < 4; i++) {
            String bold = "";
            T entry = sortedList.get(placements[i]);
            if (entry == null) break;
            if (entry.equals(c)) {
                bold = ChatColor.BOLD.toString();
            }
            sidebar.add(value.apply(entry, placements[i] + 1, bold));
        }
    }

    /* SQL LOGIC */

    public final void fetchData() {
        this.previousScores.clear();

        String newMetadata = "{}";

        try {
            ResultSet resultSet = MCGMain.sqlManager.executeQuery("SELECT * FROM `scores` WHERE `uuid` = ?", uuid.toString());
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String uuid = resultSet.getString("uuid").trim();
                int season = resultSet.getInt("season");
                String minigame = resultSet.getString("minigame").trim();
                int score = resultSet.getInt("score");
                String metadata = resultSet.getString("metadata");

                if (season == MCGMain.SEASON && minigame.equals(server.minigame)) {
                    currentScore = score;
                    if(metadata != null) {
                        newMetadata = metadata;
                    }
                } else {
                    MCGScore newScore = new MCGScore(id, uuid, season, minigame, score, metadata);
                    this.previousScores.add(newScore);
                }
            }
            ClassSaveHandler.fromJSON(this, newMetadata);
        } catch (Exception e) {
            MCGMain.broadcastError("Could not load metadata for " + this + ": " + e);
            e.printStackTrace();
        }
    }

    public final void commit() {
        if(disableCommit) {
            return;
        }
        try {
            MCGMain.sqlManager.executeQuery(
                    "INSERT INTO `scores` (`uuid`, `season`, `minigame`, `score`, `metadata`) " +
                            "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                            "`score` = VALUES(`score`), " +
                            "`metadata` = VALUES(`metadata`), " +
                            "`time` = current_timestamp()",
                    uuid.toString(), MCGMain.SEASON, server.minigame, getCurrentScore(), ClassSaveHandler.toJSON(this));
        } catch (SQLException e) {
            MCGMain.broadcastError("Could not save metadata for " + this + ": " + e);
            e.printStackTrace();
        }
    }

    public final void commitName() {
        try {
            MCGMain.sqlManager.executeQuery(
                    "INSERT INTO `players`(`uuid`, `season`, `username`, `teamid`) " +
                            "VALUES (?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE " +
                            "`username` = VALUES(`username`)",
                    uuid.toString(), MCGMain.SEASON, name, myTeam.id == 0 ? null : myTeam.id);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setGameMode(GameMode mode) {
        MCGMain.gameModeManager.setGameMode(bukkitPlayer, mode);
    }

    public GameMode getGameMode() {
        return MCGMain.gameModeManager.getGameMode(bukkitPlayer);
    }

    public void reset(GameMode mode) {
        setGameMode(mode);
        bukkitPlayer.getInventory().clear();
        bukkitPlayer.setWalkSpeed(0.2f);
        bukkitPlayer.setAbsorptionAmount(0);
        bukkitPlayer.setHealth(20);
        bukkitPlayer.setHealthScaled(false);
        bukkitPlayer.setFoodLevel(20);
        bukkitPlayer.setSaturation(5);
        bukkitPlayer.setExhaustion(0);
        bukkitPlayer.setExp(0);
        bukkitPlayer.setTotalExperience(0);
        bukkitPlayer.getActivePotionEffects().clear();
        bukkitPlayer.setFallDistance(0);
    }

    public void teleport(Location location) {
        bukkitPlayer.teleport(location);
    }

    public void teleportPositionOnly(Location location) {
        NMSHelper.teleport(bukkitPlayer, location.getX(), location.getY(), location.getZ(), 0, 0, false, false, false, true, true);
    }

    public void teleportRelative(double x, double y, double z, float yaw, float pitch, boolean relX, boolean relY, boolean relZ, boolean relYaw, boolean relPitch) {
        NMSHelper.teleport(bukkitPlayer, x, y, z, yaw, pitch, relX, relY, relZ, relYaw, relPitch);
    }

    public Location getLocation() {
        return bukkitPlayer.getLocation();
    }

    public void setHealth(double health) {
        bukkitPlayer.setHealth(health);
    }

    public boolean equals(Object o) {
        if(!(o instanceof PlayerInterface)) {
            return false;
        }
        return uuid.equals(((PlayerInterface) o).uuid);
    }

    public String toString() {
        return "PlayerInterface[" + name + ", " + uuid + "]";
    }

}
