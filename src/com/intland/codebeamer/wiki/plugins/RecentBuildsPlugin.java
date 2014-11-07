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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.velocity.VelocityContext;

import com.ecyrd.jspwiki.WikiContext;
import com.intland.codebeamer.manager.BuildManager;
import com.intland.codebeamer.manager.ScmRepositoryManager;
import com.intland.codebeamer.persistence.dto.BuildLogDto;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.ScmRepositoryDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.wiki.plugins.base.AbstractCodeBeamerWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.NamedPluginException;

/**
 * Plugin to display a short resume on project builds.
 *
 * @author <a href="mailto:kao@intland.com">Alexey Kolesnikov</a>
 * @version $Id: zluspai 2008-11-10 10:10 +0000 19269:5c014986a338  $
 * @deprecated since CB-5.7 builds are deprecated and will be removed completely in the future
 */
@SuppressWarnings("deprecation")
public class RecentBuildsPlugin extends AbstractCodeBeamerWikiPlugin {
	private static final int DEFAULT_MAX_RECORDS = 5;

	private static final String NO_BUILDS = "build.run.recent.none";
//	private static final String NO_PERMISSION = "build.run.permission.none";

	private static final String RENDER_TEMPLATE = "recentbuilds-plugin.vm";

	public String execute(WikiContext context, Map params) {
		// get beans from wiki context
		UserDto user = getUserFromContext(context);

		// parse params
		int max = NumberUtils.toInt(getParameter(params, "max"), DEFAULT_MAX_RECORDS);

		ProjectDto project;
		try {
			project = discoverProject(params, context);
		} catch (NamedPluginException ex) {
			return renderErrorTemplate(ex);
		}

		List<BuildLogDto> logs = new ArrayList<BuildLogDto>();
		for (ScmRepositoryDto repository : ScmRepositoryManager.getInstance().findByProject(user, project.getId())) {
			logs.addAll(BuildManager.getInstance().findRecentByRepositoryId(user, repository.getId(), max));
		}

		String emptyMessage = NO_BUILDS;
//		if (logs.isEmpty()) {
//			emptyMessage = NO_PERMISSION;
//		}

		// set up Velocity context
		VelocityContext velocityContext = getDefaultVelocityContextFromContext(context);
		velocityContext.put("isCurrentProject", isEnclosingProject());
		velocityContext.put("project", project);
		velocityContext.put("logs", logs);
		velocityContext.put("emptyMessage", emptyMessage);
		velocityContext.put("contextPath", getApplicationContextPath(context));

		// render template
		return renderPluginTemplate(RENDER_TEMPLATE, velocityContext);
	}
}
