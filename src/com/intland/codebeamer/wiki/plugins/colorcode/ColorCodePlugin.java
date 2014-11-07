/*
 * Copyright by Intland Software
 *
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Intland Software. ("Confidential Information"). You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Intland.
 */
package com.intland.codebeamer.wiki.plugins.colorcode;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.plugin.PluginException;

/**
 * Our ColorCode-plugin which delegates all its work to the "lu.intrasoft.jspwiki.plugin" ColorCode plugin,
 * but just allows the plugin's properties be loaded from a sub-package and not only from the root.
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 * $Id$
 */
public class ColorCodePlugin extends lu.intrasoft.jspwiki.plugin.ColorCodePlugin {
	private final static Logger logger = Logger.getLogger(ColorCodePlugin.class);

	/**
	 * The package where the plugin's configuration can be found.
	 */
	private String propertiesPackage = "colorcode";

	/**
	 * Overridden method.
	 */
	public String execute(WikiContext wiki_context, Map params) throws PluginException {
		String syntax = (String)params.get("syntax");
		if (syntax == null) {
			throw new PluginException("Missing syntax parameter!");
		}

		logger.debug("Adding package prefix to syntax='" + syntax + "', package='" + propertiesPackage +"'");
		syntax = propertiesPackage + "/" + syntax;
		params.put("syntax", syntax);

		// do the real job
		String html = super.execute(wiki_context, params);
		if(StringUtils.isBlank(html)) {
			return html;
		}

		// make the output more CB wiki compliant
		html = StringUtils.substringAfter(StringUtils.substringAfter(html, "<code"), ">");
		html = StringUtils.substringBefore(html, "</code>");
		html = "<pre class=\"wiki\">\n" + html + "</pre>";
		html = StringUtils.remove(html, "<br/>");
		html = StringUtils.remove(html, "<b>");
		html = StringUtils.remove(html, "</b>");

		return html;
	}
}
