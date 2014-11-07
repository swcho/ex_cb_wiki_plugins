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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.velocity.VelocityContext;
import org.springframework.beans.factory.annotation.Autowired;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.manager.UserManager;
import com.intland.codebeamer.persistence.dao.impl.EntityCache;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.ProjectPermission;
import com.intland.codebeamer.persistence.dto.RoleDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.util.PersistenceUtils;
import com.intland.codebeamer.utils.velocitytool.UserPhotoTool;
import com.intland.codebeamer.wiki.plugins.base.AutoWiringCodeBeamerPlugin;

/**
 * Plugin to display a list of members and administrators
 *
 * @author <a href="mailto:kao@intland.com">Alexey Kolesnikov</a>
 * @version $Id$
 */
public class MembersPlugin extends AutoWiringCodeBeamerPlugin  {
	@Autowired
	private UserManager userManager;

	@Override
	protected String getTemplateFilename() {
		return "members-plugin.vm";
	}

	@Override
	protected void populateContext(VelocityContext velocityContext, Map params) throws PluginException {
		ProjectDto project = discoverProject(params, getWikiContext());

		// Find members
		Collection<UserDto> members = findMembers(project);
		// Find all users
		Collection<UserDto> allUsers = findAllUsers(project);

		Set<UserDto> viaGroups = new HashSet<UserDto>();
		viaGroups.addAll(allUsers);
		viaGroups.removeAll(members);

		// find all admins
		Collection<UserDto> admins = findAllProjectAdmins(project);

		// set up Velocity context
		velocityContext.put("isCurrentProject", isEnclosingProject());
		velocityContext.put("project", project);
		velocityContext.put("numMembers", Integer.valueOf(members.size()));
		velocityContext.put("numMembersViaGroups", Integer.valueOf(viaGroups.size()));
		velocityContext.put("admins", admins);
		velocityContext.put("contextPath", getContextPath());
		velocityContext.put("hasPermissionToViewMembers", Boolean.valueOf(hasPermission(project)));
		velocityContext.put("userPhotoTool", new UserPhotoTool(wikiContext.getHttpRequest()));
	}

	protected boolean hasPermission(ProjectDto project) {
		return EntityCache.getInstance(getUser()).hasPermission(project, ProjectPermission.members_view);
	}

	protected Collection<UserDto> findMembers(ProjectDto project) {
		Collection<UserDto> members = userManager.findByProject(null, project.getId(), false);
		return members;
	}

	protected Collection<UserDto> findAllUsers(ProjectDto project) {
		// Find all users
		Collection<UserDto> allUsers = userManager.findByProject(null, project.getId(), true);
		return allUsers;
	}

	protected Collection<UserDto> findAllProjectAdmins(ProjectDto project) {
		return userManager.findByProjectAndRole(getUser(), project.getId(), PersistenceUtils.createSingleItemList(RoleDto.PROJECT_ADMIN), false);
	}
}
