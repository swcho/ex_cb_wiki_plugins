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

import org.apache.commons.lang.math.NumberUtils;
import org.apache.velocity.VelocityContext;

import com.ecyrd.jspwiki.WikiContext;
import com.intland.codebeamer.persistence.dao.impl.ArtifactDaoImpl;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.remoting.ArtifactType;
import com.intland.codebeamer.wiki.plugins.base.AbstractCodeBeamerWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.NamedPluginException;

/**
 * Plugin to display recent documents.
 *
 * @author <a href="mailto:kao@intland.com">Alexey Kolesnikov</a>
 * @version $Id$
 */
public class RecentDocumentsPlugin extends AbstractCodeBeamerWikiPlugin {
	private static final String RENDER_TEMPLATE = "recentdocuments-plugin.vm";

	public String execute(WikiContext context, Map params) {
		// get beans from wiki context
		UserDto user = getUserFromContext(context);

		// parse params
		int max = NumberUtils.toInt(getParameter(params, "max"), 5);

		ProjectDto project;
		try {
			project = discoverProject(params, context);
		} catch (NamedPluginException ex) {
			return renderErrorTemplate(ex);
		}

		List docs = ArtifactDaoImpl.getInstance().findRecentArtifacts(user, project.getId(), new Integer(ArtifactType.FILE));

		// truncate list
		if (docs.size() > max) {
			docs = docs.subList(0, max);
		}

		// set up Velocity context
		VelocityContext velocityContext = getDefaultVelocityContextFromContext(context);
		velocityContext.put("isCurrentProject", isEnclosingProject());
		velocityContext.put("project", project);
		velocityContext.put("docs", docs);
		velocityContext.put("contextPath", getApplicationContextPath(context));

		// render template
		return renderPluginTemplate(RENDER_TEMPLATE, velocityContext);
	}
}
