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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.DataBinder;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.controller.support.CommaStringToIntegerListPropertyEditor;
import com.intland.codebeamer.manager.ScmPullRequestManager;
import com.intland.codebeamer.persistence.dto.ScmPullRequestDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.utils.velocitytool.UserPhotoTool;
import com.intland.codebeamer.wiki.plugins.base.AbstractCommandWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.DefaultWebBindingInitializer;

/**
 * Plugin to display the list of pull requests submitted by or waiting for
 * the current user.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 */
public class PullRequestsPlugin extends AbstractCommandWikiPlugin<PullRequestsPluginCommand>  {
	@Autowired
	private ScmPullRequestManager pullRequestManager;

	public PullRequestsPlugin() {
		setWebBindingInitializer(new PullRequestsBindingInitializer());
	}

	public static class PullRequestsPropertyEditorRegistrar implements PropertyEditorRegistrar {
		public void registerCustomEditors(PropertyEditorRegistry registry) {
			registry.registerCustomEditor(List.class, "projectId", new CommaStringToIntegerListPropertyEditor());
			registry.registerCustomEditor(List.class, "repositoryId", new CommaStringToIntegerListPropertyEditor());
		}
	}

	public static class PullRequestsBindingInitializer extends DefaultWebBindingInitializer {
		public PullRequestsBindingInitializer() {
			addPropertyEditorRegistrar(new PullRequestsPropertyEditorRegistrar());
		}
	}

	@Override
	public PullRequestsPluginCommand createCommand() throws PluginException {
		return new PullRequestsPluginCommand();
	}

	@Override
	protected String getTemplateFilename() {
		return "pullrequests-plugin.vm";
	}

	@Override
	protected Map populateModel(DataBinder binder, PullRequestsPluginCommand command, Map params) throws PluginException {
		UserDto user = getUser();
		List<ScmPullRequestDto> pullRequests = findPullRequests(user, command.getProjectId(), command.getRepositoryId());

		Map model = new HashMap(3);
		model.put("user", user);
		model.put("pullRequests", pullRequests);
		model.put("userPhotoTool", new UserPhotoTool(wikiContext.getHttpRequest()));

		return model;
	}

	/**
	 * @return the pull requests submitted by or waiting for the user in a project, in a repository or globally.
	 */
	protected List<ScmPullRequestDto> findPullRequests(UserDto user, List<Integer> projectId, List<Integer> repositoryId) {
		// find pull requests in a project
		if(projectId != null) {
			return pullRequestManager.findPullRequestsByProject(user, projectId, Collections.singleton(ScmPullRequestDto.Filter.PENDING));
		}

		// find pull requests in a repository
		if(repositoryId != null) {
			return pullRequestManager.findPullRequestsByRepository(user, repositoryId, Collections.singleton(ScmPullRequestDto.Filter.PENDING));
		}

		Set<String> globalFilters = new HashSet<String>();
		globalFilters.add(ScmPullRequestDto.Filter.PENDING);
		globalFilters.add(ScmPullRequestDto.Filter.REQUESTED_BY);
		globalFilters.add(ScmPullRequestDto.Filter.AWAITING_FOR);
		// find pull requests globally
		return pullRequestManager.findPullRequestsByUser(user, user, globalFilters);
	}
}
