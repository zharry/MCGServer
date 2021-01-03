package ca.zharry.MinecraftGamesServer.Players;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.MCGScore;
import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Servers.ServerInterface;
import ca.zharry.MinecraftGamesServer.Timer.Cutscene;
import ca.zharry.MinecraftGamesServer.Utils.ChatStringUtils;
import ca.zharry.MinecraftGamesServer.Utils.SidebarDisplay;
import ca.zharry.MinecraftGamesServer.Utils.StringAlignUtils;
import ca.zharry.MinecraftGamesServer.Utils.TableGenerator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.UUID;

public abstract class PlayerInterface {

    // Scores
    public String curMinigame = "lobby";
    public int currentScore = 0;
    public String currentMetadata = "";
    public ArrayList<MCGScore> previousScores = new ArrayList<>();

    public Player bukkitPlayer; // null if the player is offline
    public UUID uuid;
    public String name; // Unformatted raw username
    public ServerInterface server;
    public Scoreboard scoreboard;
    public SidebarDisplay sidebar;
    public MCGTeam myTeam;
    public Cutscene cutscene;

    public PlayerInterface(ServerInterface server, UUID uuid, String name, String curMinigame) {
        this.server = server;
        this.uuid = uuid;
        this.name = name;
        this.curMinigame = curMinigame;

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
        Team minecraftTeam = scoreboard.getTeam(team.teamname);
        if (minecraftTeam == null) {
            minecraftTeam = scoreboard.registerNewTeam(team.teamname);
        }
        minecraftTeam.addEntry(player.name);
        minecraftTeam.setColor(team.chatColor);
        minecraftTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        minecraftTeam.setCanSeeFriendlyInvisibles(false);
        if(team != server.defaultTeam) {
            minecraftTeam.setAllowFriendlyFire(false);
        }
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
        if (minigame.equals(curMinigame)) {
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

    /* SIDEBAR */

    public abstract void updateSidebar();

    public void setTeamScoresForSidebar(String curMinigameStr, int curTeamID) {
        ArrayList<MCGTeam> sortedTeams = server.getTeamsOrderedByScore(curMinigameStr);
        int curTeamPlace = -1;
        for (int i = 0; i < sortedTeams.size(); i++) {
            int curId = sortedTeams.get(i).id;
            if (curId == curTeamID) {
                curTeamPlace = i;
                break;
            }
        }

        int[] resIds;
        int[] placements;
        if (curTeamPlace == 0 || curTeamPlace == 1 || curTeamPlace == -1) {
            resIds = new int[]{
                    sortedTeams.get(0).id,
                    sortedTeams.get(1).id,
                    sortedTeams.get(2).id,
                    sortedTeams.get(3).id
            };
            placements = new int[]{1, 2, 3, 4};
        } else if (curTeamPlace == sortedTeams.size() - 1) {
            resIds = new int[]{
                    sortedTeams.get(0).id,
                    sortedTeams.get(1).id,
                    sortedTeams.get(curTeamPlace - 1).id,
                    curTeamID
            };
            placements = new int[]{1, 2, curTeamPlace, curTeamPlace + 1};
        } else {
            resIds = new int[]{
                    sortedTeams.get(0).id,
                    sortedTeams.get(curTeamPlace - 1).id,
                    curTeamID,
                    sortedTeams.get(curTeamPlace + 1).id
            };
            placements = new int[]{1, curTeamPlace, curTeamPlace + 1, curTeamPlace + 2};
        }
//        sidebar.add(ChatColor.BLUE + "" + ChatColor.BOLD + "Game scores: ");

        for (int i = 0; i < 5; i++) {
            if (i == 0) {
                sidebar.add(ChatColor.BLUE + "" + ChatColor.BOLD + "Game scores: ");
                continue;
            }
            String bold = "";
            if (resIds[i - 1] == curTeamID) {
                bold = ChatColor.BOLD.toString();
            }
            MCGTeam t = server.getTeamFromTeamID(resIds[i - 1]);
            sidebar.add(t.chatColor + " " + placements[i - 1] + ". " + bold + t.teamname + ChatColor.WHITE + " " + t.getScore(curMinigameStr));
        }
    }

    /* SQL LOGIC */

    public void fetchData() {
        this.previousScores.clear();

        try {
            Statement statement = MCGMain.conn.connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM `scores` WHERE `uuid` = '" + uuid + "';");
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String uuid = resultSet.getString("uuid").trim();
                int season = resultSet.getInt("season");
                String minigame = resultSet.getString("minigame").trim();
                int score = resultSet.getInt("score");
                String metadata = resultSet.getString("metadata");

                if (season == MCGMain.SEASON && minigame.equals(curMinigame)) {
                    currentScore = score;
                    currentMetadata = metadata;
                } else {
                    MCGScore newScore = new MCGScore(id, uuid, season, minigame, score, metadata);
                    this.previousScores.add(newScore);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public abstract void commit();

    public void commitName() {
        try {
            PreparedStatement stmt = MCGMain.conn.connection.prepareStatement("INSERT INTO `usernames`(`uuid`, `season`, `username`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `username` = ?;");
            stmt.setString(1, uuid.toString());
            stmt.setInt(2, MCGMain.SEASON);
            stmt.setString(3, name);
            stmt.setString(4, name);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
