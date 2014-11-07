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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.validation.DataBinder;

import com.ecyrd.jspwiki.plugin.PluginException;

import com.intland.codebeamer.persistence.dto.ArtifactDto;
import com.intland.codebeamer.persistence.dto.JoinProjectRequestDto;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.RoleDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.wiki.plugins.base.AbstractCommandWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.NamedPluginException;
import com.intland.codebeamer.wiki.plugins.command.base.ProjectAwareWikiPluginCommand;

/**
 * Wiki plugin showing the pending project join approvals, and also rendering the artifact-approvals. So it:
 * 1. project join requests waiting for me to approve them
 * 2. artifact versions waiting for me to approve them
 * 3. artifact versions submitted by me and waiting for someone else's approval
 *
 * For rendering the artifact approvals it includes the @see {@link MyPendingApprovalsPlugin} plugin.
 *
 * @author <a href="mailto:kao@intland.com">Alexey Kolesnikov</a>
 * @version $Id$
 */
@SuppressWarnings("deprecation")
public class MyApprovalsPlugin extends AbstractCommandWikiPlugin<ProjectAwareWikiPluginCommand> {
	private static final String RENDER_TEMPLATE = "myapprovals-plugin.vm";

	private MyPendingApprovalsPlugin myPendingApprovalsPlugin = new MyPendingApprovalsPlugin();

	@Override
	public String getTemplateFilename() {
		return RENDER_TEMPLATE;
	}


	@Override
	protected void validate(DataBinder binder, ProjectAwareWikiPluginCommand command, Map params) throws NamedPluginException {
		super.validate(binder, command, params);
		try {
			command.discoverProject(this, params);
		} catch (PluginException ex) {
			addError(binder, "projectId", ex.getMessage());
		}
	}

	@Override
	public ProjectAwareWikiPluginCommand createCommand() {
		return new ProjectAwareWikiPluginCommand();
	}

	protected List<JoinProjectRequestDto> loadJoinProjectRequests(UserDto user, Integer projectId)  {
		List<JoinProjectRequestDto> result = new ArrayList<JoinProjectRequestDto>();
		Map<ProjectDto,Map<RoleDto,Map<UserDto,ArtifactDto>>> requests = projectManager.getPendingJoinRequests(user, projectId);
		if (requests != null && requests.size() > 0) {
			for (Map.Entry<ProjectDto,Map<RoleDto,Map<UserDto,ArtifactDto>>> project : requests.entrySet()) {
				for (Map.Entry<RoleDto,Map<UserDto,ArtifactDto>> role : project.getValue().entrySet()) {
					for (Map.Entry<UserDto,ArtifactDto> member : role.getValue().entrySet()) {
						JoinProjectRequestDto request = new JoinProjectRequestDto();
						request.setProject(project.getKey());
						request.setUser(member.getKey());
						request.setCreatedAt(member.getValue().getCreatedAt());
						request.setComment(member.getValue().getComment());

						result.add(request);
					}
				}
			}
		}
		return result;
	}

	@Override
	protected Map populateModel(DataBinder binder, ProjectAwareWikiPluginCommand command, Map params) throws PluginException {
		// get beans from wiki context
		UserDto user = getUser();

		List<JoinProjectRequestDto> requests = loadJoinProjectRequests(user, command.getProjectId());

		// set up Velocity context
		Map<String,Object> model = new HashMap<String,Object>(8);
		model.put("requests", requests);
		model.put("contextPath", getApplicationContextPath(getWikiContext()));

		// also render the artifact-approvals plugin will be included
		String includedArtifactApprovalsPlugin = myPendingApprovalsPlugin.execute(getWikiContext(),params);
		model.put("includedArtifactApprovalsPlugin", includedArtifactApprovalsPlugin);

		model.put("command", command);

		return model;
	}
}
