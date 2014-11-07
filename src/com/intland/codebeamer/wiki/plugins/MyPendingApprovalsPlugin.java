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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.validation.DataBinder;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.manager.ArtifactApprovalManager;
import com.intland.codebeamer.manager.ArtifactApprovalManager.ModificationOrder;
import com.intland.codebeamer.persistence.dto.ArtifactApprovalHistoryEntryDto;
import com.intland.codebeamer.persistence.dto.ArtifactDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.wiki.plugins.base.AbstractCommandWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.NamedPluginException;
import com.intland.codebeamer.wiki.plugins.command.base.ProjectAwareWikiPluginCommand;

/**
 * Wiki plugin show the current users' pending approvals.
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 * $Id$
 */
public class MyPendingApprovalsPlugin extends AbstractCommandWikiPlugin<ProjectAwareWikiPluginCommand> {
	private ArtifactApprovalManager artifactApprovalManager;

	@Override
	public String getTemplateFilename() {
		return "mypendingapprovals-plugin.vm";
	}

	@Override
	protected void validate(DataBinder binder, ProjectAwareWikiPluginCommand command, Map params) throws NamedPluginException {
		super.validate(binder, command, params);
		try {
			command.discoverProject(this, params);
		} catch (PluginException ex) {
			addError(binder, "projectId", ex.getMessage());
		}
		command.validate(binder, params);
	}

	@Override
	public ProjectAwareWikiPluginCommand createCommand() {
		return new ProjectAwareWikiPluginCommand();
	}

	@Override
	protected Map populateModel(DataBinder binder, ProjectAwareWikiPluginCommand command, Map params) {
		Map model = new HashMap();
		UserDto currentUser = getUser();
		Integer projectId = command.isAllProjects() ? null: command.getProjectId();
		final ModificationOrder order = ArtifactApprovalManager.ModificationOrder.OLDEST_FIRST;
		List<ArtifactApprovalHistoryEntryDto> pendingApprovals = artifactApprovalManager.findPendingApprovals(currentUser, currentUser, projectId, command.getMax(), order);
		model.put("pendingApprovals", pendingApprovals);

		Map<ArtifactDto, ArtifactApprovalHistoryEntryDto> artifactsWithState = artifactApprovalManager.findMyArtifactsInApproval(currentUser, projectId, command.getMax(), order);
		model.put("myArtifactsInApproval", artifactsWithState.keySet());
		model.put("myArtifactsInApprovalWithPendingStep", artifactsWithState);

		// put command  in context as it contains the other stuff like cssClass etc...
		model.put("command",command);
		return model;
	}

	public void setArtifactApprovalManager(ArtifactApprovalManager artifactApprovalManager) {
		this.artifactApprovalManager = artifactApprovalManager;
	}
}
