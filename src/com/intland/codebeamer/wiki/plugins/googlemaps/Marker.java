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

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;

import com.ecyrd.jspwiki.WikiContext;

/**
 *
 * A marker object stores the properties which are needed to display a marker on
 * the map. Those are latitude, longitude and the info window content.
 *
 * @author Amir Farokhzad
 *
 */
public class Marker implements JavascriptObject {

    private static int counter = 0;

    private static HashMap markerList = new HashMap();

    public static HashMap getMarkerList() {
        return markerList;
    }

    private Coordinate coordinate;

    private String name = "";

    private String icon = "red";

    private String result = "";

    private Tab[] tabs;

    private WikiContext context;

    /**
     * Receives a marker line and splits it. The value for latitude, longitude
     * and texts are extracted.
     *
     * @param code
     *            A marker line containing the marker properties latitude,
     *            longitude and multiple texts seperated by a "|". Example:
     *            Marker P1:45|-122|Text1|Text2|...
     */

    public Marker(String code, WikiContext context) {
    	this.context =  context;
        String[] lines = code.split("\n");
        String markerLine = lines[0];
        int colonPos = markerLine.indexOf(":");
        int markerPos = markerLine.indexOf("Marker");
        if(markerPos>1) icon=markerLine.substring(0,markerPos-1).toLowerCase();
        name = markerLine.substring(markerPos+"Marker".length(), colonPos).trim();
        String coordLine = markerLine.substring(colonPos + 1).trim();

        coordinate = new Coordinate(coordLine);
        String[] tabLines = new String[lines.length - 1];
        System.arraycopy(lines, 1, tabLines, 0, tabLines.length);
        tabs = findTabs(tabLines);
        markerList.put(name.toLowerCase(), this);
    }

    /**
     * Creates the JavaScript code for the current marker object.
     *
     * @return JavaScript code
     */
    public String createJavaScript() {
        if(icon.indexOf("inv")>=0) return "";
        counter++;
        String marker = "marker" + counter;
        String latlng = "latlng" + counter;
        result += IconManager.createIcon(icon);
        result += "var " + latlng + " = new GLatLng(" + coordinate.getLat()
                + "," + coordinate.getLng() + ");\n";
        // CB-specific: extend the markerBounds data with current marker
        result += "markerBounds.extend(" + latlng + ");\n";
        // end of CB-specific
        result += "var " + marker + " = new GMarker(" + latlng + ", icon"+icon+");\n";
        if (tabs.length >= 1) {
            result += "var tabInfos" + counter + " = [";
            for (int i = 0; i < tabs.length; i++) {
                String body = tabs[i].getBody();
                String heading = tabs[i].getHeading();
                String bodyHTML = Wiki2HTMLConverter.wiki2HTML(context, body);
                bodyHTML = bodyHTML.replaceAll("\"", "'");
                result += "\n new GInfoWindowTab(\"" + heading + "\", \""
                        + bodyHTML + "\")";
                if (i != tabs.length - 1) {
                    result += ",";
                }
            }
            result += "\n];\n";
            result += "GEvent.addListener(" + marker
                    + ",\"click\", function() { " + marker
                    + ".openInfoWindowTabsHtml(tabInfos" + counter + ");});\n";
        }
        result += "map.addOverlay(" + marker + ");\n";
        return result;
    }

    /**
     * Returns a coordinate object
     *
     * @return a coordinate object
     */
    public Coordinate getCoordinate() {
        return coordinate;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the texts of marker
     *
     * @return texts of a marker
     */
    public Tab[] getTabs() {
        return tabs;
    }

    public String undoSplit(String[] lines) {
    	return StringUtils.join(lines, "\n");
    }

    /**
     * Searches for Tabs in the marker code. For each tab, a new Tab object is
     * created. When no tabs are defined, only one tab without a name is
     * created.
     *
     * @param lines
     *            The marker code containing the tabs
     * @return An array containing all tabs
     */
    private Tab[] findTabs(String[] lines) {
        ArrayList tabList = new ArrayList();
        String tabCode = "";
        boolean tabSection = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("Tab:")) {
                tabSection = true;
            }
            if (tabSection) {
                tabCode += line + "\n";
                if (line.length() == 0 || i == lines.length - 1) {
                    Tab tab = new Tab(tabCode.trim(), context);
                    tabList.add(tab);
                    tabCode = "";
                    tabSection = false;
                }
            }
        }
        if (tabList.size() == 0) {
            tabCode = undoSplit(lines);
            Tab tab = new Tab("Tab: \n" + tabCode, context);
            tabList.add(tab);
        }
        return (Tab[]) tabList.toArray(new Tab[tabList.size()]);
    }

    public String getIcon() {
        return icon;
    }

}
