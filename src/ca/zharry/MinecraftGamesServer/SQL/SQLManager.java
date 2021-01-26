package ca.zharry.MinecraftGamesServer.SQL;

import ca.zharry.MinecraftGamesServer.MCGMain;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;
import java.sql.*;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.*;

public class SQLManager {

	private final LinkedBlockingQueue<Connection> pool = new LinkedBlockingQueue<>();
	private final RowSetFactory rowSetFactory = RowSetProvider.newFactory();
	private final ExecutorService executor = Executors.newCachedThreadPool();
	private final LinkedBlockingQueue<WaitingForTickResult> waitingForTick = new LinkedBlockingQueue<>();
	private final ArrayList<Connection> allConnections = new ArrayList<>();

	public SQLManager(String url, Properties properties, int poolSize) throws ClassNotFoundException, SQLException {
		Class.forName("com.mysql.jdbc.Driver");
		properties.setProperty("autoReconnect", "true");
		properties.setProperty("allowMultiQueries", "true");
		for(int i = 0; i < poolSize; ++i) {
			Connection connection = createConnection(url, properties);
			pool.offer(connection);
			allConnections.add(connection);
		}
		MCGMain.logger.info("Made " + poolSize + " connections to SQL");
	}

	private Connection createConnection(String url, Properties properties) throws SQLException {
		return DriverManager.getConnection(url, properties);
	}

	public void tick() {
		WaitingForTickResult result;
		while((result = waitingForTick.poll()) != null) {
			if(result.exception != null) {
				result.future.completeExceptionally(result.exception);
			} else {
				result.future.complete(result.rowSet);
			}
		}
	}

	public void stop() {
		close();
		executor.shutdown();
	}

	public CompletableFuture<CachedRowSet> executeQueryAsyncTick(String query, Object... params) {
		CompletableFuture<CachedRowSet> future = new CompletableFuture<>();

		executor.submit(() -> {
			try {
				waitingForTick.offer(new WaitingForTickResult(future, null, executeQuery(query, params)));
			} catch(Exception e) {
				waitingForTick.offer(new WaitingForTickResult(future, e, null));
				e.printStackTrace();
			}
		});

		return future;
	}

	public CompletableFuture<CachedRowSet> executeQueryAsync(String query, Object... params) {
		CompletableFuture<CachedRowSet> future = new CompletableFuture<>();

		executor.submit(() -> {
			try {
				future.complete(executeQuery(query, params));
			} catch(Exception e) {
				future.completeExceptionally(e);
				e.printStackTrace();
			}
		});

		return future;
	}

	public CachedRowSet executeQuery(String query, Object... params) throws SQLException {
		Connection connection = null;
		try {
			connection = pool.take();

			PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

			for(int i = 0; i < params.length; ++i) {
				statement.setObject(i + 1, params[i]);
			}

			statement.execute();

			try(ResultSet result = statement.getResultSet()) {
				if(result != null) {
					CachedRowSet rowSet = rowSetFactory.createCachedRowSet();
					rowSet.populate(result);
					return rowSet;
				}
			}
			try(ResultSet result = statement.getGeneratedKeys()) {
				if(result != null) {
					CachedRowSet rowSet = rowSetFactory.createCachedRowSet();
					rowSet.populate(result);
					return rowSet;
				}
			}
			return null;
		} catch(InterruptedException e) {
			throw new SQLException("Interrupted while claiming connection from pool", e);
		} finally {
			if(connection != null) {
				pool.offer(connection);
			}
		}
	}

	public void close() {
		for(Connection connection : allConnections) {
			try {
				connection.close();
			} catch(Exception e) {
			}
		}
	}

	private static class WaitingForTickResult {
		public CompletableFuture<CachedRowSet> future;
		public Exception exception;
		public CachedRowSet rowSet;
		public WaitingForTickResult(CompletableFuture<CachedRowSet> future, Exception exception, CachedRowSet rowSet) {
			this.future = future;
			this.exception = exception;
			this.rowSet = rowSet;
		}
	}
}
