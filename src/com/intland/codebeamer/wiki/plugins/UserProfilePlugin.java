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
import java.util.Locale;
import java.util.Map;

import org.apache.velocity.VelocityContext;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.controller.admin.UsersAndGroupsController;
import com.intland.codebeamer.manager.UserManager;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.utils.velocitytool.UserPhotoTool;
import com.intland.codebeamer.wiki.plugins.base.AbstractCodeBeamerWikiPlugin;

/**
 * Plugin to display user profile information.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 * @version $Id$
 */
public class UserProfilePlugin extends AbstractCodeBeamerWikiPlugin {
	public String execute(WikiContext context, Map params) throws PluginException {
		// parse params
		Integer targetUserId = null;// TODO option: 'basic', 'detailed', 'all'?
		String targetUserName = null;
		UserDto targetUser = null;

		String otherUserIdString = getParameter(params, "id");
		if(otherUserIdString != null) {
			targetUserId = new Integer(otherUserIdString);
		} else {
			targetUserName = getParameter(params, "name");
		}

		UserDto user = getUserFromContext(context);
		if(targetUserId != null) {
			targetUser = UserManager.getInstance().findById(user, targetUserId);
		} else if(targetUserName != null) {
			targetUser = UserManager.getInstance().findByName(user, targetUserName);
		} else {
			// fall back to current user if neither ID nor name was specified
			targetUser = user;
		}

		// set up Velocity context
		VelocityContext velocityContext = getDefaultVelocityContextFromContext(context);
		Locale locale = (Locale) velocityContext.get("locale");
		velocityContext.put("contextPath", getApplicationContextPath(context));
		velocityContext.remove("user"); // remove originator
		velocityContext.put("user", targetUser);
		velocityContext.put("country", targetUser.getLocale().getDisplayCountry(locale));
		velocityContext.put("language", targetUser.getLocale().getDisplayLanguage(locale));
		velocityContext.put("timezone", targetUser.getTimeZone().getDisplayName(locale));
		velocityContext.put("userPhotoTool", new UserPhotoTool(context.getHttpRequest()));

		// transform permission model to Velocity context
		Map<String,Object> permissionModel = new HashMap<String,Object>();
		UsersAndGroupsController.addUserPermissions(user, targetUser, permissionModel);
		for (Map.Entry<String,Object> entry : permissionModel.entrySet()) {
			velocityContext.put(entry.getKey(), entry.getValue());
		}

		// render template
		return renderPluginTemplate("user-profile-plugin.vm", velocityContext);
	}
}
