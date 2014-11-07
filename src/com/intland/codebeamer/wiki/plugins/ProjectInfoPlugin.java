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
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.utils.velocitytool.UserPhotoTool;
import com.intland.codebeamer.wiki.plugins.base.AbstractOncePerRenderingPlugin;
import com.intland.codebeamer.wiki.plugins.base.NamedPluginException;

/**
 * Plugin to display basic information about projects.
 *
 * @author <a href="mailto:kao@intland.com">Alexey Kolesnikov</a>
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 * @version $Id$
 */
public class ProjectInfoPlugin extends AbstractOncePerRenderingPlugin {
	private static final String RENDER_TEMPLATE = "projectinfo-plugin.vm";

	@Override
	protected String executeOncePerRendering(WikiContext context, Map params) {
		// stop rendering if projectID specified is incorrect
		ProjectDto project;
		try {
			project = discoverProject(params, context);
		} catch (NamedPluginException ex) {
			return renderErrorTemplate(ex);
		}

		// render template
		VelocityContext velocityContext = getDefaultVelocityContextFromContext(context);
		velocityContext.put("wikiContext", context);
		velocityContext.put("project", project);
		velocityContext.put("userPhotoTool", new UserPhotoTool(context.getHttpRequest()));

		return renderPluginTemplate(RENDER_TEMPLATE, velocityContext);
	}
}
