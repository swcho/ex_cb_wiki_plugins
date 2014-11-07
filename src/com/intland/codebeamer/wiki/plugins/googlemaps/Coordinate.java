/*
 GoogleMaps Plugin

 Copyright (C) 2006 i3G Institut - Hochschule Heilbronn

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation; either version 2.1 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.intland.codebeamer.wiki.plugins.googlemaps;

/**
 * Represents a coordinate (consists of latitude and longitude).
 *
 * @author Amir Farokhzad
 *
 */
public class Coordinate {

	private double lat;

	private double lng;

	/**
	 * Creates the Coordinate object.
	 *
	 * @param coordStr
	 *            Either two float values separated by comma, or the name of a
	 *            marker.
	 */
	public Coordinate(String coordStr) {
		if (coordStr.indexOf(",")>=0) {
			String[] splittedCoordStr = coordStr.trim().split(",");
			lat = Double.parseDouble(splittedCoordStr[0]);
			lng = Double.parseDouble(splittedCoordStr[1]);
		} else {
			// Not a coordinate but a marker name, so use marker pos
			String name = coordStr.trim().toLowerCase();
			Marker marker = (Marker) Marker.getMarkerList().get(name);
			lat = marker.getCoordinate().getLat();
			lng = marker.getCoordinate().getLng();
		}
	}

	public double getLat() {
		return lat;
	}

	public double getLng() {
		return lng;
	}
}
