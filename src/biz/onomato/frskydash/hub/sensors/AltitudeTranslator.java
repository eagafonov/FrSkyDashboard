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

package biz.onomato.frskydash.hub.sensors;

import biz.onomato.frskydash.hub.FrSkyHub;
import biz.onomato.frskydash.hub.SensorTypes;
import java.lang.Math;

public class AltitudeTranslator implements UserDataTranslator {

	/**
	 * combined value
	 */
	private double altitude = 0.0;
	private double alt_bp,alt_ap,vert_speed;

	private double PRECISION_ALTITUDE = 100.0;
	private double PRECISION_VERTICAL_SPEED = 10.0;
	
	@Override
	public double translateValue(SensorTypes type, int[] frame) {
		switch (type) {
		case altitude_before:
			alt_bp = FrSkyHub.getSignedLE16BitValue(frame);
			break;
		case altitude_after:
			alt_ap = FrSkyHub.getSignedLE16BitValue(frame)/PRECISION_ALTITUDE;
			if(Math.abs(alt_ap)<1.0)	// only add the fraction if it is less than 1 
			{
				altitude = alt_bp+alt_ap;	//as alt_ap is signed, always add
			}
			return altitude;
		case vertical_speed:
			vert_speed = FrSkyHub.getSignedLE16BitValue(frame)/PRECISION_VERTICAL_SPEED;
			return vert_speed;
		}
		return FrSkyHub.UNDEFINED_VALUE;
	}

}
