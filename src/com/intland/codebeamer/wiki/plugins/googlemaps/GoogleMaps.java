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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.persistence.dao.impl.UserDaoImpl;
import com.intland.codebeamer.persistence.dto.GeoLocationDto;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.search.GeoLocation;
import com.intland.codebeamer.search.GeoLocationImpl;
import com.intland.codebeamer.wiki.plugins.base.AbstractCodeBeamerWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.NamedPluginException;

/**
 * The GoogleMaps plugin inserts a Map on the Wiki page and allows the user to
 * place markers and lines on the map. Markers can display an info window when
 * moving the mouse over them.
 *
 * @author Amir Farokhzad
 */
public class GoogleMaps extends AbstractCodeBeamerWikiPlugin {

	public static final String KEY_PROPERTY = "jspwiki.Google.maps.key";

	/** Marker start. Example: "Red Marker P1: 100,200" */
	public static final String REGEX_MARKER = ".*Marker.*:.*";

	public static final String REGEX_LINES = "(.*)Lines:.*";

	private static final String KEY_NOT_FOUND = "The {{GoogleMaps}} plugin __requires__ to "
			+ "[obtain an API key from Google Maps|http://www.google.com/apis/maps/]. \\\\"
			+ "Add the API key as a new property {{jspwiki.Google.maps.key=...}} "
			+ "to the {{WEB-INF/classes/" + PropertyManager.PROPERTY_FILE +"}} configuration file.";

	private WikiContext wikiContext;

	// id generator for google maps plugin
	private static volatile int idgen = 0;

	/**
	 * Creates the JavaScript code which is needed to show the plugin on the
	 * jspwiki.
	 *
	 * @throws PluginException
	 */
	public String execute(WikiContext context, Map params) throws PluginException {
		/** The complete HTML and JavaScript code for the map */
		String map;
		String htmlId = "GoogleMaps_plugin_" + Integer.toHexString(idgen++);

		this.wikiContext = context;
		try {
			PropertyManager.init();
		} catch (Exception ex) {
			return ex.getMessage();
		}
		String lat = (String) params.get("lat");
		String lng = (String) params.get("lng");
		String zoom = (String) params.get("zoom");
		String mtype = (String) params.get("type");
		String mcontrol = (String) params.get("control");
		String key = (String) params.get("key");
		String width = (String) params.get("width");
		String height = (String) params.get("height");
		String trace = (String) params.get("trace");

		if (key == null || key.equals("")) {
			key = PropertyManager.getProperty(KEY_PROPERTY);
			if (key == null || key.equals("")) {
				return context.getEngine().textToHTML(context, KEY_NOT_FOUND);
			}
		}

		// CodeBeamer-specific parameters
		// FIXME: implement as a separate module
		StringBuffer markerLines = new StringBuffer("");
		StringBuffer membersInfo = new StringBuffer ("");
		boolean showMembers = "true".equalsIgnoreCase((String) params.get("showMembers"));
		if (showMembers) {
			ProjectDto project;
			try {
				project = discoverProject(params, context);
			} catch (NamedPluginException ex) {
				return renderErrorTemplate(ex);
			}

			if (project != null) {
				// Get project members list
				List<UserDto> members = UserDaoImpl.getInstance().findByProjectId(null, project.getId(), false);
				membersInfo.append("Total-" + members.size());

				final GeoLocation geoLocation = GeoLocationImpl.getInstance();

				int locatedMembers = 0;
				for (UserDto member : members) {
					GeoLocationDto location = geoLocation.findByAddress(member, key);

					if (location != null) {
						String latLng = location.getLatitude() + ", " + location.getLongitude();
						final String address = geoLocation.getGoogleMapsStyledAddress(member);
						markerLines.append("\nMarker: " + latLng + "\n" + member.getName() + " (" + address + ")\n\n");
						locatedMembers++;
					}
				}

				if (locatedMembers == 0) {
					return "No project members with valid addresses found.";
				}
				membersInfo.append(" Located-" + locatedMembers);
			}
		}
		// End of CodeBeamer-specific parameters

		if (lat == null || lat.equals("")) {
			lat = "49.12271669366686";
		}

		if (lng == null || lng.equals("")) {
			lng = "9.206113815307617";
		}

		if (zoom == null || zoom.equals("")) {
			zoom = "5";
		}

		if (mtype == null) {
			mtype = "G_NORMAL_MAP";
		} else if (mtype.trim().equalsIgnoreCase("map")) {
			mtype = "G_NORMAL_MAP";
		} else if (mtype.trim().equalsIgnoreCase("sat")) {
			mtype = "G_SATELLITE_MAP";
		} else if (mtype.trim().equalsIgnoreCase("hybrid")) {
			mtype = "G_HYBRID_TYPE";
		} else {
			mtype = "G_NORMAL_MAP";
		}
		if (mcontrol == null) {
			mcontrol = "new GLargeMapControl()";
		} else if (mcontrol.trim().equalsIgnoreCase("small")) {
			mcontrol = "new GSmallMapControl()";
		} else {
			mcontrol = "new GLargeMapControl()";
		}

		if (width == null || width.equals("")) {
			width = "400";
		}

		if (height == null || height.equals("")) {
			height = "400";
		}

		String body = (String) params.get("_body");
		if (body == null)
			body = "";
		else {
			body = body.replaceAll("\r\n", "\n");
			body = body.replaceAll("\r", "\n");
		}
		// CB-specific: add generated markers to body lines
		body += markerLines.toString();
		// end of CB-specific
		String[] bodyLines = body.trim().split("\n");

		int widthInt = Integer.parseInt(width);

		map = "<!-- Start of GoogleMaps Code -->\n";
		map += "<script src=\"http://maps.google.com/maps?file=api&v=2&key=" + key + "\" type=\"text/javascript\"></script>\n";
		map += "<div id=\"" + htmlId + "\" style=\"width: " + width + "px; height: " + height + "px\"></div>\n";

		if (trace != null && trace.trim().toLowerCase().equals("true")) {
			map += "<div id=\"coordinate\">\n";
			map += "<div style=\"width: "
					+ widthInt
					/ 3
					+ "px; padding-left:1px; padding-top:3px; padding-bottom:3px; background-color:#FFFF00; float:left;\">Latitude: <span id=\"lat\">"
					+ lat + "</span></div>\n";
			map += "<div style=\"width: "
					+ widthInt
					/ 3
					+ "px; padding-top:3px; padding-bottom:3px; background-color:#FFFF00; float:left;\">Longitude: <span id=\"lng\">"
					+ lng + "</span></div>\n";
			map += "<div style=\"width: " + widthInt / 6
					+ "px; padding-top:3px; padding-bottom:3px; background-color:#FFFF00; float:left;\">Zoom: <span id=\"zoom\">"
					+ zoom + "</span></div>\n";
			map += "<div style=\"width: "
					+ widthInt
					/ 6
					+ "px; padding-top:3px; padding-bottom:3px; background-color:#FFFF00; float:left;\"><a href=\"http://www.i3g.hs-heilbronn.de\">Plugin by i3G</a></div>\n";
			map += "</div>\n";
			if (showMembers) {
				map += "<br/><div>\n";
				map += "<strong>Members: </strong>" + membersInfo + "\n";
				map += "</div>\n";
			}
		}

		map += "<script type=\"text/javascript\">\n";
		map += "//<![CDATA[\n";

		final String init_function_name = "googlemaps" + htmlId;
		map += "var " + init_function_name +" = function() {\n";

		// CB-specific:
		map += "var markerBounds = new GLatLngBounds();\n";
		// end of CB-specific
		map += "var map = new GMap2(document.getElementById(\"" + htmlId +"\"));\n";
		map += "map.addControl(" + mcontrol + ");\n";
		map += "map.addControl(new GScaleControl());\n";
		map += "map.addControl(new GMapTypeControl());\n";
		map += "var latlng = new GLatLng(" + lat + ", " + lng + ");\n";
		map += "map.setCenter(latlng, " + zoom + ", " + mtype + ");\n";

		Marker[] markers = findMarkers(bodyLines);
		map += handleJavascriptObjects(markers);

		Polyline[] polyline = GoogleMaps.findpolyLines(bodyLines);
		map += handleJavascriptObjects(polyline);

		// FIX: error in original code.
		// The DOM elements on which the listeners try to get access
		// will be present in code if only "trace=true" is specified
		if (trace != null && trace.trim().toLowerCase().equals("true")) {
			map += addInfoListener();
		}
		// CB-specific: sep pan/zoom to get all markers visible
		if (showMembers) {
			map += "map.setCenter(markerBounds.getCenter(), map.getBoundsZoomLevel(markerBounds), " + mtype + ");\n";
		}
		// end of CB-specific
		map += "map.addControl(new GOverviewMapControl(new GSize(150,150)));\n";
		map += "}\n";

		// CB-specific: safe call of function when it's available only.
		// Necessary for IE7
		map += "YAHOO.util.Event.onAvailable(\"" + htmlId +"\", " + init_function_name +");\n";
		// end of CB-specific

		map += "//]]>\n";
		map += "</script>\n";
		map += "<!-- End of GoogleMaps Code -->\n";
		// Clear marker list
		Marker.getMarkerList().clear();
		IconManager.clear();
		return map;
	}

	/**
	 * Searches for marker entries in the plugin body. These are defined in a
	 * marker section, wich startes with a line containing "Marker:". For every
	 * marker line a new marker object is created.
	 *
	 * @param bodyLines
	 *            The lines of the plugin body
	 * @return An array containing the created marker objects
	 */
	private Marker[] findMarkers(String[] bodyLines) {
		boolean markerSection = false;
		ArrayList markerList = new ArrayList();
		String markerCode = "";
		for (int i = 0; i < bodyLines.length; i++) {
			String bodyLine = bodyLines[i].trim();

			// Marker starts here
			// TODO RegEx
			Matcher matcher = RegEx.getMatcher(REGEX_MARKER, bodyLine);
			if (matcher.matches()) {
				markerSection = true;
			}

			if (markerSection) {
				markerCode += bodyLines[i] + "\n";
				String nextLine;
				if (i < bodyLines.length - 1) {
					nextLine = bodyLines[i + 1];
				} else {
					nextLine = "";
				}

				// Marker section ends when an empty line appears
				// and no "Tab:" is found after the empty line
				// or when the end of the body is reached
				// Now create Marker
				boolean moreTabs = nextLine.startsWith("Tab:");
				if ((bodyLines[i].length() == 0 && !moreTabs) || i == bodyLines.length - 1) {
					Marker marker = new Marker(markerCode.trim(), wikiContext);
					markerList.add(marker);
					markerCode = "";
					markerSection = false;
				}
			}
		}

		int length = markerList.size();
		return (Marker[]) markerList.toArray(new Marker[length]);
	}

	/**
	 * @param object
	 * @return The joined javascripts of the objects
	 */
	private String handleJavascriptObjects(JavascriptObject[] objects) {
		StringBuffer buf = new StringBuffer();
		for (JavascriptObject object : objects) {
			buf.append(object.createJavaScript()).append("\n");
		}
		return buf.toString();
	}

	private static Polyline[] findpolyLines(String[] bodyLines) {
		boolean lineSection = false;
		ArrayList pLineList = new ArrayList();
		String color = "";
		for (int i = 0; i < bodyLines.length; i++) {
			String bodyLine = bodyLines[i].trim();
			if (bodyLine.length() == 0)
				continue;
			Matcher matcher = RegEx.getMatcher(REGEX_LINES, bodyLine);
			if (matcher.matches()) {
				lineSection = true;
				color = matcher.group(1).trim().toLowerCase();
			} else if (bodyLine.indexOf("Marker") >= 0) {
				lineSection = false;
			} else if (lineSection) {
				Polyline pLine = new Polyline(bodyLine, color);
				pLineList.add(pLine);
			}
		}
		int length = pLineList.size();
		return (Polyline[]) pLineList.toArray(new Polyline[length]);

	}

	private String addInfoListener() {
		String script;
		script = "GEvent.addListener(map, 'moveend', function() {\n";
		script += "  document.getElementById(\"lat\").innerHTML = map.getCenter().y\n";
		script += "  document.getElementById(\"lng\").innerHTML = map.getCenter().x\n";
		script += "  document.getElementById(\"zoom\").innerHTML = map.getZoom()\n";
		script += "});\n";
		return script;
	}
}
