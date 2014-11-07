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
import com.intland.codebeamer.manager.ProjectManager;
import com.intland.codebeamer.manager.WikiPageManager;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.dto.WikiPageDto;
import com.intland.codebeamer.wiki.plugins.base.AbstractCodeBeamerWikiPlugin;

/**
 * Plugin to generate a link to the homepage of the enclosing project.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 * @version $Id$
 */
public class HomePagePlugin extends AbstractCodeBeamerWikiPlugin {
	public String execute(WikiContext context, Map params) {
		UserDto user = getUserFromContext(context);
		WikiPageDto page = getPageFromContext(context);

		// get homepage
		ProjectDto project = ProjectManager.getInstance().findById(user, page.getProject().getId());
		WikiPageDto homepage = WikiPageManager.getInstance().findById(user, project.getWikiHomepageId());

		VelocityContext velocityContext = getDefaultVelocityContextFromContext(context);
		velocityContext.put("homepage", homepage);

		return renderPluginTemplate("homepage-plugin.vm", velocityContext);
	}
}
