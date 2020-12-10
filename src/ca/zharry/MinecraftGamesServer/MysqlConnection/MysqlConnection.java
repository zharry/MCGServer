package ca.zharry.MinecraftGamesServer.MysqlConnection;

import ca.zharry.MinecraftGamesServer.MCGMain;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MysqlConnection {

    String host, port, database, username, password;
    public Connection connection;

    public MysqlConnection(String host, String port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;

        try {
            this.openConnection();
            MCGMain.logger.info("Connected to MySQL!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openConnection() throws SQLException, ClassNotFoundException {
        if (connection != null && !connection.isClosed()) {
            return;
        }
        Class.forName("com.mysql.jdbc.Driver");
        connection = DriverManager.getConnection("jdbc:mysql://"
                        + this.host + ":" + this.port + "/" + this.database,
                this.username, this.password);
    }

}
