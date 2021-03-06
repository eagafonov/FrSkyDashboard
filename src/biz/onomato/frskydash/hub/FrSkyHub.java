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

import java.util.Arrays;

import biz.onomato.frskydash.domain.Channel;
import biz.onomato.frskydash.domain.Frame;
import biz.onomato.frskydash.util.Logger;

/**
 * responsible for parsing FrSky Hub Sensor Data. By using channels and their
 * setRaw method we ensure the gui thread gets all the updated values
 * 
 */
public class FrSkyHub extends Hub{

	/**
	 * Unique ID for the HUB
	 */
	public static final int HUB_ID =-1000;

	/**
	 * Unique ID's for the Hubs channels
	 * FIXME: Id for hubs and channels should be changed. Preferrably so that hub can
	 * be oblivious to channel id's
	 */
	
	public static final int CHANNEL_ID_ALTITUDE = 0 + HUB_ID;
	public static final int CHANNEL_ID_RPM = 1 + HUB_ID;
	public static final int CHANNEL_ID_TEMP1 = 2 + HUB_ID;
	public static final int CHANNEL_ID_TEMP2 = 3 + HUB_ID;
	// acc sensor
	public static final int CHANNEL_ID_ACCELEROMETER_X = 4 + HUB_ID;
	public static final int CHANNEL_ID_ACCELEROMETER_Y = 5 + HUB_ID;
	public static final int CHANNEL_ID_ACCELEROMETER_Z = 6 + HUB_ID;
	public static final int CHANNEL_ID_FUEL = 7 + HUB_ID;
	// lipo sensor, to be registered per cell (required for cannonical values,
	// unless we implement compositive values support)
	public static final int CHANNEL_ID_LIPO_CELL_1 = 10 + HUB_ID;
	public static final int CHANNEL_ID_LIPO_CELL_2 = 11 + HUB_ID;
	public static final int CHANNEL_ID_LIPO_CELL_3 = 12 + HUB_ID;
	public static final int CHANNEL_ID_LIPO_CELL_4 = 13 + HUB_ID;
	public static final int CHANNEL_ID_LIPO_CELL_5 = 14 + HUB_ID;
	public static final int CHANNEL_ID_LIPO_CELL_6 = 15 + HUB_ID;
	// gps sensor values
	public static final int CHANNEL_ID_GPS_COURSE = 21 + HUB_ID;
	public static final int CHANNEL_ID_GPS_ALTITUDE = 22 + HUB_ID;
	public static final int CHANNEL_ID_GPS_SPEED = 23 + HUB_ID;
	public static final int CHANNEL_ID_GPS_LATITUDE = 24 + HUB_ID;
	public static final int CHANNEL_ID_GPS_LONGITUDE = 25 + HUB_ID;
	
	
	// current sensor
	public static final int CHANNEL_ID_FAS_CURRENT = 26 + HUB_ID;
	public static final int CHANNEL_ID_FAS_VOLTAGE = 27 + HUB_ID;
	

	public static final int CHANNEL_ID_VERT_SPEED = 28 + HUB_ID;
	
	// OpenXVario
	public static final int CHANNEL_ID_OPENXVARIO_VFAS_VOLTAGE = 29 + HUB_ID;
	public static final int CHANNEL_ID_OPENXVARIO_GPS_DISTANCE = 30 + HUB_ID;
	
	public static final int CHANNEL_ID_GPS_TIMESTAMP = 31 + HUB_ID;
	
	// Calculated channels
	public static final int CHANNEL_ID_CALC_TIMESTAMP = 32 + HUB_ID;
	public static final int CHANNEL_ID_CALC_GPS_LOCK = 33 + HUB_ID;
	public static final int CHANNEL_ID_CALC_VERT_SPEED = 34 + HUB_ID;

	
	/**
	 * tag for logging
	 */
	public final static String TAG="FrSkyHub";
	
	/**
	 * size for user data frames
	 */
	public static final int SIZE_HUB_FRAME = 5;
	
	/**
	 * delimiter byte for user data frames
	 */
	public static final int START_STOP_HUB_FRAME = 0x5E;
	
	/**
	 * stuffing indicator for the user data frames
	 */
	public static final int STUFFING_HUB_FRAME = 0x5D;
	
	/**
	 * first byte after stuffing indicator should be XORed with this value
	 */
	public static final int XOR_HUB_FRAME = 0x60;
	
	/**
	 * the current user frame we are working on. This is used to pass data
	 * between incompletes frames.
	 */
	private static int[] hubFrame = new int[SIZE_HUB_FRAME];

	/**
	 * index of the current user frame. If set to -1 no user frame is under
	 * construction.
	 */
	private static int currentHubFrameIndex = -1;

	/**
	 * if on previous byte the XOR byte was found or not
	 */
	private static boolean hubXOR = false;

	

	
	/**
	 * def ctor. This will not initialize the channels, you need to call the
	 * initializeChannels() method!
	 */
	public FrSkyHub() {
		// eso: Prototype Channel code
		// don't call this in ctor so we can still import/export properly
		//initializeChannels();
		this.name = TAG;
	}

	/**
	 * once we extracted the user data frames these can be handled by checking
	 * their data ID
	 * 
	 * @param frame
	 */
	private void handleHubDataFrame(int[] frame) {
		// some validation first
		// all frames are delimited by the 0x5e start and end byte and should be
		// 5 bytes long. We can validate this before doing any parsing
		if (frame.length != SIZE_HUB_FRAME
				|| frame[0] != START_STOP_HUB_FRAME
				|| frame[SIZE_HUB_FRAME - 1] != START_STOP_HUB_FRAME) {
			// log exception here
			Logger.e(TAG,
					"Wrong hub frame format: " + Arrays.toString(frame));
			return;
		}
		// retrieve sensor type
		SensorTypes sensorType = TranslatorFactory.getInstance().getSensorType(
				frame);
		
		
//		
			
			// due to need to maintain state (store "before" until "after" is ready, the following approach is deprecated
			// translate value
			double value;
			try
			{
			value = TranslatorFactory.getInstance()
					.getTranslatorForFrame(sensorType)
					.translateValue(sensorType, frame);
			}
			catch (Exception e)
			{
				Logger.e(TAG, "Unable to get value for frame: "+Arrays.toString(frame));
				Logger.e(TAG, "SensorType is: "+sensorType.toString());
				value = UNDEFINED_VALUE; // need to be changed, just added for testng purposes
				
			}
			// FIXME now there is one exception for volt sensor so far
			if (sensorType == SensorTypes.volt) {
				// get the right cell for specific update
				// first 4 bit is battery cell number
				int cell = FrSkyHub.getBatteryCell(frame);
				// if (cell < 1 || cell > 6) { CHECK RANGE OF CELL
				if (cell < 0 || cell > 5) {
					Logger.d(this.getClass().toString(),
							"failed to handle cell nr out of range: " + cell);
					return;
				}
				// now we can update a specific cell
				String type = "CELL_" + cell;
				// and update
				updateChannel(SensorTypes.valueOf(type), value);
			}
			// and update channel
			else{
				if(value!=UNDEFINED_VALUE)	// only update if we have a value
				{
					updateChannel(sensorType, value);
				}
			}
		
	}

	/**
	 * translate frame form a Little Endian (LE) Signed 16 Bit value
	 * 
	 * @param frame
	 * @return
	 */
	public static int getSignedLE16BitValue(int[] frame) {
		// return 0xFFFF - ( (frame[2] & 0xFF) + ( (frame[3] & 0xFF) * 0x100) );
		// return 0xFFFF - getUnsigned16BitValue(frame);
		
		// Debug
		return (short) getUnsignedLE16BitValue(frame);
	}

	/**
	 * translate frame from a little Endian (LE) Unsigned 16 bit value
	 * 
	 * @param frame
	 * @return
	 */
	public static int getUnsignedLE16BitValue(int[] frame) {
		// return getSigned16BitValue(frame) & 0xffff;
		return ((frame[2] & 0xFF) + ((frame[3] & 0xFF) * 0x100));
	}

	/**
	 * translate frame from a Big Endian (BE) Unsigned 16 bit value
	 * 
	 * @param frame
	 * @return
	 */
	public static int getUnsignedBE16BitValue(int[] frame) {
		return frame[3] + frame[2] * 100;
	}

	// private int getBCDValue(int[] frame){
	// return 0;
	// }

	/**
	 * helper to retrieve the number of the cell from frame
	 * 
	 * @param frame
	 * @return
	 */
	public static int getBatteryCell(int[] frame) {
		// only need the first 4 bits to get cell number
		int cellNumber = frame[2] >> 4;
		return cellNumber;
	}

	/**
	 * helper to retrieve voltage of a cell from frame
	 * 
	 * @param frame
	 * @return
	 */
	public static double getCellVoltage(int[] frame) {
		// the last 12 bits are a value between 0-2100 representing voltage
		// 0-4.2v (so /500)
		double voltage = (double) ((frame[2] << 8 & 0xFFF) + frame[3]) / 500;
		return voltage;
	}

	/**
	 * update a single channel with a single value
	 * 
	 * @param sensorType
	 * @param value
	 */
	private void updateChannel(SensorTypes sensorType, double value) {

		/*
		 * eso, prototype Channel support Update a proper channel rather than
		 * broadcast. 
		 */
		switch (sensorType) {
		case rpm:
			getChannel(CHANNEL_ID_RPM).setRaw(value);
			break;
		case temp1:
			getChannel(CHANNEL_ID_TEMP1).setRaw(value);
			break;
		case temp2:
			getChannel(CHANNEL_ID_TEMP2).setRaw(value);
			break;
		case altitude_after:
			getChannel(CHANNEL_ID_ALTITUDE).setRaw(value);
			break;
		case vertical_speed:
			getChannel(CHANNEL_ID_VERT_SPEED).setRaw(value);
			break;
		case acc_x:
			getChannel(CHANNEL_ID_ACCELEROMETER_X).setRaw(value);
			break;
		case acc_y:
			getChannel(CHANNEL_ID_ACCELEROMETER_Y).setRaw(value);
			break;
		case acc_z:
			getChannel(CHANNEL_ID_ACCELEROMETER_Z).setRaw(value);
			break;
		case fuel:
			getChannel(CHANNEL_ID_FUEL).setRaw(value);
			break;
		case CELL_0:
			getChannel(CHANNEL_ID_LIPO_CELL_1).setRaw(value);
			break;
		case CELL_1:
			getChannel(CHANNEL_ID_LIPO_CELL_2).setRaw(value);
			break;
		case CELL_2:
			getChannel(CHANNEL_ID_LIPO_CELL_3).setRaw(value);
			break;
		case CELL_3:
			getChannel(CHANNEL_ID_LIPO_CELL_4).setRaw(value);
			break;
		case CELL_4:
			getChannel(CHANNEL_ID_LIPO_CELL_5).setRaw(value);
			break;
		case CELL_5:
			getChannel(CHANNEL_ID_LIPO_CELL_6).setRaw(value);
			break;
		case gps_altitude_after:
			getChannel(CHANNEL_ID_GPS_ALTITUDE).setRaw(value);
			break;
		case gps_speed_after:
			getChannel(CHANNEL_ID_GPS_SPEED).setRaw(value);
			break;
		case gps_course_after:
			getChannel(CHANNEL_ID_GPS_COURSE).setRaw(value);
			break;
		case gps_latitude_ns:
			getChannel(CHANNEL_ID_GPS_LATITUDE).setRaw(value);
			break;
		case gps_longitude_ew:
			getChannel(CHANNEL_ID_GPS_LONGITUDE).setRaw(value);
			break;
		case gps_second:
			getChannel(CHANNEL_ID_GPS_TIMESTAMP).setRaw(value);
			break;
			
		case fas_current:
			getChannel(CHANNEL_ID_FAS_CURRENT).setRaw(value);
			break;
		case fas_voltage_after:
			getChannel(CHANNEL_ID_FAS_VOLTAGE).setRaw(value);
			break;
		case openxvario_vfas_voltage:
			getChannel(CHANNEL_ID_OPENXVARIO_VFAS_VOLTAGE).setRaw(value);
			break;
		case openxvario_gps_distance:
			getChannel(CHANNEL_ID_OPENXVARIO_GPS_DISTANCE).setRaw(value);
			break;
		
		default:
			// don't update these as they are not fully composed at this time         
			break;
		}
	}

		


	@Override
	public Hub initializeChannels() {
		// Sets up the hardcoded channels (Altitude,RPM)
		// TODO create enums with description etc or proper factory instead
		configureChannelForSensor(CHANNEL_ID_ALTITUDE, 					"Hub: Altitude");
		configureChannelForSensor(CHANNEL_ID_VERT_SPEED, 				"Hub: Vertical Speed");
		configureChannelForSensor(CHANNEL_ID_RPM, 						"Hub: RPM (pulses)");
		configureChannelForSensor(CHANNEL_ID_TEMP1, 					"Hub: Temp 1");
		configureChannelForSensor(CHANNEL_ID_TEMP2, 					"Hub: Temp 2");
		configureChannelForSensor(CHANNEL_ID_ACCELEROMETER_X, 			"Hub: Acc. X");
		configureChannelForSensor(CHANNEL_ID_ACCELEROMETER_Y, 			"Hub: Acc. Y");
		configureChannelForSensor(CHANNEL_ID_ACCELEROMETER_Z, 			"Hub: Acc. Z");
		configureChannelForSensor(CHANNEL_ID_FUEL, 						"Hub: Fuel");
		configureChannelForSensor(CHANNEL_ID_GPS_ALTITUDE, 				"Hub: GPS Altitude");
		configureChannelForSensor(CHANNEL_ID_GPS_COURSE, 				"Hub: GPS Course");
		configureChannelForSensor(CHANNEL_ID_GPS_LATITUDE, 				"Hub: GPS Latitude");
		configureChannelForSensor(CHANNEL_ID_GPS_LONGITUDE,				"Hub: GPS Longitude");
		configureChannelForSensor(CHANNEL_ID_GPS_SPEED, 				"Hub: GPS Speed");
		configureChannelForSensor(CHANNEL_ID_GPS_TIMESTAMP, 			"Hub: GPS Timestamp");
		configureChannelForSensor(CHANNEL_ID_LIPO_CELL_1, 				"Hub: Lipo Cell 1");
		configureChannelForSensor(CHANNEL_ID_LIPO_CELL_2, 				"Hub: Lipo Cell 2");
		configureChannelForSensor(CHANNEL_ID_LIPO_CELL_3, 				"Hub: Lipo Cell 3");
		configureChannelForSensor(CHANNEL_ID_LIPO_CELL_4, 				"Hub: Lipo Cell 4");
		configureChannelForSensor(CHANNEL_ID_LIPO_CELL_5, 				"Hub: Lipo Cell 5");
		configureChannelForSensor(CHANNEL_ID_LIPO_CELL_6, 				"Hub: Lipo Cell 6");
		configureChannelForSensor(CHANNEL_ID_FAS_CURRENT, 				"Hub: FAS-XX Current");
		configureChannelForSensor(CHANNEL_ID_FAS_VOLTAGE, 				"Hub: FAS-XX Voltage");
		
		//OpenXVario
		
		configureChannelForSensor(CHANNEL_ID_OPENXVARIO_VFAS_VOLTAGE, 	"oXv: VFAS Voltage");
		configureChannelForSensor(CHANNEL_ID_OPENXVARIO_GPS_DISTANCE, 	"oXv: GPS Distance");
		
		// custom setup
		getChannel(CHANNEL_ID_GPS_LATITUDE).setPrecision(10);
		getChannel(CHANNEL_ID_GPS_LONGITUDE).setPrecision(10);
		// incoming current must be divided by 10
		//getChannel(CHANNEL_ID_FAS_CURRENT).setFactor((float) 0.1);
		// incoming voltage must be multiplied by (21/11) to get voltage in Volts
		//getChannel(CHANNEL_ID_FAS_VOLTAGE).setFactor((float) (21.0/11.0));
		getChannel(CHANNEL_ID_OPENXVARIO_VFAS_VOLTAGE).setPrecision(1);
		getChannel(CHANNEL_ID_GPS_TIMESTAMP).setPrecision(0);
		
		// return yourself for chaining
		return this;
	}

	/**
	 * helper to create the preconfigured channels for sensors
	 */
	private void configureChannelForSensor(int channelID, String description) {
		Channel channel = new Channel(description, 0, 1, "", "");
		channel.setId(channelID);
		// all double values, need at least precision 2
		channel.setPrecision(2);
		channel.setSilent(true);
		//channel.registerListenerForServerCommands();
		//_sourceChannelMap.put(channelID, channel);
		addChannel(channel);
	}


	/* (non-Javadoc)
	 * @see biz.onomato.frskydash.hub.Hub#getId()
	 */
	@Override
	public int getId() {
		return HUB_ID;
	}

	private int[] prevInts = null;
	private Frame prevFrame = null;
	
	@Override
	public void addUserBytes(int[] ints,Frame frame) {
		// FIXME: I don't trust this implementation:
		// I get too many start/stop bytes at wrong position
		// 1. We need to consider the length of valid bytes in the frame, and discard bytes after. SIZE_HUB_FRAME should not be used
		// 2. Hub destuffing should only apply to the actual payload, and not the length or the second NA byte
		
		// shall take user bytes
		// decode (destuff) them, and add to internal queue
		// then identify hub frames within this queue
		// and pass them to handleHubDataFrame
		// FIXED can't this be implemented in another way so we don't have to
		// pass the server instance?
		// eso: server is currently used for broadcasts from the hub, this shall
		// be removed as the hub shall not broadcast
		// => server param to be removed
		//this.server = server;
		// don't handle all the bytes, skip header (0), prim(1), size(2),
		// unused(3) and end but(10)
		// the byte at index 2 indicated how many bytes are valid in this frame.
		// Make sure to only take in account these valid bytes starting to count
		// from byte at index 4
		// index => byte description
		// 0 => frame start byte
		// 1 => type of frame byte (analog, signal, user data, ...)
		// 2 => length of valid bytes
		// 3 => discard this byte
		// 4 => first user data byte
		// ...
		// 10 => stop byte frame
		for (int b : ints) {
			// b = ints[i];
			// handle byte stuffing first
			// FIXME: This is not correct, hub stuffing only happens on payload (after size data), might not be relevant
			if (b == STUFFING_HUB_FRAME) {
				hubXOR = true;
				// drop this byte
				continue;
			}
			if (hubXOR) {
				b ^= XOR_HUB_FRAME;
				// don't unset the xor flag yet since we'll have to check on
				// this for start/stop bit detection
				// xor = false;
				Logger.d(TAG,
						"XOR operation, unstuffed to " + Integer.toHexString(b));
			}
			// if we encounter a start byte we need to indicate we're in a
			// frame or if at the end handle the frame and continue
			if (b == START_STOP_HUB_FRAME && !hubXOR) {
				// if currentFrameIndex is not set we have to start a new
				// frame here
				if (currentHubFrameIndex < 0) {
					// init current frame index at beginning
					currentHubFrameIndex = 0;
					// and copy this first byte in the frame
					hubFrame[currentHubFrameIndex++] = b;
				}
				// otherwise we were already collecting a frame so this
				// indicates we are at the end now. At this point a frame is
				// available that we can send over.
				else if (currentHubFrameIndex == SIZE_HUB_FRAME - 1) {
					// just complete the frame we were collecting
					hubFrame[currentHubFrameIndex] = b;
					// this way the length is confirmed
					handleHubDataFrame(hubFrame);
					// once information is handled we can reset the frame
					hubFrame = new int[SIZE_HUB_FRAME];
					currentHubFrameIndex = 0;
					// unlike the telemetry frames where 126 is on each side of
					// the frame (126, x, y, 126, 126, x, y, 126) the 94 bit of
					// the hub frame is only a single bit in between (94, x, y,
					// 94, x, y, 94) so we need to reuse that last 94 bit
					// encountered as the beginning of our next frame
					hubFrame[currentHubFrameIndex++] = b;
				}
				// if for some reason we got 2 0x5e bytes after each other
				// or the size of the frame was different we can't do
				// anything with the previous collected information. We can
				// log a debug message and drop the frame to start over
				// again.
				else {
					//if(hubFrame[1]!=0x00) // No need to log it if "ID" is 0x00..
					//{
					// log debug info here
					if(currentHubFrameIndex==1
							|| hubFrame[1]==0x00) // not an error to reset frame if sequential 5e like this
					{
						
					}
					else // unclear reason for dropping the frame
					{
						Logger.w(TAG, "Start/stop byte at wrong position: 0x"
								+ Integer.toHexString(b) + " frame so far: "
								+ Arrays.toString(hubFrame));
						Logger.w(TAG, "The bad hubframe: "+toHuman(hubFrame));
						Logger.w(TAG, "Incoming bytes: "+toHuman(ints));
						Logger.w(TAG, "Previous bytes: "+toHuman(prevInts));
						Logger.w(TAG, "Incoming Frame: "+frame.toHuman());
						Logger.w(TAG, "Previous Frame: "+prevFrame.toHuman());
						Logger.w(TAG, "Position in Hubframe: "+currentHubFrameIndex);
						
						Logger.w(TAG,"------");
						//}
					}
					currentHubFrameIndex = 0;
					hubFrame = new int[SIZE_HUB_FRAME];
					hubFrame[currentHubFrameIndex++] = b;
				}
			}
			// otherwise we are handling a valid byte that has to be put in
			// the frame we are collecting. But only when we are currently
			// working on a frame!
			else if (currentHubFrameIndex >= 0
					&& currentHubFrameIndex < SIZE_HUB_FRAME - 1) {
				hubFrame[currentHubFrameIndex++] = b;
			}
			// finally it's possible that we receive bytes without being in
			// a frame, just discard them for now
			else {
				// log debug info here
				Logger.w(TAG, "Received data outside frame, dropped byte: 0x"
						+ Integer.toHexString(b));
			}
			// make sure to unset the xor flag at this point
			hubXOR = false;
		}
		prevInts = ints;
		prevFrame = frame;
	}

	/* (non-Javadoc)
	 * @see biz.onomato.frskydash.hub.Hub#addUserBytes(byte[])
	 */
	@Override
	public void addUserBytes(byte[] bytes) {
		// FIXME incomplete
		throw new RuntimeException("Not Implemented");
	}

}
