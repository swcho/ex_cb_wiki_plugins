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

import java.util.List;
import java.util.Map;

import org.apache.velocity.VelocityContext;

import com.ecyrd.jspwiki.WikiContext;
import com.intland.codebeamer.manager.WikiPageManager;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.dto.WikiPageDto;
import com.intland.codebeamer.wiki.plugins.base.AbstractCodeBeamerWikiPlugin;

/**
 * Plugin to generate a list of links to the childpages of
 * the current page.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 * @version $Id$
 */
public class ChildPagesPlugin extends AbstractCodeBeamerWikiPlugin {
	public String execute(WikiContext context, Map params) {
		UserDto user = getUserFromContext(context);
		WikiPageDto page = getPageFromContext(context);

		// get child pages
		VelocityContext velocityContext = getDefaultVelocityContextFromContext(context);
		List childPages = WikiPageManager.getInstance().findByParent(user, page.getId());
		if(!childPages.isEmpty()) {
			velocityContext.put("childPages", childPages);
		}

		return renderPluginTemplate("childpages-plugin.vm", velocityContext);
	}
}
