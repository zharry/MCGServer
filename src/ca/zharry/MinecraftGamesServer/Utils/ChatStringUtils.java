package ca.zharry.MinecraftGamesServer.Utils;

public class ChatStringUtils {
    public static String truncateChatString(String chatString, int length) {
        boolean isColor = false;
        int numChars = 0;
        StringBuilder output = new StringBuilder();
        for(char c : chatString.toCharArray()) {
            if(isColor) {
                isColor = false;
                output.append(c);
            } else {
                if (c == '§') {
                    isColor = true;
                    output.append(c);
                } else {
                    if(numChars < length) {
                        output.append(c);
                        ++numChars;
                    }
                }
            }
        }
        return output.toString();
    }
}
