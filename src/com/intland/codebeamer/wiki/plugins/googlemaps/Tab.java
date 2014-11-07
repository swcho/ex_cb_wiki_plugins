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

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiProvider;

/**
 * Represents a Tab of an info window.
 *
 * @author Amir Farokhzad
 *
 */

public class Tab {

	private String heading = "";

	private String body = "";

	private WikiContext context;

	/**
	 * Creates the Tab
	 *
	 * @param code
	 *            The tab code. The first line contains "Tab: " and the heading.
	 *            Then the tab content follows.
	 */
	public Tab(String code, WikiContext context) {
		this.context =  context;
		String[] lines = code.split("\n");
		heading = lines[0].substring(4).trim();
		for (int i = 1; i < lines.length; i++) {
			String line = lines[i];
			if (line.startsWith("Page:")) {
				String pagename = line.substring(5).trim();
				String pageContent = getPageContent(pagename);
				body += pageContent;
			} else {
				body += line.trim() + "\n";
			}
		}
	}

	/** Returns the content of a given Wiki page */
	private String getPageContent(String pagename) {
		WikiEngine engine = context.getEngine();
		return engine.getPureText(pagename, WikiProvider.LATEST_VERSION);
	}

	public String getBody() {
		return body.trim();
	}

	public String getHeading() {
		return heading;
	}

}
