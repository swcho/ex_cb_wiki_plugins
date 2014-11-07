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
package com.intland.codebeamer.wiki.plugins.util;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.ecyrd.jspwiki.TextUtil;

/**
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 * @version $Id$
 */
public class WikiPluginUtils {
	public static String getParameter(Map params, String paramId) {
		return getParameter(params, paramId, null);
	}

	public static String getParameter(Map params, String paramId, String defaultValue) {
		String ret = defaultValue;

		Object parm = params.get(paramId);
		String prm = StringUtils.trimToNull(parm == null ? null : parm.toString());

		if (prm != null) {
			ret = TextUtil.replaceEntities(prm);
		}

		return ret;
	}

	/**
	 * Returns the value of plugin parameter.
	 *
	 * @param params       the map of parameters passed to this plugin
	 * @param paramId      the parameter id
	 * @param defaultValue value to be returned if requested parameter doesn't exist
	 * @return             process parameter
	 */
	public static String getStringParameter(Map params, String paramId, String defaultValue) {
		Object parm = params.get(paramId);
		return parm != null ? TextUtil.replaceEntities(parm.toString().trim()) : defaultValue;
	}

	/**
	 * Returns the value of plugin parameter.
	 *
	 * @param params       the map of parameters passed to this plugin
	 * @param paramId      the parameter id
	 * @param allowed      List<String> of allowed valies. If <code>null</code> then any value will be acceptable.
	 * @param defaultValue value to be returned if requested parameter doesn't exist or set to incorrect value
	 * @return             process parameter
	 */
	public static String getStringParameter(Map params, String paramId, Collection<String> allowedValues, String defaultValue) {
		Object parm = params.get(paramId);
		String p = parm != null ? TextUtil.replaceEntities(parm.toString().trim()) : null;
		boolean enabled = allowedValues != null && p != null ? allowedValues.contains(p) : true;
		return p != null && enabled ? p : defaultValue;
	}
}
