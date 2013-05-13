package pck;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

class DatabaseHandler {
	private static Connection conn = null;
	private static int TIMEOUT = 0;

	/**
	 * connects to database on the path passed on parameters
	 * 
	 * @param dataBasePath
	 */
	static void connectToDataBase(String dataBasePath) {
		File file = new File(dataBasePath);

		if (file.exists()) {
			// register the driver
			String sDriverName = "org.sqlite.JDBC";
			try {
				Class.forName(sDriverName);
			} catch (ClassNotFoundException e) {
				System.out.println("ERROR - EXITING............");
				e.printStackTrace();
				closeConnection();
				System.exit(-1);
			}

			String sDbUrl = "jdbc:sqlite:" + dataBasePath;
			// create a database connection
			try {
				conn = DriverManager.getConnection(sDbUrl);
			} catch (SQLException e) {
				System.out.println("ERROR - EXITING............");
				e.printStackTrace();
				closeConnection();
				System.exit(-1);
			}
		} else {
			System.out
					.println("ERROR - DATABASE FILE DOESN'T EXIST. EXITING............");
			System.exit(-1);
		}
	}

	/**
	 * Executes query on database, returns results
	 * 
	 * @param query
	 * @return result of query
	 * @throws SQLException
	 */
	static ResultSet executeQuery(String query) throws SQLException {
		Statement stmt = conn.createStatement();
		stmt.setQueryTimeout(TIMEOUT);
		ResultSet res = stmt.executeQuery(query);
		return res;
	}

	/**
	 * Executes update or insert on the database.
	 * 
	 * @param update
	 * @throws SQLException
	 */
	static void executeUpdate(String update) throws SQLException {
		Statement stmt = conn.createStatement();
		stmt.setQueryTimeout(TIMEOUT);
		stmt.executeUpdate(update);
	}

	/**
	 * closes connection to database
	 */
	static void closeConnection() {
		if (conn != null) {
			try {
				conn.close();
			} catch (Exception ignore) {
			}
		}
	}
}
