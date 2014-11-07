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
 * Represents a Line on the map.
 *
 * @author Amir Farokhzad
 *
 */
public class Polyline implements JavascriptObject {

    private Coordinate[] coordinate;

    private final String color;

    /**
     * Creates the line
     *
     * @param line
     *            The line code. Contains coordinates separated by "->"
     */
    public Polyline(String line, String color) {
        color = PropertyManager.getProperty("color."+color);
        if (color==null) this.color="#0000FF"; else this.color="#"+color;
        String[] splittedLine = line.trim().split("->");
        int length = splittedLine.length;
        coordinate = new Coordinate[length];
        if (length > 0) {
            for (int i = 0; i < length; i++)
                coordinate[i] = new Coordinate(splittedLine[i]);
        }
    }

    /** Creates the JavaScript for the line */
    public String createJavaScript() {
        String result = "";
        for (int i = 0; i < coordinate.length; i++) {
            result += "var latlng" + i + " = new GLatLng("
                    + coordinate[i].getLat() + "," + coordinate[i].getLng()
                    + ");\n";
        }
        result += "var pLines = new GPolyline([";
        for (int i = 0; i < coordinate.length; i++) {
            result += "latlng" + i;
            if (i != coordinate.length - 1) {
                result += ",";
            }
        }
        result += "],\""+color+"\");\n";
        result += "map.addOverlay(pLines);\n";
        return result;
    }

    /**
     * Returns the coordinates of the line.
     */
    public Coordinate[] getCoordinate() {
        return coordinate;
    }

}
