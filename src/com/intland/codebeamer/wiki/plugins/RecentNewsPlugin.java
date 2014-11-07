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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.velocity.VelocityContext;

import com.ecyrd.jspwiki.WikiContext;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.wiki.plugins.base.AbstractCodeBeamerWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.NamedPluginException;

/**
 * Plugin to display resent news on project forums.
 *
 * @author <a href="mailto:kao@intland.com">Alexey Kolesnikov</a>
 * @version $Id$
 * @deprecated since CB-6.2
 */
public class RecentNewsPlugin extends AbstractCodeBeamerWikiPlugin {
	private static final String RENDER_TEMPLATE = "recentnews-plugin.vm";

	public String execute(WikiContext context, Map params) {
		// get beans from wiki context
//		UserDto user = getUserFromContext(context);

		// parse params
//		int max = NumberUtils.toInt(getParameter(params, "max"), 5);

		ProjectDto project;
		try {
			project = discoverProject(params, context);
		} catch (NamedPluginException ex) {
			return renderErrorTemplate(ex);
		}

		List news = Collections.EMPTY_LIST; //ForumPostManager.getInstance().findRecentlyPostedByForum(user, forumIds, typeIds, max);

		// set up Velocity context
		VelocityContext velocityContext = getDefaultVelocityContextFromContext(context);
		velocityContext.put("isCurrentProject", isEnclosingProject());
		velocityContext.put("project", project);
		velocityContext.put("news", news);
		velocityContext.put("contextPath", getApplicationContextPath(context));

		// render template
		return renderPluginTemplate(RENDER_TEMPLATE, velocityContext);
	}
}
