package biz.onomato.frskydash.hub;

import java.util.Arrays;
import java.util.Collections;
import java.util.TreeMap;

import biz.onomato.frskydash.FrSkyServer;
import biz.onomato.frskydash.domain.Channel;
import biz.onomato.frskydash.domain.Frame;
import biz.onomato.frskydash.util.Logger;

/**
 * responsible for parsing FrSky Hub Sensor Data
 * 
 */
public class FrSkyHub extends Hub{

	public final String TAG="FrSkyHub";
	
	// hcpl sensor hub data frame parameters
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
	 * singleton instance
	 */
	private static FrSkyHub instance = null;

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
	 * is bound to a single server instance
	 */
	private static FrSkyServer server;

	/**
	 * def ctor, singleton use {@link #getInstance()} instead
	 */
	private FrSkyHub() {
		// eso: Prototype Channel code
		initializeChannels();
	}

	public static FrSkyHub getInstance() {
		if (instance == null) {
			instance = new FrSkyHub();
		}
		return instance;
	}

	
	// FIXME: shall be deprecated when addUserBytes is properly implemented
	/**
	 * Extract user data bytes from telemetry data frame. This subframe is
	 * delimited by the 0x5E byte, included in the frame argument for this
	 * method. A single call can contain incomplete user data frames so need to
	 * keep track of previous frame also.
	 * 
	 * 
	 * @param frame
	 */
	public void extractUserDataBytes(FrSkyServer paramServer, Frame frame) {
		// init
		// FIXME can't this be implemented in another way so we don't have to
		// pass the server instance?
		// eso: server is currently used for broadcasts from the hub, this shall be removed
		// as the hub shall not broadcast
		server = paramServer;
		//int b;
		//int[] ints = frame.toInts();
		//int[] ints = frame.getUserBytes();
		// iterate elements in frame
		// for (int b : frame) {
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
		//int nrOfValidBytesInFrame = ints[2];
		// for (int i = 4; i < Frame.SIZE_TELEMETRY_FRAME - 1; i++) {
		//for (int i = 4; i < 4 + nrOfValidBytesInFrame; i++) {
		for(int b : frame.getUserBytes()){
			//b = ints[i];
			// handle byte stuffing first
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
				Logger.d(TAG, "XOR operation, unstuffed to "
						+ Integer.toHexString(b));
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
					// log debug info here
					Logger.d(
							TAG,
							"Start/stop byte at wrong position: 0x"
									+ Integer.toHexString(b)
									+ " frame so far: "
									+ Arrays.toString(hubFrame));
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
				Logger.d(TAG,
						"Received data outside frame, dropped byte: 0x"
								+ Integer.toHexString(b));
			}
			// make sure to unset the xor flag at this point
			hubXOR = false;
		}
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
			Logger.d(TAG,
					"Wrong hub frame format: " + Arrays.toString(frame));
			return;
		}
		// retrieve sensor type
		SensorTypes sensorType = TranslatorFactory.getInstance().getSensorType(
				frame);
		// translate value
		double value = TranslatorFactory.getInstance()
				.getTranslatorForFrame(sensorType)
				.translateValue(sensorType, frame);
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
		else
			updateChannel(sensorType, value);

		/*
		 * 
		 * // check data ID and update correct channel switch (frame[1]) { case
		 * 0x01: updateChannel(SensorTypes.gps_altitude_before,
		 * getSignedLE16BitValue(frame)); break; case 0x01 + 8:
		 * updateChannel(SensorTypes.gps_altitude_after,
		 * getUnsignedLE16BitValue(frame)); break; case 0x02:
		 * updateChannel(SensorTypes.temp1, getSignedLE16BitValue(frame));
		 * break; case 0x03: // actual RPM value is Frame1*60 // also needs to
		 * be divided by the number of blades of the prop! (=> // TODO for
		 * settings) updateChannel(SensorTypes.rpm,
		 * getUnsignedLE16BitValue(frame) * 60); break; case 0x04:
		 * updateChannel(SensorTypes.fuel, getUnsignedLE16BitValue(frame));
		 * break; case 0x05: updateChannel(SensorTypes.temp2,
		 * getSignedLE16BitValue(frame)); case 0x06: // first 4 bit is battery
		 * cell number // last 12 bit refer to voltage range 0-2100
		 * corresponding 0-4.2V int cell = getBatteryCell(frame); // if (cell <
		 * 1 || cell > 6) { if (cell < 0 || cell > 5) {
		 * Logger.d(this.getClass().toString(),
		 * "failed to handle cell nr out of range: " + cell); break; } double
		 * value = getCellVoltage(frame); // get the right channeltype based on
		 * cell nr String channelType = "volt_" + cell; // and update
		 * updateChannel(SensorTypes.valueOf(channelType), value); break; case
		 * 0x10: updateChannel(SensorTypes.altitude_before,
		 * getSignedLE16BitValue(frame)); break; case 0x21:
		 * updateChannel(SensorTypes.altitude_after,
		 * getUnsignedLE16BitValue(frame)); break; case 0x11:
		 * updateChannel(SensorTypes.gps_speed_before,
		 * getUnsignedLE16BitValue(frame)); break; case 0x11 + 8:
		 * updateChannel(SensorTypes.gps_speed_after,
		 * getUnsignedLE16BitValue(frame)); break; case 0x12:
		 * updateChannel(SensorTypes.longitude_before,
		 * getUnsignedBE16BitValue(frame)); break; case 0x12 + 8:
		 * updateChannel(SensorTypes.longitude_after,
		 * getUnsignedBE16BitValue(frame)); break; case 0x1A + 8:
		 * updateChannel(SensorTypes.ew, getUnsignedLE16BitValue(frame)); break;
		 * case 0x13: updateChannel(SensorTypes.latitude_before,
		 * getUnsignedBE16BitValue(frame)); break; case 0x13 + 8:
		 * updateChannel(SensorTypes.latitude_after,
		 * getUnsignedBE16BitValue(frame)); break; case 0x1B + 8:
		 * updateChannel(SensorTypes.ns, getUnsignedLE16BitValue(frame)); break;
		 * case 0x14: // should be between 0 & 359.99 degrees?? // FIXME seems
		 * to work but with much noise updateChannel(SensorTypes.course_before,
		 * getUnsignedLE16BitValue(frame)); break; case 0x14 + 8:
		 * updateChannel(SensorTypes.course_after,
		 * getUnsignedLE16BitValue(frame)); break; case 0x15:
		 * updateChannel(SensorTypes.day, frame[2]);
		 * updateChannel(SensorTypes.month, frame[3]); break; case 0x16:
		 * updateChannel(SensorTypes.year, 2000 + frame[2]); break; case 0x17:
		 * updateChannel(SensorTypes.hour, frame[2]);
		 * updateChannel(SensorTypes.minute, frame[3]); break; case 0x18:
		 * updateChannel(SensorTypes.second, frame[2]); break; case 0x24: //
		 * actual 3-axis value is Frame1/1000 updateChannel(SensorTypes.acc_x,
		 * getSignedLE16BitValue(frame) / 1000); break; case 0x25: // actual
		 * 3-axis value is Frame1/1000 updateChannel(SensorTypes.acc_y,
		 * getSignedLE16BitValue(frame) / 1000); break; case 0x26: // actual
		 * 3-axis value is Frame1/1000 updateChannel(SensorTypes.acc_z,
		 * getSignedLE16BitValue(frame) / 1000); break; default: // TODO add
		 * current sensor data Logger.d(FrSkyServer.TAG,
		 * "Unknown sensor type for frame: " + Arrays.toString(frame));
		 * 
		 * 
		 * }
		 */
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
		// TODO create a channel here for the correct type of information and
		// broadcast channel so GUI can update this value
		Logger.d(TAG, "Data received for channel: " + sensorType
				+ ", value: " + value);

		/*
		 * eso, prototype Channel support Update a proper channel rather than
		 * broadcast. Allow broadcasts until Channels are fully implemented
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
		case altitude_before:
			// /TODO: Proper construction of resulting altitude double needs to
			// be done in handleHubDataFrame or extractUserDataBytes
			getChannel(CHANNEL_ID_ALTITUDE).setRaw(value);
			break;
		case altitude_after:
			getChannel(CHANNEL_ID_ALTITUDE).setRaw(value);
		case acc_x:
			getChannel(CHANNEL_ID_ACCELERATOR_X).setRaw(value);
			break;
		case acc_y:
			getChannel(CHANNEL_ID_ACCELERATOR_Y).setRaw(value);
			break;
		case acc_z:
			getChannel(CHANNEL_ID_ACCELERATOR_Z).setRaw(value);
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
		case gps_altitude_before:
			getChannel(CHANNEL_ID_GPS_ALTITUDE).setRaw(value);
			break;
		case gps_speed_after:
			getChannel(CHANNEL_ID_GPS_SPEED).setRaw(value);
			break;
		case gps_speed_before:
			getChannel(CHANNEL_ID_GPS_SPEED).setRaw(value);
			break;
		case gps_course_after:
			getChannel(CHANNEL_ID_GPS_COURSE).setRaw(value);
			break;
		case gps_course_before:
			getChannel(CHANNEL_ID_GPS_COURSE).setRaw(value);
			break;
		case gps_latitude_after:
			getChannel(CHANNEL_ID_GPS_LATITUDE).setRaw(value);
			break;
		case gps_latitude_before:
			getChannel(CHANNEL_ID_GPS_LATITUDE).setRaw(value);
			break;
		case gps_longitude_after:
			getChannel(CHANNEL_ID_GPS_LONGITUDE).setRaw(value);
			break;
		case gps_longitude_before:
			getChannel(CHANNEL_ID_GPS_LONGITUDE).setRaw(value);
			break;
		}

		// let server update this information
		server.broadcastChannelData(sensorType, value);
	}

	/**
	 * Prototype Channel support eso
	 */

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
	public static final int CHANNEL_ID_ACCELERATOR_X = 4 + HUB_ID;
	public static final int CHANNEL_ID_ACCELERATOR_Y = 5 + HUB_ID;
	public static final int CHANNEL_ID_ACCELERATOR_Z = 6 + HUB_ID;
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

	/**
	 * Poor solution for holding decimal part of altitude
	 * 
	 */
	public static double alt_after = 0;

	/**
	 * Treemap to hold the Hubs channels
	 */
	//private static TreeMap<Integer, Channel> _sourceChannelMap;

	/**
	 * 
	 * @return a ServerChannel corresponding to the given id
	 */
//	public static Channel getSourceChannel(int id) {
//		return _sourceChannelMap.get(id);
//	}

	/**
	 * TODO Should not be static since this relies on data initialised in the
	 * ctor, need to review this.
	 * 
	 * @return all the ServerChannels in a TreeMap (key is the id of the
	 *         Channel)
	 */
//	public TreeMap<Integer, Channel> getSourceChannels() {
//		return _sourceChannelMap;
//	}

	/**
	 * Create the _sourceChannelMap Populate it with our channels
	 */
	protected void initializeChannels() {
//		_sourceChannelMap = new TreeMap<Integer, Channel>(
//				Collections.reverseOrder());

		// Sets up the hardcoded channels (Altitude,RPM)
		// TODO: Figure out how to deal with race conditions on "split numbers"
		// TODO create enums with descruption etc or proper factory instead
		configureChannelForSensor(CHANNEL_ID_ALTITUDE, "Hub: Altitude");
		configureChannelForSensor(CHANNEL_ID_RPM, "Hub: RPM (pulses)");
		configureChannelForSensor(CHANNEL_ID_TEMP1, "Hub: Temp 1");
		configureChannelForSensor(CHANNEL_ID_TEMP2, "Hub: Temp 2");
		configureChannelForSensor(CHANNEL_ID_ACCELERATOR_X, "Hub: Acc. X");
		configureChannelForSensor(CHANNEL_ID_ACCELERATOR_Y, "Hub: Acc. Y");
		configureChannelForSensor(CHANNEL_ID_ACCELERATOR_Z, "Hub: Acc. Z");
		configureChannelForSensor(CHANNEL_ID_FUEL, "Hub: Fuel");
		configureChannelForSensor(CHANNEL_ID_GPS_ALTITUDE, "Hub: GPS Altitude");
		configureChannelForSensor(CHANNEL_ID_GPS_COURSE, "Hub: GPS Course");
		configureChannelForSensor(CHANNEL_ID_GPS_LATITUDE, "Hub: GPS Latitude");
		configureChannelForSensor(CHANNEL_ID_GPS_LONGITUDE,
				"Hub: GPS Longitude");
		configureChannelForSensor(CHANNEL_ID_GPS_SPEED, "Hub: GPS Speed");
		configureChannelForSensor(CHANNEL_ID_LIPO_CELL_1, "Hub: Lipo Cell 1");
		configureChannelForSensor(CHANNEL_ID_LIPO_CELL_2, "Hub: Lipo Cell 2");
		configureChannelForSensor(CHANNEL_ID_LIPO_CELL_3, "Hub: Lipo Cell 3");
		configureChannelForSensor(CHANNEL_ID_LIPO_CELL_4, "Hub: Lipo Cell 4");
		configureChannelForSensor(CHANNEL_ID_LIPO_CELL_5, "Hub: Lipo Cell 5");
		configureChannelForSensor(CHANNEL_ID_LIPO_CELL_6, "Hub: Lipo Cell 6");
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

	public static int getBefore(double value) {
		return (int) Math.round(value);
	}

	public static int getAfter(double value) {
		return (int) (value - Math.round(value));
	}

	public static double convertToAfter(int value) {
		return Double.parseDouble("0." + value);
	}

	/* (non-Javadoc)
	 * @see biz.onomato.frskydash.hub.Hub#getId()
	 */
	@Override
	public int getId() {
		return HUB_ID;
	}

	/* (non-Javadoc)
	 * @see biz.onomato.frskydash.hub.Hub#addUserBytes(int[])
	 */
	@Override
	public void addUserBytes(int[] ints) {
		// FIXME incomplete
		// shall take user bytes
		// decode (destuff) them, and add to internal queue
		// then identify hub frames within this queue
		// and pass them to handleHubDataFrame
		
	}

	/* (non-Javadoc)
	 * @see biz.onomato.frskydash.hub.Hub#addUserBytes(byte[])
	 */
	@Override
	public void addUserBytes(byte[] bytes) {
		// FIXME incomplete
		
	}

}
