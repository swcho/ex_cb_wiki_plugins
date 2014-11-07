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
package com.intland.codebeamer.wiki.plugins.command.base;

import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.validation.DataBinder;
import org.springframework.validation.ObjectError;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.wiki.plugins.base.AbstractCodeBeamerWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.AbstractCommandWikiPlugin;

/**
 * Command for plugins which depend on/allow filtering by a project.
 * This command adds support for:
 * <pre>
 * - by default supports the current project, which is automatically discovered
 * - can be overridden a project by its project id
 * - or disabling filtering by projects by using the allProjects="true" parameter
 * </pre>
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 * $Id$
 */
public class ProjectAwareWikiPluginCommand extends AbstractMaxWikiPluginCommand {
	private final static Logger logger = Logger.getLogger(ProjectAwareWikiPluginCommand.class);

	// optional parameter for filtering by project-id
	public final static String PARAM_PROJECT_ID = AbstractCodeBeamerWikiPlugin.PROJECT_ID;
	// optional parameter allow showing content from all project. By default we show only entities from current project.
	public final static String PARAM_FROM_ALL_PROJECTS = "allProjects";

	//  optional parameter for filtering by project-id
	private Integer projectId;
	// optional parameter allow showing content from all project. By default we show only entities from current project.
	private boolean allProjects = false;

	/**
	 * @return the projectId
	 */
	public Integer getProjectId() {
		if (isAllProjects()) {
			return null;
		}
		return projectId;
	}
	/**
	 * @param projectId the projectId to set
	 */
	public void setProjectId(Integer projectId) {
		this.projectId = projectId;
	}
	/**
	 * @return the allProject
	 */
	public boolean isAllProjects() {
		return allProjects;
	}
	/**
	 * @param allProject the allProject to set
	 */
	public void setAllProjects(boolean allProjects) {
		this.allProjects = allProjects;
	}

	/**
	 * Convenience method finds the current project, and puts to the projectId, but only if not allProjects is used.
	 * Usage from the plugin:
	 * <code>
	 * 		command.discoverProject(this);
	 * </code>
	 *
	 * Use it after binding, from the {@link AbstractCommandWikiPlugin#validate(org.springframework.validation.DataBinder, Object, java.util.Map)} method.
	 * @param plugin
	 * @throws PluginException If the discovery fails, or project not found
	 */
	public void discoverProject(AbstractCommandWikiPlugin plugin, Map params) throws PluginException {
		if (projectId == null && !isAllProjects()) {
			ProjectDto currentProject = plugin.discoverProject(params, plugin.getWikiContext(), true);
			projectId = (currentProject != null ? currentProject.getId() : null);
			logger.info("Discovered current project, only showing entities from this: projectId=" + projectId);
		}
	}

	/**
	 * Allow the command bean to validate itself.
	 * @param binder
	 * @param params
	 */
	public void validate(DataBinder binder, Map params) {
		if (this.getMax().intValue() <= 0) {
			String msg = "Invalid max parameter value, only positive integer values are allowed! value=" + this.getMax().intValue();
			binder.getBindingResult().addError(new ObjectError("max", msg));
		}
		if (this.getProjectId() != null && this.getProjectId().intValue() <= 0) {
			String msg = "Invalid projectId parameter value, only positive integer values are allowed! value=" + this.getProjectId().intValue();
			binder.getBindingResult().addError(new ObjectError(AbstractCodeBeamerWikiPlugin.PROJECT_ID, msg));
		}
	}
}
