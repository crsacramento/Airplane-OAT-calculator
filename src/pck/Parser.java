package pck;

import pck.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilePermission;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.DriverManager;
import java.sql.ResultSet;

class Parser {
	private static File file = null;
	private static String filePath = "";
	protected static String databaseFilePath = "";
	private static int line_number = -1;
	private static int var_id = 3;

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

	private static HashMap<String, Double> parseLine(String line) {
		// access file, get specified line
		// HashMap<String, Double> results =
		// parseFileString("23# 1110.00:1000.00,120.00:1110.00,1190.00:900.00,-52.98,-53.21");

		System.out.println("Line parsed: " + line);
		HashMap<String, Double> results = parseFileString(line);
		if (results == null) {
			System.out.println("ERROR - EXITING............");
			DatabaseHandler.closeConnection();
			System.exit(-1);
		}

		ArrayList<Double> staticPressure = new ArrayList<Double>(), dynamicPressure = new ArrayList<Double>(), temperature = new ArrayList<Double>();

		// initialize static pressure list
		if (!results.get(Constants.FIRST_PITOT_STATIC).equals(
				Constants.ERROR_VALUE)) {
			staticPressure.add(results.get(Constants.FIRST_PITOT_STATIC));
		} else {
			// sensor had an error, ignore input and write to database to ignore
			// future iterations
			/* var_id,sensor,iters_to_ignore,previous_failure, */
			modifyBlocksBD(Constants.FIRST_PITOT_STATIC);
		}
		if (!results.get(Constants.SECOND_PITOT_STATIC).equals(
				Constants.ERROR_VALUE)) {
			staticPressure.add(results.get(Constants.SECOND_PITOT_STATIC));
		} else {
			// sensor had an error, ignore input and write to database to ignore
			// future iterations
			modifyBlocksBD(Constants.SECOND_PITOT_STATIC);
		}
		if (!results.get(Constants.THIRD_PITOT_STATIC).equals(
				Constants.ERROR_VALUE)) {
			staticPressure.add(results.get(Constants.THIRD_PITOT_STATIC));
		} else {
			// sensor had an error, ignore input and write to database to ignore
			// future iterations
			modifyBlocksBD(Constants.THIRD_PITOT_STATIC);
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
		} else {
			// sensor had an error, ignore input and write to database to ignore
			// future iterations
			/* var_id,sensor,iters_to_ignore,previous_failure, */
			modifyBlocksBD(Constants.FIRST_TEMP);
		}
		if (!results.get(Constants.SECOND_TEMP).equals(Constants.ERROR_VALUE)) {
			temperature.add(results.get(Constants.SECOND_TEMP));
		} else {
			// sensor had an error, ignore input and write to database to ignore
			// future iterations
			/* var_id,sensor,iters_to_ignore,previous_failure, */
			modifyBlocksBD(Constants.SECOND_TEMP);
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

		System.out.println("staticPressureValue: " + staticPressureValue + "\ndynamicPressureValue: " + dynamicPressureValue
				+ "\ntemperatureValue: " + temperatureValue);

		double[] arr = Calculator.calculateTAS_OAT(staticPressureValue,
				dynamicPressureValue, temperatureValue);
		System.out.println("RESULTS: " + (arr[0] - Constants.KELVIN) + " ºC|"
				+ arr[1] + " knots");
		return results;
	}

	private static int countLines(String filename) throws IOException {
		LineNumberReader reader = new LineNumberReader(new FileReader(filename));
		int cnt = 0;
		while ((reader.readLine()) != null) {
		}
		cnt = reader.getLineNumber();
		reader.close();
		return cnt;
	}

	/**
	 * Checks arguments passed through command line.
	 * 
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
			System.out
					.println("ERROR - FILE DOESN'T EXIST, EXITING............");
			System.exit(-1);
		} else {
			filePath = args[0];
		}

		// connects to database
		DatabaseHandler.connectToDataBase(args[1]);
		databaseFilePath = args[1];

		// checks validity of line number
		try {
			int number = countLines(args[0]);
			if (Integer.parseInt(args[2]) > number) {
				System.out
						.println("ERROR - LINE NUMBER IS SUPERIOR TO NUMBER OF LINES IN FILE ("
								+ number + "). EXITING............");
				DatabaseHandler.closeConnection();
				System.exit(-1);
			} else {
				line_number = Integer.parseInt(args[2]);
			}
		} catch (IOException e) {
			System.out.println("ERROR - EXITING............");
			e.printStackTrace();
			DatabaseHandler.closeConnection();
			System.exit(-1);
		}

		System.out.println("Arguments are correct.");
	}

	private static String readSpecificLine(int line_num) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(filePath)));
		for (int i = 0; i < line_num - 1; ++i)
			br.readLine();
		String res = br.readLine();
		br.close();
		return res;
	}

	public static void modifyBlocksBD(String sensorInError) {
		/* var_id,sensor,iters_to_ignore,previous_failure, */
		String sensor = "", update = "", query = "";
		int iters = 0, previous_failures = 0;
		ResultSet query_result = null;
		// determine the sensor involved
		switch (sensorInError) {
		case Constants.FIRST_PITOT_DYNAMIC:
		case Constants.FIRST_PITOT_STATIC: {
			sensor += Constants.DB_P1;
			iters = Constants.ITERS_IGNORE_PRESSURE;
			previous_failures = 1;
			break;
		}
		case Constants.SECOND_PITOT_DYNAMIC:
		case Constants.SECOND_PITOT_STATIC: {
			sensor += Constants.DB_P2;
			iters = Constants.ITERS_IGNORE_PRESSURE;
			previous_failures = 1;
			break;
		}
		case Constants.THIRD_PITOT_DYNAMIC:
		case Constants.THIRD_PITOT_STATIC: {
			sensor += Constants.DB_P3;
			iters = Constants.ITERS_IGNORE_PRESSURE;
			previous_failures = 1;
			break;
		}
		case Constants.FIRST_TEMP: {
			sensor += Constants.DB_T1;
			iters = Constants.ITERS_IGNORE_TEMP;
			previous_failures = 1;
			break;
		}
		case Constants.SECOND_TEMP: {
			sensor += Constants.DB_T2;
			iters = Constants.ITERS_IGNORE_TEMP;
			previous_failures = 1;
			break;
		}
		default:
			System.out.println("WRONG SENSOR INPUT");
			DatabaseHandler.closeConnection();
			System.exit(-1);
		}

		query = "select * from blocks where var_id = " + var_id
				+ " and sensor = '" + sensor + "'";
		System.out.println("query = " + query);

		// execute query, check if there's a row in the table referring to this
		// variant and sensor
		try {
			query_result = DatabaseHandler.executeQuery(query);
		} catch (SQLException e) {
			System.out.println("ERROR - EXITING............");
			e.printStackTrace();
			DatabaseHandler.closeConnection();
			System.exit(-1);
		}

		try {
			if (!query_result.next()) { // if empty set, since fucker doesnt have a size method -.-
				// query didnt return results, new row will be inserted
				update = "insert into blocks values(" + var_id + ",'" + sensor
						+ "'," + iters + "," + previous_failures + ")";
				System.out.println("update = " + update);
				System.out.println("Row to update:" + update);
				try {
					DatabaseHandler.executeUpdate(update);
				} catch (SQLException e) {
					System.out.println("ERROR - EXITING............");
					e.printStackTrace();
					DatabaseHandler.closeConnection();
					System.exit(-1);
				}
				System.out.println("Row successfully inserted.");
			} else {
				/*
				 * query returned results, row shall be analysed and updated
				 * accordingly: 1. if prev_failure=2 = do nothing, sensor is banned
				 * forever; 2. if iters>0 && prev_failure=1 = iters--; 3. if iters=0
				 * && prev_failure=1 = prev_failure = 2; else error
				 */
				try {
					iters = Integer.parseInt(query_result
							.getString("iters_to_ignore"));
					previous_failures = Integer.parseInt(query_result
							.getString("previous_failure"));
					System.out.println("iters = " + iters + "| previous failures = " + previous_failures);
				} catch (NumberFormatException e) {
					System.out.println("ERROR - EXITING............");
					e.printStackTrace();
					DatabaseHandler.closeConnection();
					System.exit(-1);
				} catch (SQLException e) {
					System.out.println("ERROR - EXITING............");
					e.printStackTrace();
					DatabaseHandler.closeConnection();
					System.exit(-1);
				}

				if (previous_failures == 1) {
					if (iters > 0) {
						// iters--
						update = "update blocks set iters_to_ignore = "
								+ (iters - 1) + " where var_id = " + var_id
								+ " and sensor = '" + sensor + "'";
						System.out.println("update = " + update);
						System.out.println("Row to update:" + update);
						try {
							DatabaseHandler.executeUpdate(update);
						} catch (SQLException e) {
							System.out.println("ERROR - EXITING............");
							e.printStackTrace();
							DatabaseHandler.closeConnection();
							System.exit(-1);
						}
						System.out.println("Row successfully modified.");
					} else if (iters == 0) {
						// previous_failures = 2
						update = "update blocks set previous_failure = 2 where var_id = " + var_id
								+ " and sensor = '" + sensor + "'";
						System.out.println("Row to update:" + update);
						try {
							DatabaseHandler.executeUpdate(update);
						} catch (SQLException e) {
							System.out.println("ERROR - EXITING............");
							e.printStackTrace();
							DatabaseHandler.closeConnection();
							System.exit(-1);
						}
						System.out.println("Row successfully modified.");
					} else {
						// error
						System.out.println("ERROR - prev_failure!=1 && prev_failure != 1 or iters < 0 - EXITING");
						DatabaseHandler.closeConnection();
						System.exit(-1);
					}
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// checks arguments
		checkArgs(args);

		// extracts, verifies and processes line
		if (!filePath.equals("")) {
			try {
				String line = readSpecificLine(line_number);
				HashMap<String, Double> results = parseLine(line);
				// write to database = line_num, var_id, pd1, ps1, pd2, ps2,
				// pd3, ps3, tp1, tp2
				String update = "insert into inputs values(" + line_number
						+ "," + var_id + ","
						+ results.get(Constants.FIRST_PITOT_DYNAMIC) + ","
						+ results.get(Constants.FIRST_PITOT_STATIC) + ","
						+ results.get(Constants.SECOND_PITOT_DYNAMIC) + ","
						+ results.get(Constants.SECOND_PITOT_STATIC) + ","
						+ results.get(Constants.THIRD_PITOT_DYNAMIC) + ","
						+ results.get(Constants.THIRD_PITOT_STATIC) + ","
						+ results.get(Constants.FIRST_TEMP) + ","
						+ results.get(Constants.SECOND_TEMP) + ")";
				System.out.println("Row to insert:" + update);
				DatabaseHandler.executeUpdate(update);
				System.out.println("Row successfully inserted.");

				// for testing purposes
				ResultSet res = DatabaseHandler
						.executeQuery("select * from inputs where var_id = "
								+ var_id);
				while (res.next()) {
					System.out.println(res.getString("line_num") + "|"
							+ res.getString("var_id") + "|"
							+ res.getString("pd1") + "|" + res.getString("ps1")
							+ "|" + res.getString("pd2") + "|"
							+ res.getString("ps2") + "|" + res.getString("pd3")
							+ "|" + res.getString("ps3") + "|"
							+ res.getString("tp1") + "|" + res.getString("tp2")
							+ "|");
				}
				res = DatabaseHandler
						.executeQuery("select * from blocks where var_id = "
								+ var_id);/* var_id + ",'" + sensor
						+ "'," + iters + "," + previous_failures + ")"*/
				while (res.next()) {
					System.out.println(res.getString("var_id") + "|"
							+ res.getString("sensor") + "|" + res.getString("iters")
							+ "|" + res.getString("previous_failures") + "|");
				}
			} catch (IOException e) {
				System.out.println("ERROR - EXITING............");
				e.printStackTrace();
				DatabaseHandler.closeConnection();
				System.exit(-1);
			} catch (SQLException e) {
				System.out.println("ERROR - EXITING............");
				e.printStackTrace();
				DatabaseHandler.closeConnection();
				System.exit(-1);
			}
		}
	}
}
