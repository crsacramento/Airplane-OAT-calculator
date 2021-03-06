package pck;

import pck.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.sql.SQLException;
import java.sql.ResultSet;

class Parser {
	private static File file = null;
	private static String filePath = "";
	protected static String databaseFilePath = "";
	private static int line_number = -1;
	private static int var_id = 3;
	private static HashMap<String, Boolean> validSensors = new HashMap<String, Boolean>();

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

	private static double[] parseLine(String line) {
		// access file, get specified line
		// HashMap<String, Double> results =
		// parseFileString("23# 1110.00:1000.00,120.00:1110.00,1190.00:900.00,-52.98,-53.21");

		//System.out.println("Line parsed: " + line);
		HashMap<String, Double> results = parseFileString(line);
		if (results == null) {
			// System.out.println("ERROR - EXITING............");
			System.out.println("FAIL_SAFE(1)");
			DatabaseHandler.closeConnection();
			System.exit(-1);
		}

		ArrayList<Double> staticPressure = new ArrayList<Double>(),
				dynamicPressure = new ArrayList<Double>(),
				temperature = new ArrayList<Double>();

		// initialize static pressure list
		if (validSensors.get(Constants.DB_P1)) {
			if (testInputs(results.get(Constants.FIRST_PITOT_STATIC),
					Constants.FIRST_PITOT_STATIC)) {
				dynamicPressure.add(results.get(Constants.FIRST_PITOT_STATIC));
			} else {
				// sensor had an error, ignore input and write to database to
				// ignore future iterations
				/* var_id,sensor,iters_to_ignore,previous_failure, */
				modifyBlocksBD(Constants.DB_P1);
			}

			if (testInputs(results.get(Constants.FIRST_PITOT_DYNAMIC),
					Constants.FIRST_PITOT_DYNAMIC)) {
				staticPressure.add(results.get(Constants.FIRST_PITOT_DYNAMIC));
			}
		} else {
			modifyBlocksBD(Constants.DB_P1);
		}

		if (validSensors.get(Constants.DB_P2)) {
			if (testInputs(results.get(Constants.SECOND_PITOT_STATIC),
					Constants.SECOND_PITOT_STATIC)) {
				dynamicPressure.add(results.get(Constants.SECOND_PITOT_STATIC));
			} else {
				modifyBlocksBD(Constants.DB_P2);
			}

			if (testInputs(results.get(Constants.SECOND_PITOT_DYNAMIC),
					Constants.SECOND_PITOT_DYNAMIC)) {
				staticPressure
						.add(results.get(Constants.SECOND_PITOT_DYNAMIC));
			}
		} else {
			modifyBlocksBD(Constants.DB_P2);
		}

		if (validSensors.get(Constants.DB_P3)) {
			if (testInputs(results.get(Constants.THIRD_PITOT_STATIC),
					Constants.THIRD_PITOT_STATIC)) {
				dynamicPressure.add(results.get(Constants.THIRD_PITOT_STATIC));
			} else {
				modifyBlocksBD(Constants.DB_P3);
			}

			if (testInputs(results.get(Constants.THIRD_PITOT_DYNAMIC),
					Constants.THIRD_PITOT_DYNAMIC)) {
				staticPressure.add(results.get(Constants.THIRD_PITOT_DYNAMIC));
			}
		} else {
			modifyBlocksBD(Constants.DB_P3);
		}

		// initialize temperature list
		if (validSensors.get(Constants.DB_T1)) {
			if (testInputs(results.get(Constants.FIRST_TEMP),
					Constants.FIRST_TEMP)) {
				temperature.add(results.get(Constants.FIRST_TEMP));
			} else {
				modifyBlocksBD(Constants.DB_T1);
			}
		} else {
			modifyBlocksBD(Constants.DB_T1);
		}
		if (validSensors.get(Constants.DB_T2)) {
			if (testInputs(results.get(Constants.SECOND_TEMP),
					Constants.SECOND_TEMP)) {
				temperature.add(results.get(Constants.SECOND_TEMP));
			} else {
				modifyBlocksBD(Constants.DB_T2);
			}
		} else {
			modifyBlocksBD(Constants.DB_T2);
		}

		// sort lists
		Collections.sort(staticPressure);
		Collections.sort(dynamicPressure);
		Collections.sort(temperature);

		// get medians
		double staticPressureValue = median(staticPressure), dynamicPressureValue = median(dynamicPressure), temperatureValue = median(temperature);

		// test if inputs are in [p-10%, p+10%] -> p: median of inputs received
		/*for (Double d : staticPressure) {
			if (d < (staticPressureValue * 0.9)
					|| d > (staticPressureValue * 1.1)) {
				// System.out.println("ERROR - STATIC PRESSURE VALUE " + d +
				// " IS OUT OF RANGE [" + (staticPressureValue * 0.9) + "," +
				// (staticPressureValue * 1.1) + "]");
				System.out.println("FAIL_SAFE(2)");
				DatabaseHandler.closeConnection();
				System.exit(-1);
			}
		}
		for (Double d : dynamicPressure) {
			if (d < (dynamicPressureValue * 0.9)
					|| d > (dynamicPressureValue * 1.1)) {
				// System.out.println("ERROR - DYNAMIC PRESSURE VALUE " + d +
				// " IS OUT OF RANGE [" + (dynamicPressureValue * 0.9) + "," +
				// (dynamicPressureValue * 1.1) + "]");
				System.out.println("FAIL_SAFE(3)");
				DatabaseHandler.closeConnection();
				System.exit(-1);
			}
		}*/
		/*for (Double d : temperature) {
			if (d < (temperatureValue * 0.9) || d > (temperatureValue * 1.1)) {
				 System.out.println("ERROR - TEMPERATURE VALUE " + d +
				 " IS OUT OF RANGE [" + (temperatureValue * 0.9) + "," +
				 (temperatureValue * 1.1) + "]");
				System.out.println("FAIL_SAFE(4)");
				DatabaseHandler.closeConnection();
				System.exit(-1);
			}
		}*/

		/*
		 * System.out.println("static pressure value: " + staticPressureValue +
		 * "\ndynamic pressure value: " + dynamicPressureValue +
		 * "\ntemperature value: " + temperatureValue);
		 */

		String update = "insert into inputs values(" + line_number + ","
				+ var_id + "," + results.get(Constants.FIRST_PITOT_DYNAMIC)
				+ "," + results.get(Constants.FIRST_PITOT_STATIC) + ","
				+ results.get(Constants.SECOND_PITOT_DYNAMIC) + ","
				+ results.get(Constants.SECOND_PITOT_STATIC) + ","
				+ results.get(Constants.THIRD_PITOT_DYNAMIC) + ","
				+ results.get(Constants.THIRD_PITOT_STATIC) + ","
				+ results.get(Constants.FIRST_TEMP) + ","
				+ results.get(Constants.SECOND_TEMP) + ")";
		// System.out.println("Row to insert:" + update);
		try {
			DatabaseHandler.executeUpdate(update);
		} catch (SQLException e) {
			System.out.println("FAIL_SAFE(5)");
			DatabaseHandler.closeConnection();
			System.exit(-1);
		}
		
		double[] arr = Calculator.calculateTAS_OAT(staticPressureValue,
				dynamicPressureValue, temperatureValue);
		arr[1]=arr[1]-Constants.KELVIN;
		/*
		 * System.out.println("RESULTS: " + (arr[0] - Constants.KELVIN) + " �C|"
		 * + arr[1] + " knots");
		 */
		return arr;
	}

	/**
	 * Tests input for the following conditions: 1. if result == -1 (had -- on
	 * the file) 2. if pressure -> see last 4 lines, read if all the inputs are
	 * equal if !pressure -> same procedure, only check 10 inputs 3. if pressure
	 * -> check if value is in [100;1500] if !pressure -> check if value is in
	 * [-100;100] 4. check if input is out of interval [-0.1 + p;p+0.1] p being
	 * the median of the inputs received
	 * 
	 * @param value
	 *            value of input
	 * @param type
	 *            which input it is
	 * @return true/false (if input is valid or not)
	 */
	private static boolean testInputs(Double value, String type) {
		ResultSet rs = null;
		String query = "";
		ArrayList<Double> list = new ArrayList<Double>();
		// verify type passed as params
		if (type.equals(Constants.FIRST_PITOT_DYNAMIC)
				|| type.equals(Constants.FIRST_PITOT_STATIC)
				|| type.equals(Constants.SECOND_PITOT_DYNAMIC)
				|| type.equals(Constants.SECOND_PITOT_STATIC)
				|| type.equals(Constants.THIRD_PITOT_DYNAMIC)
				|| type.equals(Constants.THIRD_PITOT_STATIC)) {
			// check if value is within limits
			if (value < Constants.MIN_LIMIT_PRESSURE
					|| value > Constants.MAX_LIMIT_PRESSURE)
				return false;

			// check 4 last inserted lines on table inputs
			query = "select " + type.toLowerCase()
					+ " from inputs where var_id = " + var_id
					+ " and line_num < " + line_number + " limit "
					+ Constants.ITERS_IGNORE_PRESSURE;
			//System.out.println("query = " + query);
			try {
				rs = DatabaseHandler.executeQuery(query);
			} catch (SQLException e) {
				e.printStackTrace();
				DatabaseHandler.closeConnection();
				System.exit(-1);
			}

			try {
				if (!rs.next()) // no results to query
					return true;
				else {
					while (!rs.next()) {
						list.add(Double.parseDouble(rs.getString(type
								.toLowerCase())));
					}
				}
			} catch (SQLException e) {
				// System.out.println("ERROR - EXITING");
				// e.printStackTrace();
				System.out.println("FAIL_SAFE(6)");
				DatabaseHandler.closeConnection();
				System.exit(-1);
			}

			// if there are no previous lines
			if (list.isEmpty())
				return true;
			else {
				if (list.size() == Constants.ITERS_IGNORE_PRESSURE) {
					// check if previous inputs are equal,
					// and if next input is equal to the previous ones
					boolean b = false;
					Double prev = list.get(0);
					for (Double d : list) {
						if (d != prev) {
							b = true;
							break;
						}
					}
					// if all values are equal, checks new input
					if (!b) {
						if (value == prev) {
							return false;
						}
					}
				}
				return true;
			}
		} else if (type.equals(Constants.FIRST_TEMP)
				|| type.equals(Constants.SECOND_TEMP)) {
			// check if value is within limits
			if (value < Constants.MIN_LIMIT_TEMP
					|| value > Constants.MAX_LIMIT_TEMP)
				return false;
			// check 10 last inserted lines on table inputs
			query = "select " + type.toLowerCase()
					+ " from inputs where var_id = " + var_id
					+ " and line_num < " + line_number + " limit "
					+ Constants.ITERS_IGNORE_TEMP;
			try {
				rs = DatabaseHandler.executeQuery(query);
			} catch (SQLException e) {
				e.printStackTrace();
				DatabaseHandler.closeConnection();
				System.exit(-1);
			}

			try {
				if (!rs.next()) // no results to query
					return true;
				else {
					while (!rs.next()) {
						list.add(Double.parseDouble(rs.getString(type
								.toLowerCase())));
					}
				}
			} catch (SQLException e) {
				// System.out.println("ERROR - EXITING");
				// e.printStackTrace();
				System.out.println("FAIL_SAFE(7)");
				DatabaseHandler.closeConnection();
				System.exit(-1);
			}

			// if there are no previous lines
			if (list.isEmpty())
				return true;
			else {
				if (list.size() == Constants.ITERS_IGNORE_TEMP) {
					// check if previous inputs are equal,
					// and if next input is equal to the previous ones
					boolean b = false;
					Double prev = list.get(0);
					for (Double d : list) {
						if (d != prev) {
							b = true;
							break;
						}
					}
					// if all values are equal, checks new input
					if (!b) {
						if (value == prev) {
							return false;
						}
					}
				}
				return true;
			}
		} else
			return false;
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
			// System.out.println("ERROR - FILE DOESN'T EXIST, EXITING");
			System.out.println("FAIL_SAFE(8)");
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
				/*
				 * System.out .println(
				 * "ERROR - LINE NUMBER IS SUPERIOR TO NUMBER OF LINES IN FILE ("
				 * + number + "). EXITING");
				 */
				System.out.println("FAIL_SAFE(9)");
				DatabaseHandler.closeConnection();
				System.exit(-1);
			} else {
				line_number = Integer.parseInt(args[2]);
			}
		} catch (IOException e) {
			// System.out.println("ERROR - EXITING............");
			// e.printStackTrace();
			System.out.println("FAIL_SAFE(10)");
			DatabaseHandler.closeConnection();
			System.exit(-1);
		}

		//System.out.println("Arguments are correct.");
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

	private static void modifyBlocksBD(String sensorInError) {
		/* var_id,sensor,iters_to_ignore,previous_failure, */
		String sensor = "", update = "", query = "";
		int iters = 0, previous_failures = 0;
		ResultSet query_result = null;
		// determine the sensor involved
		if (sensorInError.equals(Constants.DB_P1)) {
			sensor = Constants.DB_P1;
			iters = Constants.ITERS_IGNORE_PRESSURE;
			previous_failures = 1;
		}
		else if (sensorInError.equals(Constants.DB_P2)) {
			sensor += Constants.DB_P2;
			iters = Constants.ITERS_IGNORE_PRESSURE;
			previous_failures = 1;
		}
		else if (sensorInError.equals(Constants.DB_P3)) {
			sensor += Constants.DB_P3;
			iters = Constants.ITERS_IGNORE_PRESSURE;
			previous_failures = 1;
		}
		else if (sensorInError.equals(Constants.DB_T1)) {
			sensor += Constants.DB_T1;
			iters = Constants.ITERS_IGNORE_TEMP;
			previous_failures = 1;
		}
		else if (sensorInError.equals(Constants.DB_T2)) {
			sensor += Constants.DB_T2;
			iters = Constants.ITERS_IGNORE_TEMP;
			previous_failures = 1;
		}
		else {
			// System.out.println("WRONG SENSOR INPUT (" + sensorInError +
			// ") - EXITING");
			System.out.println("FAIL_SAFE(11)");
			DatabaseHandler.closeConnection();
			System.exit(-1);
		}

		query = "select * from blocks where var_id = " + var_id
				+ " and sensor = '" + sensor + "'";
		// System.out.println("query = " + query);

		// execute query, check if there's a row in the table referring to this
		// variant and sensor
		try {
			query_result = DatabaseHandler.executeQuery(query);
		} catch (SQLException e) {
			// System.out.println("ERROR - EXITING............");
			// e.printStackTrace();
			System.out.println("FAIL_SAFE(12)");
			DatabaseHandler.closeConnection();
			System.exit(-1);
		}

		try {
			if (!query_result.next()) { // if empty set, since fucker doesnt
										// have a size method -.-
				// query didnt return results, new row will be inserted
				update = "insert into blocks values(" + var_id + ",'" + sensor
						+ "'," + iters + "," + previous_failures + ")";
				//System.out.println("update = " + update);
				//System.out.println("Row to update:" + update);
				try {
					DatabaseHandler.executeUpdate(update);
				} catch (SQLException e) {
					// System.out.println("ERROR - EXITING............");
					// e.printStackTrace();
					System.out.println("FAIL_SAFE(12)");
					DatabaseHandler.closeConnection();
					System.exit(-1);
				}
				//System.out.println("Row successfully inserted.");
			} else {
				/*
				 * query returned results, row shall be analysed and updated
				 * accordingly: 1. if prev_failure=2 = do nothing, sensor is
				 * banned forever; 2. if iters>0 && prev_failure=1 = iters--; 3.
				 * if iters=0 && prev_failure=1 = prev_failure = 2; else error
				 */
				try {
					iters = Integer.parseInt(query_result
							.getString("iters_to_ignore"));
					previous_failures = Integer.parseInt(query_result
							.getString("previous_failure"));
					//System.out.println("iters = " + iters
						//	+ "| previous failures = " + previous_failures);
				} catch (NumberFormatException e) {
					// System.out.println("ERROR - EXITING............");
					// e.printStackTrace();
					System.out.println("FAIL_SAFE(13)");
					DatabaseHandler.closeConnection();
					System.exit(-1);
				} catch (SQLException e) {
					// System.out.println("ERROR - EXITING............");
					// e.printStackTrace();
					System.out.println("FAIL_SAFE(14)");
					DatabaseHandler.closeConnection();
					System.exit(-1);
				}

				if (previous_failures == 1) {
					if (iters > 0) {
						// iters--
						update = "update blocks set iters_to_ignore = "
								+ (iters - 1) + " where var_id = " + var_id
								+ " and sensor = '" + sensor + "'";
						//System.out.println("update = " + update);
						//System.out.println("Row to update:" + update);
						try {
							DatabaseHandler.executeUpdate(update);
						} catch (SQLException e) {
							// System.out.println("ERROR - EXITING............");
							// e.printStackTrace();
							DatabaseHandler.closeConnection();
							System.exit(-1);
						}
						// System.out.println("Row successfully modified.");
					} else if (iters == 0) {
						// previous_failures = 2
						update = "update blocks set previous_failure = 2 where var_id = "
								+ var_id + " and sensor = '" + sensor + "'";
						// System.out.println("Row to update:" + update);
						try {
							DatabaseHandler.executeUpdate(update);
						} catch (SQLException e) {
							// System.out.println("ERROR - EXITING............");
							// e.printStackTrace();
							System.out.println("FAIL_SAFE(15)");
							DatabaseHandler.closeConnection();
							System.exit(-1);
						}
						// System.out.println("Row successfully modified.");
					} else {
						// error
						/*
						 * System.out .println(
						 * "ERROR - prev_failure!=1 && prev_failure != 1 or iters < 0 - EXITING"
						 * );
						 */
						System.out.println("FAIL_SAFE(16)");
						DatabaseHandler.closeConnection();
						System.exit(-1);
					}
				}
			}
		} catch (SQLException e) {
			// System.out.println("ERROR - EXITING");
			// e.printStackTrace();
			System.out.println("FAIL_SAFE(17)");
			DatabaseHandler.closeConnection();
			System.exit(-1);
		}
	}

	private static void initValidSensors() {
		ResultSet res = null;
		try {
			res = DatabaseHandler
					.executeQuery("select * from blocks where var_id = "
							+ var_id);
		} catch (SQLException e) {
			// System.out.println("ERROR - EXITING");
			// e.printStackTrace();
			System.out.println("FAIL_SAFE(18)");
			DatabaseHandler.closeConnection();
			System.exit(-1);
		}/*
		 * var_id + ",'" + sensor + "'," + iters + "," + previous_failures + ")"
		 */
		try {
			while (res.next()) {
				String sensor = res.getString("sensor");
				int iters = Integer.parseInt(res.getString("iters_to_ignore"));
				int previous_failures = Integer.parseInt(res
						.getString("previous_failure"));
				
				if (sensor.toUpperCase().equals(Constants.DB_P1)) {
					if (iters == 0 && previous_failures < 2)
						validSensors.put(Constants.DB_P1, true);
					else {
						validSensors.put(Constants.DB_P1, false);
					}
				}
				else if (sensor.toUpperCase().equals(Constants.DB_P2)) {
					if (iters == 0 && previous_failures < 2)
						validSensors.put(Constants.DB_P2, true);
					else {
						validSensors.put(Constants.DB_P2, false);
					}
				}
				else if (sensor.toUpperCase().equals(Constants.DB_P3)) {
					if (iters == 0 && previous_failures < 2)
						validSensors.put(Constants.DB_P3, true);
					else {
						validSensors.put(Constants.DB_P3, false);
					}
				}
				else if (sensor.toUpperCase().equals(Constants.DB_T1)) {
					if (iters == 0 && previous_failures < 2)
						validSensors.put(Constants.DB_T1, true);
					else {
						validSensors.put(Constants.DB_T1, false);
					}
				}
				else if (sensor.toUpperCase().equals(Constants.DB_T2)) {
					if (iters == 0 && previous_failures < 2)
						validSensors.put(Constants.DB_T2, true);
					else {
						validSensors.put(Constants.DB_T2, false);
					}
				}
				else {
					System.out
							.println("ERROR - INVALID SENSOR IN TABLE BLOCKS ("
									+ sensor + ") - EXITING");
					DatabaseHandler.closeConnection();
					System.exit(-1);
				}
			}
		} catch (SQLException e) {
			// System.out.println("ERROR - EXITING");
			// e.printStackTrace();
			System.out.println("FAIL_SAFE(19)");
			DatabaseHandler.closeConnection();
			System.exit(-1);
		}

		// now fill hashmap with sensors not found in the query
		if (validSensors.get(Constants.DB_P1) == null)
			validSensors.put(Constants.DB_P1, true);
		if (validSensors.get(Constants.DB_P2) == null)
			validSensors.put(Constants.DB_P2, true);
		if (validSensors.get(Constants.DB_P3) == null)
			validSensors.put(Constants.DB_P3, true);
		if (validSensors.get(Constants.DB_T1) == null)
			validSensors.put(Constants.DB_T1, true);
		if (validSensors.get(Constants.DB_T2) == null)
			validSensors.put(Constants.DB_T2, true);
	}

	public static void main(String[] args) {
		// checks arguments
		checkArgs(args);

		// sees blocks table for blocked sensors
		initValidSensors();

		// extracts, verifies and processes line
		if (!filePath.equals("")) {
			String line = null;
			try {
				line = readSpecificLine(line_number);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			double[] results = parseLine(line);
			System.out.println(args[2] + "#: " + results[0] + ", " + results[1]);
			// write to database = line_num, var_id, pd1, ps1, pd2, ps2,
			// pd3, ps3, tp1, tp2

			// System.out.println("Row successfully inserted.");

			// for testing purposes
			/*
			 * ResultSet res = DatabaseHandler
			 * .executeQuery("select * from inputs where var_id = " + var_id);
			 * while (res.next()) { System.out.println(res.getString("line_num")
			 * + "|" + res.getString("var_id") + "|" + res.getString("pd1") +
			 * "|" + res.getString("ps1") + "|" + res.getString("pd2") + "|" +
			 * res.getString("ps2") + "|" + res.getString("pd3") + "|" +
			 * res.getString("ps3") + "|" + res.getString("tp1") + "|" +
			 * res.getString("tp2") + "|"); } res = DatabaseHandler
			 * .executeQuery("select * from blocks where var_id = " + var_id);/*
			 * var_id + ",'" + sensor + "'," + iters + "," + previous_failures +
			 * ")"
			 */
			/*
			 * while (res.next()) { System.out.println(res.getString("var_id") +
			 * "|" + res.getString("sensor") + "|" +
			 * res.getString("iters_to_ignore") + "|" +
			 * res.getString("previous_failure") + "|"); }
			 */
		}
	}
}
