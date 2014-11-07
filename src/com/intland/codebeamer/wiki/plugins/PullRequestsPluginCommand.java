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

import javax.validation.constraints.AssertFalse;

import org.apache.commons.collections.CollectionUtils;

import com.intland.codebeamer.wiki.plugins.command.base.AbstractWikiPluginCommand;

/**
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 */
public class PullRequestsPluginCommand extends AbstractWikiPluginCommand {
	private List<Integer> projectId;
	private List<Integer> repositoryId;

	public List<Integer> getProjectId() {
		return projectId;
	}

	public void setProjectId(List<Integer> projectId) {
		this.projectId = projectId;
	}

	public List<Integer> getRepositoryId() {
		return repositoryId;
	}

	public void setRepositoryId(List<Integer> repositoryId) {
		this.repositoryId = repositoryId;
	}

	@AssertFalse(message = "either 'projectId' or 'repositoryId' may be used, but not both")
	public boolean isOnlyOneOfProjectIdOrRepositoryIdValid() {
		boolean isProjectIdValid = !CollectionUtils.isEmpty(projectId);
		boolean isrepositoryIdValid = !CollectionUtils.isEmpty(repositoryId);

		return (isProjectIdValid && isrepositoryIdValid);
	}
}
