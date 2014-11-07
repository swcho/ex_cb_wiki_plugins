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

import java.io.IOException;
import java.io.StringReader;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.parser.JSPWikiMarkupParser;
import com.ecyrd.jspwiki.parser.MarkupParser;
import com.ecyrd.jspwiki.parser.WikiDocument;
import com.ecyrd.jspwiki.render.WikiRenderer;
import com.ecyrd.jspwiki.render.XHTMLRenderer;

/**
 * Converts Wiki Code to HTML. Needed for the info windows, as they can contain
 * Wiki Code.
 *
 * @author Amir Farokhzad
 *
 */
public class Wiki2HTMLConverter {

	public static String wiki2HTML(WikiContext wikiContext, String wikiMarkup) {
		StringReader stringReader = new StringReader(wikiMarkup);
		String html = "Error parsing";

		// code for 2.3? always returns "" for 2.4 branch
		/* TranslatorReader translatorReader = new TranslatorReader(wikiContext,
				stringReader);
		StringWriter stringWriter = new StringWriter();
		try {
			FileUtil.copyContents(translatorReader, stringWriter);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return stringWriter.toString().replace("\n", "<br>"); */

		// Compliant 2.4 branch code
		MarkupParser parser = new JSPWikiMarkupParser( wikiContext, stringReader );
		try {
			WikiDocument doc = parser.parse();
			WikiRenderer rend = new XHTMLRenderer( wikiContext, doc );
			html = rend.getString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return html;


	}

}
