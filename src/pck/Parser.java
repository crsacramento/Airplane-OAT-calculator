package pck;

import pck.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.DriverManager;
import java.sql.ResultSet;

public class Parser {
	private static Connection conn = null;
	private static File file = null;
	private int TIMEOUT = 0;

	private static HashMap<String, Double> parseFileString(String line) {
		// 23# 1110.00:1000.00,120.00:1110.00,1190.00:900.00,-52.98,-53.21
		// 24# --:--,120.00:1110.00,1190.00:900.00,--,-53.21
		HashMap<String, Double> res = new HashMap<String, Double>();

		String[] components = line.substring(line.indexOf("#") + 1).split(",");
		if (components.length > 0 && components.length == 5) {
			// first pitot read
			String[] pitots = components[0].split(":");
			if (pitots[0].equals(Constants.ERROR)
					|| pitots[1].equals(Constants.ERROR)) {
				// error value
				res.put(Constants.FIRST_PITOT_STATIC, Constants.ERROR_VALUE);
				res.put(Constants.FIRST_PITOT_DYNAMIC, Constants.ERROR_VALUE);
			} else {
				res.put(Constants.FIRST_PITOT_STATIC, Double.valueOf(pitots[0]));
				res.put(Constants.FIRST_PITOT_DYNAMIC,
						Double.valueOf(pitots[1]));
			}

			// second pitot read
			pitots = components[1].split(":");
			if (pitots[0].equals(Constants.ERROR)
					|| pitots[1].equals(Constants.ERROR)) {
				// error value
				res.put(Constants.SECOND_PITOT_STATIC, Constants.ERROR_VALUE);
				res.put(Constants.SECOND_PITOT_DYNAMIC, Constants.ERROR_VALUE);
			} else {
				res.put(Constants.SECOND_PITOT_STATIC,
						Double.valueOf(pitots[0]));
				res.put(Constants.SECOND_PITOT_DYNAMIC,
						Double.valueOf(pitots[1]));
			}

			// second pitot read
			pitots = components[2].split(":");
			if (pitots[0].equals(Constants.ERROR)
					|| pitots[1].equals(Constants.ERROR)) {
				// error value
				res.put(Constants.THIRD_PITOT_STATIC, Constants.ERROR_VALUE);
				res.put(Constants.THIRD_PITOT_DYNAMIC, Constants.ERROR_VALUE);
			} else {
				res.put(Constants.THIRD_PITOT_STATIC, Double.valueOf(pitots[0]));
				res.put(Constants.THIRD_PITOT_DYNAMIC,
						Double.valueOf(pitots[1]));
			}

			// fill first temperature
			if (components[3].equals(Constants.ERROR))
				res.put(Constants.FIRST_TEMP, Constants.ERROR_VALUE);
			else
				res.put(Constants.FIRST_TEMP, Double.valueOf(components[3]));

			// fill second temperature
			if (components[4].equals(Constants.ERROR))
				res.put(Constants.SECOND_TEMP, Constants.ERROR_VALUE);
			else
				res.put(Constants.SECOND_TEMP, Double.valueOf(components[4]));

			return res;

		} else {
			return null;
		}
	}

	/**
	 * Calculates the median of the values in param. Param must be sorted.
	 * 
	 * @param m
	 * @return
	 */
	private static double median(ArrayList<Double> m) {
		int middle = m.size() / 2;
		if (m.size() % 2 == 1) {
			return m.get(middle);
		} else {
			return (m.get(middle - 1) + m.get(middle)) / 2.0;
		}
	}

	private void parseLine(int line_numer) {
		// access file, get specified line
		HashMap<String, Double> results = parseFileString("23# 1110.00:1000.00,120.00:1110.00,1190.00:900.00,-52.98,-53.21");

		if (results == null) {
			System.out.println("ERROR, INVALID FILE LINE");
			System.exit(-1);
		}

		ArrayList<Double> staticPressure = new ArrayList<Double>(), dynamicPressure = new ArrayList<Double>(), temperature = new ArrayList<Double>();

		// initialize static pressure list
		if (!results.get(Constants.FIRST_PITOT_STATIC).equals(
				Constants.ERROR_VALUE)) {
			staticPressure.add(results.get(Constants.FIRST_PITOT_STATIC));
		}
		if (!results.get(Constants.SECOND_PITOT_STATIC).equals(
				Constants.ERROR_VALUE)) {
			staticPressure.add(results.get(Constants.SECOND_PITOT_STATIC));
		}
		if (!results.get(Constants.THIRD_PITOT_STATIC).equals(
				Constants.ERROR_VALUE)) {
			staticPressure.add(results.get(Constants.THIRD_PITOT_STATIC));
		}

		// initialize dynamic pressure list
		if (!results.get(Constants.FIRST_PITOT_DYNAMIC).equals(
				Constants.ERROR_VALUE)) {
			dynamicPressure.add(results.get(Constants.FIRST_PITOT_DYNAMIC));
		}
		if (!results.get(Constants.SECOND_PITOT_DYNAMIC).equals(
				Constants.ERROR_VALUE)) {
			dynamicPressure.add(results.get(Constants.SECOND_PITOT_DYNAMIC));
		}
		if (!results.get(Constants.THIRD_PITOT_DYNAMIC).equals(
				Constants.ERROR_VALUE)) {
			dynamicPressure.add(results.get(Constants.THIRD_PITOT_DYNAMIC));
		}

		// initialize temperature list
		if (!results.get(Constants.FIRST_TEMP).equals(Constants.ERROR_VALUE)) {
			temperature.add(results.get(Constants.FIRST_TEMP));
		}
		if (!results.get(Constants.SECOND_TEMP).equals(Constants.ERROR_VALUE)) {
			temperature.add(results.get(Constants.SECOND_TEMP));
		}
		// sort lists
		Collections.sort(staticPressure);
		Collections.sort(dynamicPressure);
		Collections.sort(temperature);

		// TODO remove
		System.out.println(staticPressure.toString());
		System.out.println(dynamicPressure.toString());
		System.out.println(temperature.toString());

		// get medians
		double staticPressureValue = median(staticPressure), dynamicPressureValue = median(dynamicPressure), temperatureValue = median(temperature);

		System.out.println(staticPressureValue + "_" + dynamicPressureValue
				+ "_" + temperatureValue);

		double[] arr = Calculator.calculateTAS_OAT(staticPressureValue,
				dynamicPressureValue, temperatureValue);
		System.out.println("RESULTS: " + (arr[0] - Constants.KELVIN) + " ºC|"
				+ arr[1] + " knots");
	}

	/**
	 * connects to database on the path passed on parameters
	 * 
	 * @param dataBasePath
	 */
	private static void connectToDataBase(String dataBasePath) {
		File file = new File(dataBasePath);

		if (file.exists()) // here's how to check
		{
		
		// register the driver
		String sDriverName = "org.sqlite.JDBC";
		try {
			Class.forName(sDriverName);
		} catch (ClassNotFoundException e) {
			System.out.println("ERROR - EXITING");
			e.printStackTrace();
			closeConnection();
			System.exit(-1);
		}

		String sDbUrl = "jdbc:sqlite:" + dataBasePath;
		// create a database connection
		try {
			conn = DriverManager.getConnection(sDbUrl);
		} catch (SQLException e) {
			System.out.println("ERROR - EXITING");
			e.printStackTrace();
			closeConnection();
			System.exit(-1);
		}
		}else{
			System.out.println("ERROR - DATABASE FILE DOESN'T EXIST. EXITING............");
			System.exit(-1);
		}
	}

	/**
	 * Executes query on database, returns results
	 * 
	 * @param query
	 * @return result of query
	 */
	private ResultSet executeQuery(String query) {
		try {
			Statement stmt = conn.createStatement();
			stmt.setQueryTimeout(TIMEOUT);
			ResultSet res = stmt.executeQuery(query);
			return res;
		} catch (SQLException e) {
			System.out.println("ERROR - EXITING............");
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	/**
	 * Executes update or insert on the database.
	 * 
	 * @param update
	 */
	private void executeUpdate(String update) {
		try {
			Statement stmt = conn.createStatement();
			stmt.setQueryTimeout(TIMEOUT);
			stmt.executeUpdate(update);
		} catch (SQLException e) {
			System.out.println("ERROR - EXITING............");
			e.printStackTrace();
			closeConnection();
			System.exit(-1);
		}
	}

	/**
	 * closes connection to database
	 */
	public static void closeConnection() {
		if (conn != null) {
			try {
				conn.close();
			} catch (Exception ignore) {
			}
		}
	}

	private static int countLines(String filename) throws IOException {
		LineNumberReader reader = new LineNumberReader(new FileReader(filename));
		int cnt = 0;
		String lineRead = "";
		while ((lineRead = reader.readLine()) != null) {
		}
		cnt = reader.getLineNumber();
		reader.close();
		return cnt;
	}
	
	/**
	 * Checks arguments passed through command line.
	 * @param args
	 */
	private static void checkArgs(String[] args) {
		// must have 3 args: file db line_number
		if (args.length != 3) {
			System.out
					.println("Usage: Parser <path file to process>\n\t\t<path to .db file>\n\t\t<line number>");
			System.exit(-1);
		}

		// checks file
		file = new File(args[0]);
		if (!file.exists()) {
			System.out.println("ERROR - FILE DOESN'T EXIST, EXITING............");
			System.exit(-1);
		}
		
		// connects to database
		connectToDataBase(args[1]);

		// checks validity of line number
		try {
			int number = countLines(args[0]);
			if(Integer.parseInt(args[2]) > number){
				System.out.println("ERROR - LINE NUMBER IS SUPERIOR TO NUMBER OF LINES IN FILE (" + number + "). EXITING............");
				closeConnection();
				System.exit(-1);
			}
		} catch (IOException e) {
			System.out.println("ERROR - EXITING............");
			e.printStackTrace();
			closeConnection();
			System.exit(-1);
		}
		
		System.out.println("LE YAY");
		closeConnection();
		//
	}
	
	public static void main(String[] args){
		checkArgs(args);
		
	}
}
