package ca.zharry.MinecraftGamesServer.Utils;

import ca.zharry.MinecraftGamesServer.MCGMain;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.UUID;

public class SQLQueryUtil {
    public static HashMap<UUID, String> queryAllPlayers(int season) {
        HashMap<UUID, String> users = null;
        try {
            users = new HashMap<>();
            Statement statement = MCGMain.conn.connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM `usernames` WHERE `season` = " + season + ";");
            while (resultSet.next()) {
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                String username = resultSet.getString("username");
                users.put(uuid, username);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }
}
