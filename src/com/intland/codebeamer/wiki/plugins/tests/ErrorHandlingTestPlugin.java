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
package com.intland.codebeamer.wiki.plugins.tests;

import java.util.Map;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.wiki.plugin.WikiErrorHandlingTests;
import com.intland.codebeamer.wiki.plugins.base.AbstractCodeBeamerWikiPlugin;

/**
 * Plugin for {@link WikiErrorHandlingTests}
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public class ErrorHandlingTestPlugin extends AbstractCodeBeamerWikiPlugin {

	public enum exceptionTypes {
		RUNTIME_EXCEPTION,
		THROWABLE,
		PLUGIN_EXCEPTION
	}

	// The message part of the exception
	public final static String MSG = "ErrorHandlingTestPlugin-error-" + Math.random();

	public String execute(WikiContext context, Map params) throws PluginException {
		String exceptionType = (String) params.get("exceptionType");
		if (exceptionTypes.RUNTIME_EXCEPTION.name().equals(exceptionType)) {
			throw new RuntimeException(MSG);
		}
		if (exceptionTypes.THROWABLE.name().equals(exceptionType)) {
			throw new Error(MSG);
		}
		if (exceptionTypes.PLUGIN_EXCEPTION.name().equals(exceptionType)) {
			throw new PluginException(MSG);
		}
		return "";
	}

}
