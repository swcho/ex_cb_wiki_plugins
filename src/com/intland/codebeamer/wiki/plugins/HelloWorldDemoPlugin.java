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
package com.intland.codebeamer.wiki.plugins;

import java.util.Map;

import com.ecyrd.jspwiki.WikiContext;
import com.intland.codebeamer.wiki.plugins.base.AbstractCodeBeamerWikiPlugin;

/**
 * Simplistic plugin to demonstrate the simplest plugin possible.
 * Only for educational purposes.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 * @version $Id$
 */
public class HelloWorldDemoPlugin extends AbstractCodeBeamerWikiPlugin {
	public String execute(WikiContext context, Map params) {
		String name = (String)params.get("name");
		if(name == null) {
			name = "World";
		}

		return "Hello " + name + "!";
	}
}
