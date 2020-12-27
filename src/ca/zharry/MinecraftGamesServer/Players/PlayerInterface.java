package ca.zharry.MinecraftGamesServer.Players;

import ca.zharry.MinecraftGamesServer.MCGMain;
import ca.zharry.MinecraftGamesServer.MCGScore;
import ca.zharry.MinecraftGamesServer.MCGTeam;
import ca.zharry.MinecraftGamesServer.Servers.ServerInterface;
import ca.zharry.MinecraftGamesServer.Utils.SidebarDisplay;
import ca.zharry.MinecraftGamesServer.Utils.StringAlignUtils;
import ca.zharry.MinecraftGamesServer.Utils.TableGenerator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.UUID;

public abstract class PlayerInterface {

    // Scores
    public String curMinigame = "lobby";
    public int currentScore = 0;
    public String currentMetadata = "";
    public ArrayList<MCGScore> previousScores;

    public Player bukkitPlayer;
    public ServerInterface server;
    public Scoreboard scoreboard;
    public SidebarDisplay sidebar;
    public MCGTeam myTeam;

    public PlayerInterface(Player bukkitPlayer, ServerInterface server, String curMinigame) {
        this.bukkitPlayer = bukkitPlayer;
        this.server = server;
        this.curMinigame = curMinigame;

        this.previousScores = new ArrayList<MCGScore>();
        getData();

        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        sidebar = new SidebarDisplay(scoreboard, "MCG Season " + MCGMain.SEASON);
        this.bukkitPlayer.setScoreboard(scoreboard);
        myTeam = server.teams.get(server.teamLookup.get(bukkitPlayer.getUniqueId()));

        // For all other players
        for (PlayerInterface player : server.players) {
            // Get their team
            MCGTeam playerTeam = server.teams.get(server.teamLookup.get(player.bukkitPlayer.getUniqueId()));

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
        minecraftTeam.addEntry(player.bukkitPlayer.getName());
        minecraftTeam.setColor(team.chatColor);
        minecraftTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        minecraftTeam.setCanSeeFriendlyInvisibles(true);
        minecraftTeam.setAllowFriendlyFire(false);
    }

    public void doStatsRefresh() {
        updateTabList();
        updateScoreboard();
    }

    private static final String EMPTY_LINE = "                                                                      \n";
    private static final String LINE = "&5&m                                                            &r";
    private static final String[] HEADERS = {
            "&bMCG Season " + MCGMain.SEASON,
            "",
            LINE,
            "&fOverall Game Scores:"
    };

    private static final StringAlignUtils strFmt = new StringAlignUtils(70,5);


    public String getPlayerNameFormatted(Player player) {
        return player.getDisplayName();
    }

    public void updateTabList() {

        StringBuilder sb = new StringBuilder(EMPTY_LINE);
        for (String header : HEADERS) {
            sb.append(strFmt.center(ChatColor.translateAlternateColorCodes('&', header))).append("\n");
        }

        ArrayList<MCGTeam> teams = server.getOrderedTeams();
        int columnCnt = 2;

        TableGenerator.Alignment[] alignments = new TableGenerator.Alignment[columnCnt];
        alignments[columnCnt-1] = TableGenerator.Alignment.RIGHT;
        for (int i = 0; i < columnCnt-1; i++) {
            alignments[i] = TableGenerator.Alignment.LEFT;
        }

        TableGenerator table = new TableGenerator(alignments);
        table.addRow("                                                  ");

        for (int place = 1; place <= teams.size(); place++) {
            MCGTeam team = teams.get(place-1);
            String[] teamInfo = new String[columnCnt];
            teamInfo[0] = "§r" + place + ". §" + team.chatColor.getChar() + team.teamname;
            teamInfo[columnCnt-1] = team.getScore() + "";
            table.addRow(teamInfo);
            ArrayList<UUID> uuids = team.players;
            StringBuilder nameStrs = new StringBuilder();

            int maxLen = (50-uuids.size()+1) / uuids.size();
            for (UUID uuid : uuids) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    String name = getPlayerNameFormatted(p);
                    String nameStripped = ChatColor.stripColor(name);
                    int nOverflow = Math.max(0, nameStripped.length() - maxLen);
                    nameStrs.append(name, 0, name.length() - nOverflow).append(" ");
                }
            }
            table.addRow("§" + team.chatColor.getChar() + nameStrs.toString());
            table.addRow();
        }

        for (String line : table.generate(TableGenerator.Receiver.CLIENT, true, true)) {
            sb.append(line).append("\n");
        }
        sb.append(strFmt.center(ChatColor.translateAlternateColorCodes('&', LINE))).append("\n");

        for (int i = 0; i < 100; i++) sb.append(EMPTY_LINE);
        bukkitPlayer.setPlayerListHeader(sb.toString());
    }

    public void setGameScores(String curMinigameStr, int curTeamID) {
        ArrayList<MCGTeam> sortedTeams = new ArrayList<>(server.teams.values());
        sortedTeams.sort((a, b) -> b.getScore(curMinigameStr) - a.getScore(curMinigameStr)); // sorted in descending order
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
        if (curTeamPlace == 0 || curTeamPlace == 1) {
            resIds = new int[] {
                    sortedTeams.get(0).id,
                    sortedTeams.get(1).id,
                    sortedTeams.get(2).id,
                    sortedTeams.get(3).id
            };
            placements = new int[] {1,2,3,4};
        } else if (curTeamPlace == sortedTeams.size() - 1) {
            resIds =  new int[] {
                    sortedTeams.get(0).id,
                    sortedTeams.get(1).id,
                    sortedTeams.get(curTeamPlace-1).id,
                    curTeamID
            };
            placements = new int[] {1,2,curTeamPlace,curTeamPlace+1};
        } else {
            resIds =  new int[] {
                    sortedTeams.get(0).id,
                    sortedTeams.get(curTeamPlace-1).id,
                    curTeamID,
                    sortedTeams.get(curTeamPlace+1).id
            };
            placements = new int[] {1,curTeamPlace,curTeamPlace+1,curTeamPlace+2};
        }
//        sidebar.add(ChatColor.BLUE + "" + ChatColor.BOLD + "Game scores: ");

        for (int i = 0; i < 5; i++) {
            if (i == 0) {
                sidebar.add(ChatColor.BLUE + "" + ChatColor.BOLD + "Game scores: ");
                continue;
            }
            String bold = "";
            if (resIds[i-1] == curTeamID) {
                bold = ChatColor.BOLD.toString();
            }
            MCGTeam t = server.teams.get(resIds[i-1]);
            sidebar.add(t.chatColor + " " + placements[i-1] + ". " + bold + t.teamname + ChatColor.WHITE + " " + t.getScore(curMinigameStr));
        }
    }

    public abstract void updateScoreboard();

    public abstract void commit();

    public void getData() {
        this.previousScores.clear();

        try {
            Statement statement = MCGMain.conn.connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM `scores` WHERE `uuid` = '" + bukkitPlayer.getUniqueId() + "';");
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

}
