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
package com.intland.codebeamer.wiki.plugins.base;

import org.apache.commons.lang.StringUtils;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.ecyrd.jspwiki.plugin.WikiPlugin;

/**
 * Plugin-Exception holds the plugin's name as additional info about a plugin-exception.
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 * $Id$
 */
public class NamedPluginException extends PluginException {

	private String pluginName;

	/**
	 * Constructor.
	 * @param plugin
	 * @param message
	 */
	public NamedPluginException(WikiPlugin plugin, String message) {
		super(message);
		init(plugin);
	}

	public NamedPluginException(WikiPlugin plugin, String message, Throwable cause) {
		super(message, cause);
		init(plugin);
	}

	public NamedPluginException(WikiPlugin plugin, Throwable cause) {
		super(cause);
		init(plugin);
	}

	private void init(WikiPlugin plugin) {
		pluginName = getPluginName(plugin);
	}


	/**
	 * Get the name of the plugin was throwing this exception
	 * @return the pluginName
	 */
	public String getPluginName() {
		return pluginName;
	}

	public static String  getPluginName(WikiPlugin plugin) {
		if (plugin != null) {
			return StringUtils.substringAfterLast(plugin.getClass().getName(), ".");
		}
		return "Unknown";
	}

}
