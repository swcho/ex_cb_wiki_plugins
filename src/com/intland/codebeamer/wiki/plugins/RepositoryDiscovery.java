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
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.DataBinder;
import org.springframework.validation.ObjectError;

import com.intland.codebeamer.controller.ControllerUtils;
import com.intland.codebeamer.manager.ScmRepositoryManager;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.ScmRepositoryDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.util.PersistenceUtils;
import com.intland.codebeamer.wiki.plugins.base.AutoWiringCodeBeamerPlugin;
import com.intland.codebeamer.wiki.plugins.base.NamedPluginException;

/**
 * Helper class for executing repository discovery.
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public class RepositoryDiscovery {

	private final static Logger logger = Logger.getLogger(RepositoryDiscovery.class);

	private AutoWiringCodeBeamerPlugin plugin;

	@Autowired
	private ScmRepositoryManager scmRepositoryManager;

	public RepositoryDiscovery(AutoWiringCodeBeamerPlugin plugin) {
		this.plugin = plugin;
		ControllerUtils.autoWire(this, plugin.getApplicationContext());
	}

	private boolean checkIfRepositoryIdsAreValid = true;

	public boolean isCheckIfRepositoryIdsAreValid() {
		return checkIfRepositoryIdsAreValid;
	}

	public void setCheckIfRepositoryIdsAreValid(boolean checkIfRepositoryIdsAreValid) {
		this.checkIfRepositoryIdsAreValid = checkIfRepositoryIdsAreValid;
	}

	/**
	 * Discover repository-ids if needed
	 * @param pluginParams the plugin's parameters
	 * @param repositoryId The current repositoryIds. If empty, missing then discovery is attempted
	 * @throws NamedPluginException
	 */
	protected List<Integer> discoverRepositoryIds(DataBinder binder, Map pluginParams, List<Integer> repositoryId) throws NamedPluginException {
		final UserDto user = plugin.getUser();

		if (CollectionUtils.isEmpty(repositoryId)) {
			// try to auto discover project and its repositories
			ProjectDto project = plugin.discoverProject(pluginParams, plugin.getWikiContext());
			if (project != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Discovered project <" + project +">, now querying repositories");
				}
				List<ScmRepositoryDto> projectRepositories = scmRepositoryManager.findByProject(user, project.getId());
				List<Integer> discovered = PersistenceUtils.grabIds(projectRepositories);

				return discovered;
			}
		} else {
			if (checkIfRepositoryIdsAreValid) {
				// check if all provided repository-ids are valid and accessible for the user
				List<ScmRepositoryDto> repositories = scmRepositoryManager.findById(user, repositoryId);
				// using a set to avoid duplications
				Set<Integer> requestedIds = new TreeSet<Integer>(repositoryId);
				List<Integer> foundIds = PersistenceUtils.grabIds(repositories);
				requestedIds.removeAll(foundIds);
				if (! requestedIds.isEmpty()) {
					logger.info("Not all repositories are accessible for the user, showing invalid repository-ids");
					ObjectError err = new ObjectError("command", new String[] {"scm.plugins.invalid.repository.ids"} , new String[] {requestedIds.toString()}, "Some repository ids are invalid");
					binder.getBindingResult().addError(err);
				}
			}
		}
		return repositoryId;
	}

	/**
	 * Get the message-code explaining why there is no repository information is displayed
	 * @param pluginParams The plugin's parameters
	 * @return The message code
	 */
	public static String getMissingRepositoryIdReasonCode(AutoWiringCodeBeamerPlugin plugin, Map pluginParams) {
		if (pluginParams.get("repositoryId") == null) {
			return "scm.plugins.this.project.has.no.repository";
		}
		return "scm.plugins.missing.repository.ids";
	}

}
