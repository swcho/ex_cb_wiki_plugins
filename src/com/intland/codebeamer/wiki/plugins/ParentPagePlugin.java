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

import org.apache.velocity.VelocityContext;

import com.ecyrd.jspwiki.WikiContext;
import com.intland.codebeamer.manager.WikiPageManager;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.dto.WikiPageDto;
import com.intland.codebeamer.wiki.plugins.base.AbstractCodeBeamerWikiPlugin;

/**
 * Plugin to generate a links to the parent page of the current page.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 * @version $Id$
 */
public class ParentPagePlugin extends AbstractCodeBeamerWikiPlugin {
	public String execute(WikiContext context, Map params) {
		UserDto user = getUserFromContext(context);
		WikiPageDto page = getPageFromContext(context);

		// get parent page
		WikiPageDto parentPage = WikiPageManager.getInstance().findById(user, page.getParent().getId());

		VelocityContext velocityContext = getDefaultVelocityContextFromContext(context);
		if(parentPage != null) {
			velocityContext.put("parentPage", parentPage);
		}

		return renderPluginTemplate("parentpage-plugin.vm", velocityContext);
	}
}
