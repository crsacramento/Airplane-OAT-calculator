package pck;

import pck.Constants;

public class Calculator {

	/**
	 * function that calculates the Mach number based on params given
	 * 
	 * @param staticPressure
	 *            = static pressure (Qc) measured in mB
	 * @param dynamicPressure
	 *            = dynamic pressure (p) measured in mB
	 * @return Mach number (M)
	 */
	private static double machNumber(double staticPressure,
			double dynamicPressure) {
		return ((Math.pow(((dynamicPressure / staticPressure) + 1), (2.0 / 7)) - 1) * 5);
	}

	/**
	 * Function that calculates true air speed based on params given
	 * 
	 * @param machNumber
	 *            mach number calculated by machNumber function
	 * @param staticAirTemperature
	 *            given by Pitot tubes
	 * @return true air speed
	 */
	private static double TAS(double machNumber, double staticAirTemperature) {
		return (Constants.SOUND_SPEED_AT_AIR_LEVEL * machNumber * Math
				.sqrt(staticAirTemperature / Constants.TEMPERATURE_AT_AIR_LEVEL));
	}

	/**
	 * Function that calculates outside air temperature based on params given
	 * 
	 * @param staticAirTemperature
	 *            given by Pitot tube
	 * @param machNumber
	 *            calculated by machNumber function
	 * @return outside air temperature (in kelvin)
	 */
	private static double OAT(double staticAirTemperature, double machNumber) {
		return ((staticAirTemperature + Constants.KELVIN)/ (1 + 0.2 * machNumber));
	}

	/**
	 * Function to be called by main function to calculate outside air
	 * temperature
	 * 
	 * @param staticPressure
	 *            static pressure median passed by Pitot tubes
	 * @param dynamicPressure
	 *            dynamic pressure median passed by Pitot tubes
	 * @param temperature
	 *            temperature median passed by Pitot tubes
	 * @return
	 * 		  [OAT,TAS]
	 */
	public static double[] calculateTAS_OAT(double staticPressure,
			double dynamicPressure, double temperature) {
		double machNumber = machNumber(staticPressure, dynamicPressure);
		System.out.println("M: " + machNumber);
		double outsideAirTemp = OAT(temperature, machNumber);
		System.out.println("OAT: " + outsideAirTemp);
		double trueSpeed = TAS(machNumber, outsideAirTemp);
		System.out.println("TAS: " + trueSpeed);
		return new double[] { outsideAirTemp, trueSpeed };
	}
}
