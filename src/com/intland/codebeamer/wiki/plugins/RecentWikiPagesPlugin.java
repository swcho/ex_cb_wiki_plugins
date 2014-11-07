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

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.velocity.VelocityContext;

import com.ecyrd.jspwiki.WikiContext;

import com.intland.codebeamer.persistence.dao.impl.ArtifactDaoImpl;
import com.intland.codebeamer.persistence.dto.ArtifactDto;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.remoting.ArtifactType;
import com.intland.codebeamer.wiki.plugins.base.AbstractCodeBeamerWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.NamedPluginException;

/**
 * Plugin to display list of recently changed/commented wiki pages.
 *
 * @author <a href="mailto:kao@intland.com">Alexey Kolesnikov</a>
 * @version $Id: zluspai 2008-11-10 10:10 +0000 19269:5c014986a338  $
 */
public class RecentWikiPagesPlugin extends AbstractCodeBeamerWikiPlugin {
	private static final String RENDER_TEMPLATE = "recentwikipages-plugin.vm";

	public String execute(WikiContext context, Map params) {
		UserDto user = getUserFromContext(context);
		ProjectDto project;
		try {
			project = discoverProject(params, context);
		} catch (NamedPluginException ex) {
			return renderErrorTemplate(ex);
		}

		int max = NumberUtils.toInt(getParameter(params, "max"), 5);
		int since = NumberUtils.toInt(getParameter(params, "since"), 100);

		// set up Velocity context
		VelocityContext velocityContext = getDefaultVelocityContextFromContext(context);
		velocityContext.put("wikiUpdates", getUpdates(user, project, since, max));
		velocityContext.put("isCurrentProject", isEnclosingProject());
		velocityContext.put("project", project);
		velocityContext.put("contextPath", getApplicationContextPath(context));

		// render template
		return renderPluginTemplate(RENDER_TEMPLATE, velocityContext);
	}

	public List<ArtifactDto> getUpdates(UserDto user, ProjectDto project, int since, int max) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -1 * since);

		List<Integer> pageTypes = Collections.singletonList(Integer.valueOf(project != null ? ArtifactType.PROJECT_WIKIPAGE
																							: ArtifactType.USER_WIKIPAGE));

		return ArtifactDaoImpl.getInstance().findRecentlyModifiedArtifacts(user, project, pageTypes, cal.getTime(), max);
	}
}
