package ca.zharry.MinecraftGamesServer.Utils;

import org.bukkit.ChatColor;

public class StringAlignUtils{

    /** Current max length of a line */
    private int maxChars;
    private int paddings;

    public StringAlignUtils(int maxChars, int paddings) {
        if (maxChars < 0) {
            throw new IllegalArgumentException("maxChars must be positive.");
        }
        this.maxChars = maxChars;
        this.paddings = paddings;
    }

    public String center(String s) {
        StringBuffer sb = new StringBuffer();
        String t = ChatColor.stripColor(s);
        int edgePadCnt = (maxChars - t.length()) / 2;
        pad(sb, edgePadCnt);
        sb.append(s);
        pad(sb, edgePadCnt);
        return sb.toString();
    }

    protected final void pad(StringBuffer to, int howMany) {
        for (int i = 0; i < howMany; i++)
            to.append(' ');
    }

    public String merge(String l, String r) {
        StringBuffer centerPadding = new StringBuffer();
        StringBuffer edgePadding = new StringBuffer();
        pad(edgePadding, paddings);
        pad(centerPadding, maxChars - ChatColor.stripColor(l).length() - ChatColor.stripColor(r).length() - 2 * paddings);
        return edgePadding.toString() + l + centerPadding.toString() + r + edgePadding.toString();
    }
}