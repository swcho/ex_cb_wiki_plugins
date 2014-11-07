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

import java.util.Date;
import java.util.Map;

import org.apache.velocity.VelocityContext;

import com.ecyrd.jspwiki.WikiContext;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.dto.WikiPageDto;
import com.intland.codebeamer.wiki.plugins.base.AbstractCodeBeamerWikiPlugin;

/**
 * Plugin to demonstrate using {@link AbstractCodeBeamerWikiPlugin}.
 * Only for educational purposes.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 * @version $Id$
 */
public class SecondDemoPlugin extends AbstractCodeBeamerWikiPlugin {
	public String execute(WikiContext context, Map params) {
		// get beans from wiki context
		UserDto user = getUserFromContext(context);
		WikiPageDto page = getPageFromContext(context);

		// get default Velocity context
		VelocityContext velocityContext = getDefaultVelocityContextFromContext(context);

		// put the current system time in the user's format to the context
		velocityContext.put("currentTime", user.getDateTimeFormat().format(new Date()));

		// put whether the current user is the owner of the current page to the context
		velocityContext.put("owner", page.getOwner().getId().equals(user.getId()) ? "yes" : "no");

		// render template
		return renderPluginTemplate("seconddemo-plugin.vm", velocityContext);
	}
}
