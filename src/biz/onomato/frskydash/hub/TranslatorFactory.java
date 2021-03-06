/*
 * Copyright 2011-2013, Espen Solbu, Hans Cappelle
 * 
 * This file is part of FrSky Dashboard.
 *
 *  FrSky Dashboard is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FrSky Dashboard is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FrSky Dashboard.  If not, see <http://www.gnu.org/licenses/>.
 */

package biz.onomato.frskydash.hub;

import java.util.HashMap;

import biz.onomato.frskydash.hub.sensors.AccelerometerTranslator;
import biz.onomato.frskydash.hub.sensors.AltitudeTranslator;
import biz.onomato.frskydash.hub.sensors.FASTranslator;
import biz.onomato.frskydash.hub.sensors.FuelTranslator;
import biz.onomato.frskydash.hub.sensors.GPSTranslator;
import biz.onomato.frskydash.hub.sensors.OpenXVarioTranslator;
import biz.onomato.frskydash.hub.sensors.RPMTranslator;
import biz.onomato.frskydash.hub.sensors.TempTranslator;
import biz.onomato.frskydash.hub.sensors.UserDataTranslator;
import biz.onomato.frskydash.hub.sensors.VoltTranslator;

/**
 * Use this factory to get the right translator based on user data frame
 * 
 * @author hcpl
 * 
 */
public class TranslatorFactory {

	/**
	 * singleton instance
	 */
	private static TranslatorFactory instance = null;

	/**
	 * collection of available data ids mapped to their sensor type
	 */
	private final HashMap<Integer, SensorTypes> dataIDs = new HashMap<Integer, SensorTypes>();

	/**
	 * collection of sensor types mapped to data translators
	 */
	private HashMap<SensorTypes, UserDataTranslator> dataTranslators = new HashMap<SensorTypes, UserDataTranslator>();

	/**
	 * singleton, use {@link #getInstance()} instead
	 */
	private TranslatorFactory() {
		// create the fixed mapping, in later releases we could also load
		// custom mappings based on user configuration
		initTranslators();
		// we also need a mapping for the data IDs
		initDataIDs();
	}

	/**
	 * retrieve singleton instance
	 * 
	 * @return
	 */
	public static TranslatorFactory getInstance() {
		if (instance == null)
			instance = new TranslatorFactory();
		return instance;
	}

	/**
	 * helper to initialise mapping of translators
	 */
	private void initTranslators() {
		GPSTranslator gpsTranslator = new GPSTranslator();
		dataTranslators.put(SensorTypes.gps_altitude_before, gpsTranslator);
		dataTranslators.put(SensorTypes.gps_altitude_after, gpsTranslator);
		dataTranslators.put(SensorTypes.temp1, new TempTranslator());
		dataTranslators.put(SensorTypes.rpm, new RPMTranslator());
		dataTranslators.put(SensorTypes.fuel, new FuelTranslator());
		dataTranslators.put(SensorTypes.temp2, new TempTranslator());
		dataTranslators.put(SensorTypes.volt, new VoltTranslator());
		AltitudeTranslator altTranslator = new AltitudeTranslator();
		dataTranslators.put(SensorTypes.altitude_before, altTranslator);
		dataTranslators.put(SensorTypes.altitude_after, altTranslator);
		dataTranslators.put(SensorTypes.vertical_speed, altTranslator);
		dataTranslators.put(SensorTypes.gps_speed_before, gpsTranslator);
		dataTranslators.put(SensorTypes.gps_speed_after, gpsTranslator);
		dataTranslators.put(SensorTypes.gps_longitude_before, gpsTranslator);
		dataTranslators.put(SensorTypes.gps_longitude_after, gpsTranslator);
		dataTranslators.put(SensorTypes.gps_longitude_ew, gpsTranslator);
		dataTranslators.put(SensorTypes.gps_latitude_before, gpsTranslator);
		dataTranslators.put(SensorTypes.gps_latitude_after, gpsTranslator);
		dataTranslators.put(SensorTypes.gps_latitude_ns, gpsTranslator);
		dataTranslators.put(SensorTypes.gps_course_before, gpsTranslator);
		dataTranslators.put(SensorTypes.gps_course_after, gpsTranslator);
		dataTranslators.put(SensorTypes.gps_day_month, gpsTranslator);
		dataTranslators.put(SensorTypes.gps_year, gpsTranslator);
		dataTranslators.put(SensorTypes.gps_hour_minute, gpsTranslator);
		dataTranslators.put(SensorTypes.gps_second, gpsTranslator);
		AccelerometerTranslator accTranslator = new AccelerometerTranslator();
		dataTranslators.put(SensorTypes.acc_x, accTranslator);
		dataTranslators.put(SensorTypes.acc_y, accTranslator);
		dataTranslators.put(SensorTypes.acc_z, accTranslator);
		
		//FAS_100
		FASTranslator fasTranslator = new FASTranslator();
		dataTranslators.put(SensorTypes.fas_voltage_before, fasTranslator);
		dataTranslators.put(SensorTypes.fas_voltage_after, fasTranslator);
		dataTranslators.put(SensorTypes.fas_current, fasTranslator);
		
		//OpenXVario
		OpenXVarioTranslator openXVarioTranslator = new OpenXVarioTranslator();
		dataTranslators.put(SensorTypes.openxvario_vfas_voltage, openXVarioTranslator);
		dataTranslators.put(SensorTypes.openxvario_gps_distance, openXVarioTranslator);
		
		
		
	}

	/**
	 * helper to initialise mapping of data IDs
	 */
	private void initDataIDs() {
		// put data
		dataIDs.put(0x00, SensorTypes.undefined);
		dataIDs.put(0x01, SensorTypes.gps_altitude_before);
		dataIDs.put(0x01 + 8, SensorTypes.gps_altitude_after);
		dataIDs.put(0x02, SensorTypes.temp1);
		dataIDs.put(0x03, SensorTypes.rpm);
		dataIDs.put(0x04, SensorTypes.fuel);
		dataIDs.put(0x05, SensorTypes.temp2);
		dataIDs.put(0x06, SensorTypes.volt);
		dataIDs.put(0x10, SensorTypes.altitude_before);
		dataIDs.put(0x21, SensorTypes.altitude_after);
		dataIDs.put(0x11, SensorTypes.gps_speed_before);
		dataIDs.put(0x11 + 8, SensorTypes.gps_speed_after);
		dataIDs.put(0x12, SensorTypes.gps_longitude_before);
		dataIDs.put(0x12 + 8, SensorTypes.gps_longitude_after);
		dataIDs.put(0x1A + 8, SensorTypes.gps_longitude_ew);
		dataIDs.put(0x13, SensorTypes.gps_latitude_before);
		dataIDs.put(0x13 + 8, SensorTypes.gps_latitude_after);
		dataIDs.put(0x1B + 8, SensorTypes.gps_latitude_ns);
		dataIDs.put(0x14, SensorTypes.gps_course_before);
		dataIDs.put(0x14 + 8, SensorTypes.gps_course_after);
		dataIDs.put(0x15, SensorTypes.gps_day_month);
		dataIDs.put(0x16, SensorTypes.gps_year);
		dataIDs.put(0x17, SensorTypes.gps_hour_minute);
		dataIDs.put(0x18, SensorTypes.gps_second);
		dataIDs.put(0x24, SensorTypes.acc_x);
		dataIDs.put(0x25, SensorTypes.acc_y);
		dataIDs.put(0x26, SensorTypes.acc_z);
		
		//FAS-100
		dataIDs.put(0x28, SensorTypes.fas_current);
		dataIDs.put(0x3a, SensorTypes.fas_voltage_before);
		dataIDs.put(0x3b, SensorTypes.fas_voltage_after);
		
		//OpenXVario
		dataIDs.put(0x30, SensorTypes.vertical_speed);
		dataIDs.put(0x39, SensorTypes.openxvario_vfas_voltage);
		dataIDs.put(0x3c, SensorTypes.openxvario_gps_distance);
		
		
	}

	/**
	 * retrieve the translator based on the data available in the user data
	 * frame
	 * 
	 * TODO see if we can use the Frame object instead
	 * 
	 * @param sensorType
	 * @return
	 */
	public UserDataTranslator getTranslatorForFrame(SensorTypes sensorType) {
		return dataTranslators.get(sensorType);
	}

	/**
	 * resolve the sensor type based on the given frame information
	 * 
	 * @param frame
	 * @return
	 */
	public SensorTypes getSensorType(int[] frame) {
		if(dataIDs.get(frame[1])!=null)
			return dataIDs.get(frame[1]);
		else
			return dataIDs.get(0x00); // return undefined if key is unknown
	}

}
