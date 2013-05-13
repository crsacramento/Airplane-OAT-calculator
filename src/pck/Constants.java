package pck;

public class Constants {
	/** this constant is measured in knots */
	public static final double SOUND_SPEED_AT_AIR_LEVEL = 661.47;
	
	/** this constant is measured in kelvin */
	public static final double TEMPERATURE_AT_AIR_LEVEL = 288.15;
	
	public static final String FIRST_PITOT_STATIC = "PS1";
	public static final String FIRST_PITOT_DYNAMIC = "PD1";
	
	public static final String SECOND_PITOT_STATIC = "PS2";
	public static final String SECOND_PITOT_DYNAMIC = "PD2";
	
	public static final String THIRD_PITOT_STATIC = "PS3";
	public static final String THIRD_PITOT_DYNAMIC = "PD3";
	
	public static final String FIRST_TEMP = "TP1";
	public static final String SECOND_TEMP = "TP2";
	
	public static final String ERROR = "--";
	public static final Double ERROR_VALUE = -1.0;

	public static final Double KELVIN = 273.15;
	
	// related to database
	
	public static final int ITERS_IGNORE_TEMP = 10;
	public static final int ITERS_IGNORE_PRESSURE = 4;
	public static final String DB_P1 = "P1";
	public static final String DB_P2 = "P2";
	public static final String DB_P3 = "P3";
	public static final String DB_T1 = "T1";
	public static final String DB_T2 = "T2";
	
	// related to error verification
	public static final int DELTA_TEMP = 10;
	public static final double DELTA_PRESSURE = 0.1;
	public static final int MIN_LIMIT_TEMP = -100;
	public static final int MAX_LIMIT_TEMP = 100;
	public static final int MIN_LIMIT_PRESSURE = 100;
	public static final int MAX_LIMIT_PRESSURE = 1500;
}
