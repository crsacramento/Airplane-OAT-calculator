package pck;

import pck.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class Parser {
	
	
	private static HashMap<String,Double> parseFileString(String line) {
		// 23# 1110.00:1000.00,120.00:1110.00,1190.00:900.00,-52.98,-53.21
		// 24# --:--,120.00:1110.00,1190.00:900.00,--,-53.21
		HashMap<String,Double> res = new HashMap<String,Double>();
		
		String[] components = line.substring(line.indexOf("#") + 1).split(",");
		if(components.length > 0 && components.length == 5){
			// first pitot read
			String[] pitots = components[0].split(":");
			if(pitots[0].equals(Constants.ERROR) || pitots[1].equals(Constants.ERROR)){
				// error value
				res.put(Constants.FIRST_PITOT_STATIC, Constants.ERROR_VALUE);
				res.put(Constants.FIRST_PITOT_DYNAMIC, Constants.ERROR_VALUE);
			}else{
				res.put(Constants.FIRST_PITOT_STATIC, Double.valueOf(pitots[0]));
				res.put(Constants.FIRST_PITOT_DYNAMIC, Double.valueOf(pitots[1]));
			}
			
			// second pitot read
			pitots = components[1].split(":");
			if(pitots[0].equals(Constants.ERROR) || pitots[1].equals(Constants.ERROR)){
				// error value
				res.put(Constants.SECOND_PITOT_STATIC, Constants.ERROR_VALUE);
				res.put(Constants.SECOND_PITOT_DYNAMIC, Constants.ERROR_VALUE);
			}else{
				res.put(Constants.SECOND_PITOT_STATIC, Double.valueOf(pitots[0]));
				res.put(Constants.SECOND_PITOT_DYNAMIC, Double.valueOf(pitots[1]));
			}
			
			// second pitot read
			pitots = components[2].split(":");
			if(pitots[0].equals(Constants.ERROR) || pitots[1].equals(Constants.ERROR)){
				// error value
				res.put(Constants.THIRD_PITOT_STATIC, Constants.ERROR_VALUE);
				res.put(Constants.THIRD_PITOT_DYNAMIC, Constants.ERROR_VALUE);
			}else{
				res.put(Constants.THIRD_PITOT_STATIC, Double.valueOf(pitots[0]));
				res.put(Constants.THIRD_PITOT_DYNAMIC, Double.valueOf(pitots[1]));
			}

			// fill first temperature
			if(components[3].equals(Constants.ERROR))
				res.put(Constants.FIRST_TEMP, Constants.ERROR_VALUE);
			else
				res.put(Constants.FIRST_TEMP, Double.valueOf(components[3]));
			
			// fill second temperature
			if(components[4].equals(Constants.ERROR))
				res.put(Constants.SECOND_TEMP, Constants.ERROR_VALUE);
			else
				res.put(Constants.SECOND_TEMP, Double.valueOf(components[4]));
			
			return res;
		
		}else{
			return null;
		}
	}
	
	/**
	 * Calculates the median of the values in param. Param must be sorted.
	 * @param m
	 * @return
	 */
	public static double median(ArrayList<Double> m) {
	    int middle = m.size()/2;
	    if (m.size() % 2 == 1) {
	        return m.get(middle);
	    } else {
	        return (m.get(middle - 1) + m.get(middle)) / 2.0;
	    }
	}
	
	public static void main(String[] args){
		// 23# 1110.00:1000.00,120.00:1110.00,1190.00:900.00,-52.98,-53.21
		// 24# --:--,120.00:1110.00,1190.00:900.00,--,-53.21
		HashMap<String, Double> results = parseFileString("23# 1110.00:1000.00,120.00:1110.00,1190.00:900.00,-52.98,-53.21");
		//HashMap<String, Double> results = parseFileString("24# --:--,120.00:1110.00,1190.00:900.00,--,-53.21");
		
		if(results == null){
			System.out.println("ERROR, INVALID FILE LINE");
			System.exit(-1);
		}
		ArrayList<Double> 	staticPressure  = new ArrayList<Double>(),
							dynamicPressure = new ArrayList<Double>(),
							temperature 	= new ArrayList<Double>();
		
		// initialize static pressure list
		if(!results.get(Constants.FIRST_PITOT_STATIC).equals(Constants.ERROR_VALUE)){
			staticPressure.add(results.get(Constants.FIRST_PITOT_STATIC));
		}
		if(!results.get(Constants.SECOND_PITOT_STATIC).equals(Constants.ERROR_VALUE)){
			staticPressure.add(results.get(Constants.SECOND_PITOT_STATIC));
		}
		if(!results.get(Constants.THIRD_PITOT_STATIC).equals(Constants.ERROR_VALUE)){
			staticPressure.add(results.get(Constants.THIRD_PITOT_STATIC));
		}
		
		// initialize dynamic pressure list
		if(!results.get(Constants.FIRST_PITOT_DYNAMIC).equals(Constants.ERROR_VALUE)){
			dynamicPressure.add(results.get(Constants.FIRST_PITOT_DYNAMIC));
		}
		if(!results.get(Constants.SECOND_PITOT_DYNAMIC).equals(Constants.ERROR_VALUE)){
			dynamicPressure.add(results.get(Constants.SECOND_PITOT_DYNAMIC));
		}
		if(!results.get(Constants.THIRD_PITOT_DYNAMIC).equals(Constants.ERROR_VALUE)){
			dynamicPressure.add(results.get(Constants.THIRD_PITOT_DYNAMIC));
		}
		
		// initialize temperature list
		if(!results.get(Constants.FIRST_TEMP).equals(Constants.ERROR_VALUE)){
			temperature.add(results.get(Constants.FIRST_TEMP));
		}
		if(!results.get(Constants.SECOND_TEMP).equals(Constants.ERROR_VALUE)){
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
		
		//get medians
		double 	staticPressureValue  = median(staticPressure),
				dynamicPressureValue = median(dynamicPressure),
				temperatureValue 	 = median(temperature);
		
		System.out.println(staticPressureValue + "_" + dynamicPressureValue + "_" + temperatureValue);
		
		double[] arr = Calculator.calculateTAS_OAT(staticPressureValue, dynamicPressureValue, temperatureValue);
		System.out.println("RESULTS: " + (arr[0] - Constants.KELVIN) + " ºC|" + arr[1] + " knots");
	}
}
